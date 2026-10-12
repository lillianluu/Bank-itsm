package com.itsm.module.platform.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.itsm.common.exception.BusinessException;
import com.itsm.module.platform.dto.LoginCmd;
import com.itsm.module.platform.dto.LoginVO;
import com.itsm.module.platform.dto.MeVO;
import com.itsm.module.platform.dto.RefreshCmd;
import com.itsm.module.platform.entity.SysUser;
import com.itsm.module.platform.entity.SysUserRole;
import com.itsm.module.platform.mapper.SysUserMapper;
import com.itsm.module.platform.mapper.SysUserRoleMapper;
import com.itsm.module.platform.security.JwtTokenProvider;
import com.itsm.module.platform.security.LoginUser;
import com.itsm.module.platform.security.LoginUserLoader;
import com.itsm.module.platform.security.SecurityUtils;
import com.itsm.module.platform.service.AuthService;
import com.itsm.module.platform.service.SysMenuService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

import static com.itsm.common.api.ErrorCode.FORBIDDEN;
import static com.itsm.common.api.ErrorCode.UNAUTHORIZED;

/** 认证服务实现(无状态 JWT) */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysMenuService menuService;
    private final LoginUserLoader loginUserLoader;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Override
    public LoginVO login(LoginCmd cmd) {
        SysUser user = userMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, cmd.username()));
        if (user == null || !passwordEncoder.matches(cmd.password(), user.getPassword())) {
            throw new BusinessException(UNAUTHORIZED, "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(FORBIDDEN, "账号已停用");
        }
        touchLastLogin(user.getId());
        return issueTokens(user);
    }

    @Override
    public LoginVO refresh(RefreshCmd cmd) {
        Claims claims;
        try {
            claims = tokenProvider.parse(cmd.refreshToken());
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(UNAUTHORIZED, "刷新令牌无效或已过期");
        }
        if (!JwtTokenProvider.TYPE_REFRESH.equals(claims.get(JwtTokenProvider.CLAIM_TYPE, String.class))) {
            throw new BusinessException(UNAUTHORIZED, "令牌类型错误");
        }
        Long userId = tokenProvider.userIdOf(claims);
        SysUser user = userId == null ? null : userMapper.selectById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(UNAUTHORIZED, "账号不可用");
        }
        return issueTokens(user);
    }

    @Override
    public MeVO me() {
        LoginUser lu = SecurityUtils.current();
        List<Long> roleIds = roleIdsOf(lu.getUserId());
        return new MeVO(lu.getUserId(), lu.getUsername(), lu.getNickname(),
                lu.getRoles(), lu.getPerms(), menuService.myMenuTree(lu.isAdmin(), roleIds));
    }

    @Override
    public void logout() {
        // 无状态 JWT:签发即有效,由前端丢弃令牌;M2 引入 Redis 黑名单按 jti 撤销
        log.info("用户退出登录: {}", SecurityUtils.currentUsernameOrNull());
    }

    private LoginVO issueTokens(SysUser user) {
        LoginUser lu = loginUserLoader.load(user.getId());
        List<String> roles = lu == null || lu.getRoles() == null
                ? List.of() : List.copyOf(lu.getRoles());
        return new LoginVO(
                tokenProvider.createAccessToken(user.getId(), user.getUsername()),
                tokenProvider.createRefreshToken(user.getId(), user.getUsername()),
                user.getId(), user.getUsername(), user.getNickname(), roles);
    }

    private List<Long> roleIdsOf(Long userId) {
        return userRoleMapper.selectList(
                        Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).toList();
    }

    private void touchLastLogin(Long userId) {
        SysUser upd = new SysUser();
        upd.setId(userId);
        upd.setLastLoginAt(OffsetDateTime.now());
        userMapper.updateById(upd);
    }
}

