package com.diet.config;

import com.diet.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String COOKIE_NAME = "diet_token";
    private static final long RENEW_THRESHOLD_MS = 2 * 60 * 60 * 1000; // 2 hours

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        if (token != null) {
            try {
                Claims claims = jwtUtil.parseClaims(token);
                Long userId = Long.parseLong(claims.getSubject());
                String role = claims.get("role", String.class);
                String username = claims.get("username", String.class);

                // Token 即将过期时自动续期（通过 httpOnly Cookie 返回新 token）
                long remaining = claims.getExpiration().getTime() - System.currentTimeMillis();
                if (remaining < RENEW_THRESHOLD_MS) {
                    String newToken = jwtUtil.generateToken(userId, username, role);
                    addTokenCookie(request, response, newToken);
                }

                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                logger.warn("JWT token解析失败: " + e.getMessage());
                // 清除无效 cookie
                clearTokenCookie(request, response);
            }
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 从 httpOnly Cookie 或 Authorization Header 中提取 token
     * 优先使用 Cookie（更安全），兼容 Header（Swagger 等工具）
     */
    private String resolveToken(HttpServletRequest request) {
        // 1. 优先从 Cookie 读取
        Cookie cookie = WebUtils.getCookie(request, COOKIE_NAME);
        if (cookie != null && !cookie.getValue().isEmpty()) {
            return cookie.getValue();
        }
        // 2. 兼容 Authorization Header（供 Swagger/Postman 使用）
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * 设置 httpOnly Cookie（Secure + SameSite）
     */
    public static void addTokenCookie(HttpServletRequest request, HttpServletResponse response, String token) {
        String secure = request.isSecure() ? "; Secure" : "";
        response.addHeader("Set-Cookie",
            COOKIE_NAME + "=" + token + "; Path=/; HttpOnly; Max-Age=86400; SameSite=Lax" + secure);
    }

    /**
     * 清除 token Cookie
     */
    public static void clearTokenCookie(HttpServletRequest request, HttpServletResponse response) {
        String secure = request.isSecure() ? "; Secure" : "";
        response.addHeader("Set-Cookie",
            COOKIE_NAME + "=; Path=/; HttpOnly; Max-Age=0; SameSite=Lax" + secure);
    }
}
