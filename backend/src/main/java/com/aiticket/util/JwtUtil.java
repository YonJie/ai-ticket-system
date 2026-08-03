package com.aiticket.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;

/**
 * JWT 工具类：生成与解析 token。
 */
@Component
public class JwtUtil {

    private final String secret;
    private final long expirationMs;

    /**
     * 从配置读取签名密钥与过期时间。
     *
     * @param secret       JWT 签名密钥（jwt.secret / 环境变量 JWT_SECRET）
     * @param expirationMs 过期毫秒数（jwt.expiration-ms）
     */
    public JwtUtil(
            @Value("${jwt.secret:ai-ticket-system-jwt-secret-change-me}") String secret,
            @Value("${jwt.expiration-ms:86400000}") long expirationMs) {
        this.secret = secret;
        this.expirationMs = expirationMs;
    }

    /**
     * 根据用户 ID 生成 JWT。
     *
     * @param userId 用户 ID
     * @return JWT 字符串
     */
    public String generateToken(UUID userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .setSubject(userId.toString())
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
    public UUID getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
        return UUID.fromString(claims.getSubject());
    }
}
