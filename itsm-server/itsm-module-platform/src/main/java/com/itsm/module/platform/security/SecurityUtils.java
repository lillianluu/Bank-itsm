package com.itsm.module.platform.security;

import com.itsm.common.api.ErrorCode;
import com.itsm.common.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** 当前登录用户工具 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /** 未认证时抛 UNAUTHORIZED(permitAll 接口内调用属于误用) */
    public static LoginUser current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser user) {
            return user;
        }
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }

    /** 审计字段填充等场景使用,未登录时返回 system */
    public static String currentUsernameOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser user) {
            return user.getUsername();
        }
        return null;
    }

    public static boolean hasPerm(String perm) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof LoginUser user && user.hasPerm(perm);
    }
}

