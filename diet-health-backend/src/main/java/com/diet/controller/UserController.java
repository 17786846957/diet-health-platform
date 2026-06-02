package com.diet.controller;

import com.diet.common.R;
import com.diet.common.BusinessException;
import com.diet.common.ResultCode;
import com.diet.dto.UserProfileUpdateRequest;
import com.diet.entity.User;
import com.diet.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Tag(name = "用户管理", description = "用户个人信息管理")
@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "获取个人信息", description = "获取当前登录用户的个人信息")
    @GetMapping("/profile")
    public R<User> getProfile(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return R.ok(user);
    }

    @Operation(summary = "更新个人信息", description = "更新当前登录用户的个人信息")
    @PutMapping("/profile")
    public R<?> updateProfile(@Valid @RequestBody UserProfileUpdateRequest request, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        User user = new User();
        user.setId(userId);
        user.setEmail(request.getEmail());
        user.setGender(request.getGender());
        user.setAge(request.getAge());
        user.setHeight(request.getHeight());
        user.setWeight(request.getWeight());
        user.setActivityLevel(request.getActivityLevel());
        user.setGoal(request.getGoal());
        userService.updateProfile(user);
        return R.ok("更新成功", null);
    }
}
