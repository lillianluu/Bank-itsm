package com.itsm.module.platform.dto;

/** 用户分页查询 */
public record UserQuery(
        Integer pageNum,
        Integer pageSize,
        String keyword,
        Long deptId,
        Integer status) {
}

