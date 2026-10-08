package com.itsm.module.platform.dto;

import java.util.ArrayList;
import java.util.List;

/** 菜单树节点(路由与菜单管理共用) */
public record MenuNode(
        Long id,
        Long parentId,
        String name,
        String type,
        String path,
        String component,
        String icon,
        Integer sort,
        List<MenuNode> children) {

    public static MenuNode of(Long id, Long parentId, String name, String type,
                              String path, String component, String icon, Integer sort) {
        return new MenuNode(id, parentId, name, type, path, component, icon, sort, new ArrayList<>());
    }
}

