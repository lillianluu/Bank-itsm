package com.itsm.module.platform.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.itsm.module.platform.entity.SysRole;
import com.itsm.module.platform.entity.SysUser;
import com.itsm.module.platform.entity.SysUserRole;
import com.itsm.module.platform.mapper.SysRoleMapper;
import com.itsm.module.platform.mapper.SysUserMapper;
import com.itsm.module.platform.mapper.SysUserRoleMapper;
import com.itsm.module.platform.security.LoginUser;
import com.itsm.module.platform.security.LoginUserLoader;
import com.itsm.module.platform.service.SysMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** 登录上下文装载:用户 → 启用角色 → 权限点 */
@Component
@RequiredArgsConstructor
public class LoginUserLoaderImpl implements LoginUserLoader {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysMenuService menuService;

    @Override
    public LoginUser load(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            return null;
        }
        List<Long> roleIds = userRoleMapper.selectList(
                        Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).toList();

        Set<String> roles = roleIds.isEmpty() ? Set.of()
                : roleMapper.selectBatchIds(roleIds).stream()
                        .filter(r -> r.getStatus() != null && r.getStatus() == 1)
                        .map(SysRole::getCode)
                        .collect(Collectors.toSet());

        boolean admin = roles.contains(LoginUser.ROLE_ADMIN);
        Set<String> perms = admin ? Set.of() : menuService.permsOf(roleIds);

        return LoginUser.builder()
                .userId(userId)
                .username(user.getUsername())
                .nickname(user.getNickname())
                .roles(roles)
                .perms(perms)
                .build();
    }
}

