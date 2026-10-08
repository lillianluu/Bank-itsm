package com.itsm.module.platform.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.itsm.module.platform.entity.SysMenu;
import com.itsm.module.platform.entity.SysRoleMenu;
import com.itsm.module.platform.mapper.SysMenuMapper;
import com.itsm.module.platform.mapper.SysRoleMenuMapper;
import com.itsm.module.platform.dto.MenuNode;
import com.itsm.module.platform.dto.MenuSaveCmd;
import com.itsm.module.platform.service.SysMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.itsm.common.api.ErrorCode.BAD_REQUEST;
import static com.itsm.common.api.ErrorCode.NOT_FOUND;

import com.itsm.common.exception.BusinessException;

/** 菜单与权限点服务实现 */
@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl implements SysMenuService {

    private static final List<String> NAV_TYPES = List.of("M", "C");

    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    @Override
    public List<MenuNode> tree() {
        List<SysMenu> menus = menuMapper.selectList(
                Wrappers.<SysMenu>lambdaQuery().orderByAsc(SysMenu::getSort).orderByAsc(SysMenu::getId));
        return buildTree(menus);
    }

    @Override
    public List<MenuNode> myMenuTree(boolean admin, List<Long> roleIds) {
        if (admin) {
            List<SysMenu> menus = menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                    .in(SysMenu::getType, NAV_TYPES)
                    .eq(SysMenu::getStatus, 1)
                    .orderByAsc(SysMenu::getSort).orderByAsc(SysMenu::getId));
            return buildTree(menus);
        }
        List<Long> menuIds = menuIdsOfRoles(roleIds);
        if (menuIds.isEmpty()) {
            return List.of();
        }
        List<SysMenu> menus = menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                .in(SysMenu::getId, menuIds)
                .in(SysMenu::getType, NAV_TYPES)
                .eq(SysMenu::getStatus, 1)
                .eq(SysMenu::getVisible, 1)
                .orderByAsc(SysMenu::getSort).orderByAsc(SysMenu::getId));
        return buildTree(menus);
    }

    @Override
    public Set<String> permsOf(List<Long> roleIds) {
        List<Long> menuIds = menuIdsOfRoles(roleIds);
        if (menuIds.isEmpty()) {
            return Set.of();
        }
        return menuMapper.selectBatchIds(menuIds).stream()
                .map(SysMenu::getPerm)
                .filter(p -> p != null && !p.isBlank())
                .collect(Collectors.toSet());
    }

    private List<Long> menuIdsOfRoles(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        return roleMenuMapper.selectList(
                        Wrappers.<SysRoleMenu>lambdaQuery().in(SysRoleMenu::getRoleId, roleIds))
                .stream().map(SysRoleMenu::getMenuId).distinct().toList();
    }

    private List<MenuNode> buildTree(List<SysMenu> menus) {
        Map<Long, MenuNode> nodeMap = new LinkedHashMap<>();
        for (SysMenu m : menus) {
            nodeMap.put(m.getId(), MenuNode.of(m.getId(), m.getParentId(), m.getName(),
                    m.getType(), m.getPath(), m.getComponent(), m.getIcon(), m.getSort()));
        }
        List<MenuNode> roots = new ArrayList<>();
        for (MenuNode node : nodeMap.values()) {
            MenuNode parent = node.parentId() == null ? null : nodeMap.get(node.parentId());
            if (parent == null || parent == node) {
                roots.add(node);
            } else {
                parent.children().add(node);
            }
        }
        return roots;
    }

    @Override
    @Transactional
    public Long create(MenuSaveCmd cmd) {
        SysMenu menu = new SysMenu();
        apply(menu, cmd);
        menuMapper.insert(menu);
        return menu.getId();
    }

    @Override
    @Transactional
    public void update(Long id, MenuSaveCmd cmd) {
        SysMenu menu = requireExists(id);
        if (cmd.parentId().equals(id)) {
            throw new BusinessException(BAD_REQUEST, "上级菜单不能是自身");
        }
        apply(menu, cmd);
        menuMapper.updateById(menu);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireExists(id);
        Long children = menuMapper.selectCount(Wrappers.<SysMenu>lambdaQuery().eq(SysMenu::getParentId, id));
        if (children > 0) {
            throw new BusinessException(BAD_REQUEST, "存在下级菜单,无法删除");
        }
        menuMapper.deleteById(id);
        roleMenuMapper.delete(Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getMenuId, id));
    }

    private SysMenu requireExists(Long id) {
        SysMenu menu = menuMapper.selectById(id);
        if (menu == null) {
            throw new BusinessException(NOT_FOUND, "菜单不存在");
        }
        return menu;
    }

    private void apply(SysMenu menu, MenuSaveCmd cmd) {
        menu.setParentId(cmd.parentId());
        menu.setName(cmd.name());
        menu.setType(cmd.type());
        menu.setPath(cmd.path());
        menu.setComponent(cmd.component());
        menu.setPerm(cmd.perm());
        menu.setIcon(cmd.icon());
        menu.setSort(cmd.sort() == null ? 0 : cmd.sort());
        menu.setVisible(cmd.visible() == null ? 1 : cmd.visible());
        menu.setStatus(cmd.status() == null ? 1 : cmd.status());
    }
}

