package com.diet.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.diet.common.R;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    @Value("${rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long WINDOW_MS = 60 * 1000; // 1 minute

    private static final int MAX_REGISTER_ATTEMPTS = 3;
    private static final long REGISTER_WINDOW_MS = 120 * 1000; // 2 minutes

    // ConcurrentHashMap 保证线程安全，定期清理防止内存泄漏
    private final Map<String, AttemptInfo> loginAttemptMap = new ConcurrentHashMap<>();
    private final Map<String, AttemptInfo> registerAttemptMap = new ConcurrentHashMap<>();

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 测试环境下跳过限流
        if (!rateLimitEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String uri = request.getRequestURI();
        String method = request.getMethod();

        // 登录接口限流
        if ("/auth/login".equals(uri) && "POST".equalsIgnoreCase(method)) {
            if (!checkLoginRate(request, response)) {
                return;
            }
        }

        // 注册接口限流
        if ("/auth/register".equals(uri) && "POST".equalsIgnoreCase(method)) {
            if (!checkRegisterRate(request, response)) {
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean checkLoginRate(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String clientIp = getClientIp(request);
        AttemptInfo info = loginAttemptMap.compute(clientIp, (key, existing) -> {
            if (existing == null || System.currentTimeMillis() - existing.windowStart > WINDOW_MS) {
                return new AttemptInfo();
            }
            return existing;
        });

        if (info.attempts.incrementAndGet() > MAX_LOGIN_ATTEMPTS) {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(429);
            response.getWriter().write(objectMapper.writeValueAsString(R.error(429, "登录尝试过于频繁，请1分钟后再试")));
            return false;
        }
        return true;
    }

    private boolean checkRegisterRate(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String clientIp = getClientIp(request);
        AttemptInfo info = registerAttemptMap.compute(clientIp, (key, existing) -> {
            if (existing == null || System.currentTimeMillis() - existing.windowStart > REGISTER_WINDOW_MS) {
                return new AttemptInfo();
            }
            return existing;
        });

        if (info.attempts.incrementAndGet() > MAX_REGISTER_ATTEMPTS) {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(429);
            response.getWriter().write(objectMapper.writeValueAsString(R.error(429, "注册尝试过于频繁，请2分钟后再试")));
            return false;
        }
        return true;
    }

    /**
     * 定时清理过期的限流记录，防止内存泄漏
     * 每5分钟执行一次
     */
    @Scheduled(fixedRate = 300000)
    public void cleanExpiredRecords() {
        long now = System.currentTimeMillis();
        loginAttemptMap.entrySet().removeIf(entry -> now - entry.getValue().windowStart > WINDOW_MS * 2);
        registerAttemptMap.entrySet().removeIf(entry -> now - entry.getValue().windowStart > REGISTER_WINDOW_MS * 2);
    }

    private String getClientIp(HttpServletRequest request) {
        // 优先使用 X-Real-IP（反向代理设置，不可伪造）
        String ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty()) {
            // 回退到 request.getRemoteAddr()（直接连接的客户端 IP）
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private static class AttemptInfo {
        final AtomicInteger attempts = new AtomicInteger(0);
        final long windowStart = System.currentTimeMillis();
    }
}
