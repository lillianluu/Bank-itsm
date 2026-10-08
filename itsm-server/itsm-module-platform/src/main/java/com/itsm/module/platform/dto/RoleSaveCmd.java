package com.itsm.module.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 创建/更新角色 */
public record RoleSaveCmd(
        @NotBlank(message = "角色名称不能为空") @Size(max = 64) String name,
        @NotBlank(message = "角色编码不能为空") @Size(max = 64) String code,
        String dataScope,
        Integer status,
        String remark) {
}

