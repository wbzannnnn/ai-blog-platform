package com.lanou.springaidemo.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类
 * 用于生成、解析、验证 JWT 令牌（登录鉴权核心）
 */
// 交给 Spring 管理，可被 @Autowired 注入
@Component
public class JwtUtil {
    // 从配置文件 application.yml 读取 jwt.secret 密钥
    @Value("${jwt.secret}")
    private String secret;
    // 从配置文件读取 token 过期时间（毫秒）
    @Value("${jwt.expiration}")
    private Long expiration;

    /**
     * 获取签名密钥
     * 把配置的字符串密钥转成 JJWT 需要的 SecretKey 格式
     *
     * @return
     */
    private SecretKey getSigningKey() {
        // 把密钥字符串转成 UTF-8 字节数组
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        // 生成 HMAC-SHA 加密密钥（JJWT 规范）
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成 JWT Token
     *
     * @param username 用户名（存入 token 作为唯一标识）
     * @return 生成的 token 字符串
     */
    public String generateToken(String username) {
        // 创建 JWT 构建器
        return Jwts.builder()
                // 设置主题（存入用户名）
                .subject(username)
                // 设置 token 签发时间
                .issuedAt(new Date(System.currentTimeMillis()))
                // 设置 token 过期时间
                .expiration(new Date(System.currentTimeMillis() + expiration))
                // 设置签名密钥（加密）
                .signWith(getSigningKey())
                // 压缩生成最终的 token 字符串
                .compact();
    }

    /**
     * 从 token 中提取用户名
     *
     * @param token 前端传过来的令牌
     * @return 用户名
     */
    public String extractUsername(String token) {
        return Jwts.parser()
                // 验证签名密钥
                .verifyWith(getSigningKey())
                // 构建解析器
                .build()
                // 解析并验证 token
                .parseSignedClaims(token)
                // 获取 token 载荷（存储数据的部分）
                .getPayload()
                // 从载荷中获取 subject（即用户名）
                .getSubject();
    }

    /**
     * 验证 token 是否合法（未篡改、未过期）
     *
     * @param token 令牌
     * @return true=合法 false=无效
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    // 能正常解析 = 有效 token
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // 捕获所有 JWT 异常：签名错误、过期、格式非法等
            return false;
        }
    }

    /**
     * 判断 token 是否过期
     *
     * @param token 令牌
     * @return true=已过期 false=未过期
     */
    public boolean isTokenExpired(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    // 获取过期时间
                    .getExpiration()
                    // 和当前时间对比：在当前时间之前 = 已过期
                    .before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            // 解析失败（签名错/格式错）一律视为过期
            return true;
        }
    }
}