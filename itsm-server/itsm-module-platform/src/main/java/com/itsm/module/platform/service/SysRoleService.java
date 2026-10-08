package com.itsm.module.platform.service;

import com.itsm.module.platform.dto.RoleSaveCmd;
import com.itsm.module.platform.dto.RoleVO;

import java.util.List;

/** 角色服务 */
public interface SysRoleService {

    List<RoleVO> list();

    Long create(RoleSaveCmd cmd);

    void update(Long id, RoleSaveCmd cmd);

    void delete(Long id);

    void assignMenus(Long id, List<Long> menuIds);
}

