package com.diet.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * JWT配置验证类
 * 启动时验证JWT Secret的安全性和合法性
 */
@Slf4j
@Component
public class JwtSecurityConfig {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    @PostConstruct
    public void validate() {
        // 验证密钥长度（至少32字节）
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                "JWT Secret 长度不足32字符，请使用强密钥启动应用。\n" +
                "可通过环境变量 JWT_SECRET 设置，或在配置文件中配置。\n" +
                "建议使用: openssl rand -base64 32 生成强密钥"
            );
        }

        // 验证密钥是否使用了默认值（安全风险）
        if (secret.contains("diet-health-platform-secret-key") ||
            secret.equals("changeme") ||
            secret.equals("secret")) {
            throw new IllegalStateException(
                "检测到使用了默认或弱 JWT Secret，存在严重安全风险！\n" +
                "请通过环境变量 JWT_SECRET 设置强密钥。\n" +
                "建议使用: openssl rand -base64 32 生成强密钥"
            );
        }

        // 验证Token过期时间
        if (expiration <= 0) {
            throw new IllegalStateException("JWT expiration 必须大于0");
        }
        
        if (expiration > 86400000) { // 超过24小时
            log.warn("⚠️  Token 过期时间超过24小时，可能存在安全风险");
        }

        log.info("JWT配置验证通过: secret长度={}字符, expiration={}小时", 
                 secret.length(), expiration / 3600000);
    }
}
