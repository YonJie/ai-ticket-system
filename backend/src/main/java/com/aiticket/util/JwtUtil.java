package com.aiticket.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * JWT 工具类：生成与解析 token。
 */
@Component
public class JwtUtil {

    private static final long EXPIRATION_MS = 24L * 60 * 60 * 1000;

    private final String secret;

    /**
     * 从环境变量 JWT_SECRET 读取签名密钥。
     */
    public JwtUtil() {
        String envSecret = System.getenv("JWT_SECRET");
        this.secret = (envSecret == null || envSecret.trim().isEmpty())
                ? "ai-ticket-system-jwt-secret-change-me"
                : envSecret;
    }

    /**
     * 根据用户 ID 生成 JWT（有效期 24 小时）。
     *
     * @param userId 用户 ID
     * @return JWT 字符串
     */
    public String generateToken(Long userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + EXPIRATION_MS);
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }

    /**
     * 从 JWT 中解析用户 ID。
     *
     * @param token JWT 字符串
     * @return 用户 ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
        return Long.valueOf(claims.getSubject());
    }
}
