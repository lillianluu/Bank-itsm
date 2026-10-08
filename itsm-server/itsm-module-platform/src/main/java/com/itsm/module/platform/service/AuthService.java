package com.itsm.module.platform.service;

import com.itsm.module.platform.dto.LoginCmd;
import com.itsm.module.platform.dto.LoginVO;
import com.itsm.module.platform.dto.MeVO;
import com.itsm.module.platform.dto.RefreshCmd;

/** 认证服务 */
public interface AuthService {

    LoginVO login(LoginCmd cmd);

    LoginVO refresh(RefreshCmd cmd);

    MeVO me();

    void logout();
}

