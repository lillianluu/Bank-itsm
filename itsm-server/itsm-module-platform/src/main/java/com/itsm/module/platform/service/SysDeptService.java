package com.itsm.module.platform.service;

import com.itsm.module.platform.dto.DeptNode;
import com.itsm.module.platform.dto.DeptSaveCmd;

import java.util.List;

/** 部门服务 */
public interface SysDeptService {

    List<DeptNode> tree();

    Long create(DeptSaveCmd cmd);

    void update(Long id, DeptSaveCmd cmd);

    void delete(Long id);
}

