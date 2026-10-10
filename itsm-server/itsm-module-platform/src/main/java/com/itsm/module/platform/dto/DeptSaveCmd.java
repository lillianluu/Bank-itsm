package com.itsm.module.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

/** 创建/更新部门 */
public record DeptSaveCmd(
        @NotNull(message = "上级部门不能为空") @Min(0) Long parentId,
        @NotBlank(message = "部门名称不能为空") @Size(max = 64) String name,
        Integer sort,
        @Size(max = 64) String leader,
        @Size(max = 32) String phone,
        @Size(max = 128) String email,
        @Min(0) @Max(1) Integer status) {
}

