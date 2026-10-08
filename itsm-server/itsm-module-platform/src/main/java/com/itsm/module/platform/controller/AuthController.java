package com.itsm.module.platform.controller;

import com.itsm.common.api.Result;
import com.itsm.module.platform.dto.LoginCmd;
import com.itsm.module.platform.dto.LoginVO;
import com.itsm.module.platform.dto.MeVO;
import com.itsm.module.platform.dto.RefreshCmd;
import com.itsm.module.platform.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 认证接口:登录 / 刷新 / 登出 / 当前用户 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginCmd cmd) {
        return Result.ok(authService.login(cmd));
    }

    @PostMapping("/refresh")
    public Result<LoginVO> refresh(@Valid @RequestBody RefreshCmd cmd) {
        return Result.ok(authService.refresh(cmd));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.ok(null);
    }

    @GetMapping("/me")
    public Result<MeVO> me() {
        return Result.ok(authService.me());
    }
}

