package com.itsm.module.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 创建用户 */
public record UserCreateCmd(
        @NotBlank(message = "用户名不能为空") @Size(max = 64) String username,
        @NotBlank(message = "密码不能为空") @Size(min = 6, max = 64, message = "密码长度 6-64 位") String password,
        @NotBlank(message = "姓名不能为空") @Size(max = 64) String nickname,
        Long deptId,
        String email,
        String phone,
        Integer status,
        List<Long> roleIds) {
}

