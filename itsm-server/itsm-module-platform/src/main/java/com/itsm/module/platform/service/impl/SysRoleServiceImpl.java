package com.itsm.module.platform.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.itsm.common.exception.BusinessException;
import com.itsm.module.platform.dto.RoleSaveCmd;
import com.itsm.module.platform.dto.RoleVO;
import com.itsm.module.platform.entity.SysRole;
import com.itsm.module.platform.entity.SysRoleMenu;
import com.itsm.module.platform.mapper.SysRoleMapper;
import com.itsm.module.platform.mapper.SysRoleMenuMapper;
import com.itsm.module.platform.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.itsm.common.api.ErrorCode.BAD_REQUEST;
import static com.itsm.common.api.ErrorCode.CONFLICT;
import static com.itsm.common.api.ErrorCode.NOT_FOUND;

/** 角色服务实现 */
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl implements SysRoleService {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    @Override
    public List<RoleVO> list() {
        List<SysRole> roles = roleMapper.selectList(
                Wrappers.<SysRole>lambdaQuery().orderByAsc(SysRole::getId));
        if (roles.isEmpty()) {
            return List.of();
        }
        List<Long> roleIds = roles.stream().map(SysRole::getId).toList();
        Map<Long, List<Long>> menuIdsByRole = roleMenuMapper.selectList(
                        Wrappers.<SysRoleMenu>lambdaQuery().in(SysRoleMenu::getRoleId, roleIds))
                .stream().collect(Collectors.groupingBy(SysRoleMenu::getRoleId,
                        Collectors.mapping(SysRoleMenu::getMenuId, Collectors.toList())));
        return roles.stream().map(r -> toVO(r, menuIdsByRole.getOrDefault(r.getId(), List.of()))).toList();
    }

    @Override
    @Transactional
    public Long create(RoleSaveCmd cmd) {
        checkCodeUnique(cmd.code(), null);
        SysRole role = new SysRole();
        apply(role, cmd);
        roleMapper.insert(role);
        return role.getId();
    }

    @Override
    @Transactional
    public void update(Long id, RoleSaveCmd cmd) {
        SysRole role = requireExists(id);
        if (SysRole.BUILTIN_ADMIN.equals(role.getCode()) && !Objects.equals(role.getCode(), cmd.code())) {
            throw new BusinessException(BAD_REQUEST, "内置角色编码不可修改");
        }
        checkCodeUnique(cmd.code(), id);
        apply(role, cmd);
        roleMapper.updateById(role);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        SysRole role = requireExists(id);
        if (SysRole.BUILTIN_ADMIN.equals(role.getCode())) {
            throw new BusinessException(BAD_REQUEST, "内置角色不可删除");
        }
        roleMapper.deleteById(id);
        roleMenuMapper.delete(Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getRoleId, id));
    }

    @Override
    @Transactional
    public void assignMenus(Long id, List<Long> menuIds) {
        requireExists(id);
        roleMenuMapper.delete(Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getRoleId, id));
        List<Long> ids = menuIds == null ? List.of() : menuIds.stream().filter(Objects::nonNull).distinct().toList();
        for (Long menuId : ids) {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(id);
            rm.setMenuId(menuId);
            roleMenuMapper.insert(rm);
        }
    }

    private SysRole requireExists(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(NOT_FOUND, "角色不存在");
        }
        return role;
    }

    private void checkCodeUnique(String code, Long excludeId) {
        Long count = roleMapper.selectCount(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getCode, code).ne(excludeId != null, SysRole::getId, excludeId));
        if (count > 0) {
            throw new BusinessException(CONFLICT, "角色编码已存在: " + code);
        }
    }

    private void apply(SysRole role, RoleSaveCmd cmd) {
        role.setName(cmd.name());
        role.setCode(cmd.code());
        role.setDataScope(cmd.dataScope() == null ? "SELF" : cmd.dataScope());
        role.setStatus(cmd.status() == null ? 1 : cmd.status());
        role.setRemark(cmd.remark());
    }

    private RoleVO toVO(SysRole role, List<Long> menuIds) {
        return new RoleVO(role.getId(), role.getName(), role.getCode(), role.getDataScope(),
                role.getStatus(), role.getRemark(), new ArrayList<>(menuIds));
    }
}

