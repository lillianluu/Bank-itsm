package com.itsm.module.platform.dto;

import java.util.List;

/** 登录/刷新响应 */
public record LoginVO(
        String accessToken,
        String refreshToken,
        Long userId,
        String username,
        String nickname,
        List<String> roles) {
}

