package com.itsm.module.platform.security;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

/** 登录用户上下文对象(认证后携带角色与权限点) */
@Data
@Builder
public class LoginUser {

    /** 内置超管角色码 */
    public static final String ROLE_ADMIN = "ADMIN";

    private Long userId;
    private String username;
    private String nickname;
    private Set<String> roles;
    private Set<String> perms;

    public boolean isAdmin() {
        return roles != null && roles.contains(ROLE_ADMIN);
    }

    public boolean hasPerm(String perm) {
        if (perm == null || perm.isBlank()) {
            return true;
        }
        return isAdmin() || (perms != null && perms.contains(perm));
    }
}

