package com.diet.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {
        long start = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            if (duration > 1000) {
                log.warn("Slow request: {} {} status={} duration={}ms",
                        request.getMethod(), request.getRequestURI(),
                        response.getStatus(), duration);
            } else {
                log.info("req method={} uri={} status={} durationMs={}",
                        request.getMethod(), request.getRequestURI(),
                        response.getStatus(), duration);
            }
        }
    }
}
