package com.itsm.module.platform.dto;

import java.util.ArrayList;
import java.util.List;

/** 部门树节点 */
public record DeptNode(
        Long id,
        Long parentId,
        String name,
        Integer sort,
        String leader,
        String phone,
        Integer status,
        List<DeptNode> children) {

    public static DeptNode of(Long id, Long parentId, String name, Integer sort,
                              String leader, String phone, Integer status) {
        return new DeptNode(id, parentId, name, sort, leader, phone, status, new ArrayList<>());
    }
}

