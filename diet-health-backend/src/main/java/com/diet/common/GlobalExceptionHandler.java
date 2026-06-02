package com.diet.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一捕获各类异常并返回标准 R 响应格式
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常 — 由 BusinessException 抛出，包含自定义错误码和消息
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<R<?>> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常: code={}, message={}, uri={}", e.getCode(), e.getMessage(), request.getRequestURI());
        HttpStatus status;
        switch (e.getCode()) {
            case 401: status = HttpStatus.UNAUTHORIZED; break;
            case 403: status = HttpStatus.FORBIDDEN; break;
            case 404: case 1002: case 1004: case 1005: case 1007: case 1009: case 1011: case 1013: status = HttpStatus.NOT_FOUND; break;
            case 1001: status = HttpStatus.CONFLICT; break;
            case 1003: case 1006: case 1008: case 1010: case 1012: case 1014: status = HttpStatus.FORBIDDEN; break;
            default: status = HttpStatus.BAD_REQUEST; break;
        }
        return ResponseEntity.status(status).body(R.error(e.getCode(), e.getMessage()));
    }

    /**
     * 参数校验异常 — @Valid 注解触发的校验失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<?>> handleValidation(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: uri={}, errors={}", request.getRequestURI(), message);
        return ResponseEntity.badRequest().body(R.error(ResultCode.BAD_REQUEST.getCode(), message));
    }

    /**
     * 缺少请求参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<R<?>> handleMissingParam(MissingServletRequestParameterException e, HttpServletRequest request) {
        log.warn("缺少请求参数: uri={}, param={}", request.getRequestURI(), e.getParameterName());
        return ResponseEntity.badRequest().body(R.error(ResultCode.BAD_REQUEST.getCode(), "缺少必填参数: " + e.getParameterName()));
    }

    /**
     * 参数类型不匹配
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<R<?>> handleTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        log.warn("参数类型错误: uri={}, param={}", request.getRequestURI(), e.getName());
        return ResponseEntity.badRequest().body(R.error(ResultCode.BAD_REQUEST.getCode(), "参数类型错误: " + e.getName()));
    }

    /**
     * 请求方法不支持
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<R<?>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        log.warn("不支持的请求方法: uri={}, method={}", request.getRequestURI(), e.getMethod());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(R.error(405, "不支持的请求方法: " + e.getMethod()));
    }

    /**
     * 404 — 路径不存在
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<R<?>> handleNotFound(NoHandlerFoundException e, HttpServletRequest request) {
        log.warn("路径不存在: uri={}", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(R.error(404, "接口不存在: " + request.getRequestURI()));
    }

    /**
     * 认证失败 — 用户名密码错误
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<R<?>> handleBadCredentials(BadCredentialsException e, HttpServletRequest request) {
        log.warn("认证失败: uri={}", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(R.error(ResultCode.UNAUTHORIZED.getCode(), "用户名或密码错误"));
    }

    /**
     * 权限不足
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<R<?>> handleAccessDenied(AccessDeniedException e, HttpServletRequest request) {
        log.warn("权限不足: uri={}", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(R.error(ResultCode.FORBIDDEN.getCode(), "没有操作权限"));
    }

    /**
     * 兜底异常 — 未预料到的错误
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<?>> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常: uri={}, error={}", request.getRequestURI(), e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(R.error(ResultCode.INTERNAL_ERROR.getCode(), "服务器内部错误，请稍后重试"));
    }
}
