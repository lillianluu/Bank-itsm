package com.itsm.module.platform.service;

import com.itsm.module.platform.dto.MenuNode;
import com.itsm.module.platform.dto.MenuSaveCmd;

import java.util.List;
import java.util.Set;

/** 菜单与权限点服务 */
public interface SysMenuService {

    /** 全量菜单树(含按钮,管理端用) */
    List<MenuNode> tree();

    /** 当前用户可见的导航菜单树(仅目录/菜单,排除按钮) */
    List<MenuNode> myMenuTree(boolean admin, List<Long> roleIds);

    /** 角色集合对应的权限点(如 sys:user:add) */
    Set<String> permsOf(List<Long> roleIds);

    Long create(MenuSaveCmd cmd);

    void update(Long id, MenuSaveCmd cmd);

    void delete(Long id);
}

