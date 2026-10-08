package com.itsm.module.platform.security;

/** 按 userId 装载登录上下文(用户 + 角色码 + 权限点);用户不存在/停用返回 null */
public interface LoginUserLoader {

    LoginUser load(Long userId);
}

