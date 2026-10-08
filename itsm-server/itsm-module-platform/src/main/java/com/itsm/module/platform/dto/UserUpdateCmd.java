package com.itsm.module.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 更新用户(不含用户名与密码) */
public record UserUpdateCmd(
        @NotBlank(message = "姓名不能为空") @Size(max = 64) String nickname,
        Long deptId,
        String email,
        String phone,
        Integer status,
        List<Long> roleIds) {
}

