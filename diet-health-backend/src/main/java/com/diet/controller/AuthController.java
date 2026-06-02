package com.diet.controller;

import com.diet.common.AuditLog;
import com.diet.common.BusinessException;
import com.diet.common.R;
import com.diet.common.ResultCode;
import com.diet.config.JwtAuthenticationFilter;
import com.diet.dto.LoginRequest;
import com.diet.dto.RegisterRequest;
import com.diet.entity.User;
import com.diet.service.UserService;
import com.diet.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Map;

@Tag(name = "认证管理", description = "用户注册、登录、Token刷新")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @AuditLog(value = "用户注册", resource = "auth")
    @Operation(summary = "用户注册", description = "注册新用户账号")
    @PostMapping("/register")
    public R<?> register(@Valid @RequestBody RegisterRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        userService.register(user);
        return R.ok("注册成功", null);
    }

    @AuditLog(value = "用户登录", resource = "auth")
    @Operation(summary = "用户登录", description = "用户名密码登录，Token 通过 httpOnly Cookie 返回")
    @PostMapping("/login")
    public R<?> login(@Valid @RequestBody LoginRequest params, HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> result = userService.login(params.getUsername(), params.getPassword());
        String token = (String) result.get("token");
        JwtAuthenticationFilter.addTokenCookie(request, response, token);
        // 响应体只返回用户信息，不返回 token
        return R.ok(Map.of("user", result.get("user")));
    }

    @Operation(summary = "刷新Token", description = "Token 即将过期时自动续期")
    @PostMapping("/refresh")
    public R<?> refresh(Authentication auth, HttpServletRequest request, HttpServletResponse response) {
        Long userId = (Long) auth.getPrincipal();
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户不存在");
        }
        String newToken = jwtUtil.generateToken(
                user.getId(), user.getUsername(), user.getRole());
        JwtAuthenticationFilter.addTokenCookie(request, response, newToken);
        return R.ok("刷新成功", null);
    }

    @Operation(summary = "退出登录", description = "清除 Token Cookie")
    @PostMapping("/logout")
    public R<?> logout(HttpServletRequest request, HttpServletResponse response) {
        JwtAuthenticationFilter.clearTokenCookie(request, response);
        return R.ok("退出成功", null);
    }
}
