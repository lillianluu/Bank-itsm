package com.itsm.module.platform.dto;

import java.util.List;

/** 角色视图 */
public record RoleVO(
        Long id,
        String name,
        String code,
        String dataScope,
        Integer status,
        String remark,
        List<Long> menuIds) {
}

