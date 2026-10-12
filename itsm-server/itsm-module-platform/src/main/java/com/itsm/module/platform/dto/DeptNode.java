package com.itsm.module.platform.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.util.ArrayList;
import java.util.List;

/** 部门树节点 */
public record DeptNode(
        @JsonSerialize(using = ToStringSerializer.class) Long id,
        @JsonSerialize(using = ToStringSerializer.class) Long parentId,
        String name,
        Integer sort,
        String leader,
        String phone,
        String email,
        Integer status,
        List<DeptNode> children) {

    public static DeptNode of(Long id, Long parentId, String name, Integer sort,
                              String leader, String phone, String email, Integer status) {
        return new DeptNode(id, parentId, name, sort, leader, phone, email, status, new ArrayList<>());
    }
}

