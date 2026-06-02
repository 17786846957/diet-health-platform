package com.diet.common;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 审计日志切面
 * 拦截标注了 @AuditLog 的方法，记录操作人、操作内容、请求信息
 */
@Slf4j
@Aspect
@Component
public class AuditLogAspect {

    @AfterReturning(pointcut = "@annotation(auditLog)", returning = "result")
    public void onSuccess(JoinPoint joinPoint, AuditLog auditLog, Object result) {
        String username = getCurrentUsername();
        String ip = getClientIp();
        log.info("[AUDIT] 用户={}, 操作={}, 资源={}, IP={}, 结果=成功",
                username, auditLog.value(), auditLog.resource(), ip);
    }

    @AfterThrowing(pointcut = "@annotation(auditLog)", throwing = "ex")
    public void onFail(JoinPoint joinPoint, AuditLog auditLog, Exception ex) {
        String username = getCurrentUsername();
        String ip = getClientIp();
        log.warn("[AUDIT] 用户={}, 操作={}, 资源={}, IP={}, 结果=失败, 原因={}",
                username, auditLog.value(), auditLog.resource(), ip, ex.getMessage());
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long) {
            return "userId:" + auth.getPrincipal();
        }
        return "anonymous";
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String ip = request.getHeader("X-Real-IP");
                if (ip == null || ip.isEmpty()) {
                    ip = request.getRemoteAddr();
                }
                return ip;
            }
        } catch (Exception ignored) {
        }
        return "unknown";
    }
}
