package com.itsm.module.platform.dto;

import jakarta.validation.constraints.NotBlank;

/** 刷新令牌请求 */
public record RefreshCmd(
        @NotBlank(message = "refreshToken 不能为空") String refreshToken) {
}

