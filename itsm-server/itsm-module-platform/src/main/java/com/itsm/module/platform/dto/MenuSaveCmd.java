package com.itsm.module.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 创建/更新菜单 */
public record MenuSaveCmd(
        @NotNull(message = "上级菜单不能为空") Long parentId,
        @NotBlank(message = "菜单名称不能为空") @Size(max = 64) String name,
        @NotBlank(message = "菜单类型不能为空") String type,
        String path,
        String component,
        String perm,
        String icon,
        Integer sort,
        Integer visible,
        Integer status) {
}

