package com.itsm.module.platform.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/** JWT 签发与解析(jjwt 0.12) */
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    public static final String CLAIM_UID = "uid";
    public static final String CLAIM_TYPE = "typ";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final JwtProperties props;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Long userId, String username) {
        return build(userId, username, TYPE_ACCESS, Duration.ofMinutes(props.getAccessTokenMinutes()));
    }

    public String createRefreshToken(Long userId, String username) {
        return build(userId, username, TYPE_REFRESH, Duration.ofDays(props.getRefreshTokenDays()));
    }

    private String build(Long userId, String username, String type, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(username)
                .claim(CLAIM_UID, userId)
                .claim(CLAIM_TYPE, type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key())
                .compact();
    }

    /** 解析并验签,失败(过期/篡改)抛 io.jsonwebtoken.JwtException */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
    }

    public Long userIdOf(Claims claims) {
        Object uid = claims.get(CLAIM_UID);
        return uid instanceof Number n ? n.longValue() : null;
    }
}

