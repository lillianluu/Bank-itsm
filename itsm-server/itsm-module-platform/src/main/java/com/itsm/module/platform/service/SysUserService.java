package com.itsm.module.platform.service;

import com.itsm.common.api.PageResult;
import com.itsm.module.platform.dto.UserCreateCmd;
import com.itsm.module.platform.dto.UserQuery;
import com.itsm.module.platform.dto.UserUpdateCmd;
import com.itsm.module.platform.dto.UserVO;

import java.util.List;

/** 用户服务 */
public interface SysUserService {

    PageResult<UserVO> page(UserQuery query);

    UserVO get(Long id);

    Long create(UserCreateCmd cmd);

    void update(Long id, UserUpdateCmd cmd);

    void delete(List<Long> ids);

    void resetPassword(Long id, String newPassword);

    void assignRoles(Long id, List<Long> roleIds);
}

