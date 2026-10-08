package com.itsm.module.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 创建/更新部门 */
public record DeptSaveCmd(
        @NotNull(message = "上级部门不能为空") Long parentId,
        @NotBlank(message = "部门名称不能为空") @Size(max = 64) String name,
        Integer sort,
        String leader,
        String phone,
        String email,
        Integer status) {
}

