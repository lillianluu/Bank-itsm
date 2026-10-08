package com.itsm.module.platform.dto;

import java.util.List;
import java.util.Set;

/** 当前用户信息(/me) */
public record MeVO(
        Long userId,
        String username,
        String nickname,
        Set<String> roles,
        Set<String> perms,
        List<MenuNode> menus) {
}

