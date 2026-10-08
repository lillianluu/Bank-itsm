package com.itsm.module.platform.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** JWT 配置(itsm.security.jwt),密钥生产环境必须通过环境变量注入 */
@Data
@ConfigurationProperties(prefix = "itsm.security.jwt")
public class JwtProperties {

    /** HS256 签名密钥,长度须 ≥ 32 字节 */
    private String secret = "itsm-dev-secret-please-change-0123456789abcdef";

    /** 访问令牌有效期(分钟) */
    private long accessTokenMinutes = 30;

    /** 刷新令牌有效期(天) */
    private long refreshTokenDays = 7;
}

