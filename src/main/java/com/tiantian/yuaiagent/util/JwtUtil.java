package com.tiantian.yuaiagent.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * JWT 双令牌工具类
 * access_token：短时效，携带用户身份
 * refresh_token：长时效，存 Redis 用于续签
 */
@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long accessExpiration;
    private final long refreshExpiration;
    private final StringRedisTemplate redisTemplate;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshExpiration,
            StringRedisTemplate redisTemplate) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
        this.redisTemplate = redisTemplate;
    }

    /** 生成 access_token */
    public String generateAccessToken(String userId) {
        return Jwts.builder()
                .subject(userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpiration))
                .signWith(secretKey)
                .compact();
    }

    /** 生成 refresh_token，并存入 Redis */
    public String generateRefreshToken(String userId) {
        String token = Jwts.builder()
                .subject(userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(secretKey)
                .compact();
        // Redis Key: refresh:{userId}, 7天过期
        redisTemplate.opsForValue().set("refresh:" + userId, token, refreshExpiration, TimeUnit.MILLISECONDS);
        return token;
    }

    /** 从 token 中提取 userId */
    public String getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    /** 校验 access_token，返回 userId 或 null */
    public String validateAccessToken(String token) {
        return getUserIdFromToken(token);
    }

    /** 用 refresh_token 续签，返回新 access_token */
    public String refreshAccessToken(String refreshToken) {
        String userId = getUserIdFromToken(refreshToken);
        if (userId == null) return null;
        // 校验 Redis 中的 refresh_token 是否匹配
        String stored = redisTemplate.opsForValue().get("refresh:" + userId);
        if (!refreshToken.equals(stored)) return null;
        // 生成新 access_token
        return generateAccessToken(userId);
    }

    /** 登出：删除 Redis 中的 refresh_token */
    public void logout(String userId) {
        redisTemplate.delete("refresh:" + userId);
    }
}
