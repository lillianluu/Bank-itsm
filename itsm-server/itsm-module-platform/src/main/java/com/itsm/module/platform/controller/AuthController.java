package com.itsm.module.platform.controller;

import com.itsm.common.api.ErrorCode;
import com.itsm.common.api.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口骨架。
 * TODO(V1.0):JWT 签发/刷新 + 企业 SSO(OIDC)/LDAP 对接,见设计文档 7.10 与 10.4-D。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @PostMapping("/login")
    public Result<Void> login() {
        return Result.fail(ErrorCode.NOT_IMPLEMENTED);
    }

    @PostMapping("/refresh")
    public Result<Void> refresh() {
        return Result.fail(ErrorCode.NOT_IMPLEMENTED);
    }
}

