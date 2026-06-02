package com.diet.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.diet.common.R;
import com.diet.entity.User;
import com.diet.service.DietRecordService;
import com.diet.service.FoodService;
import com.diet.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "后台管理", description = "管理员专用接口（需管理员权限）")
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('admin')")
public class AdminController {

    private final UserService userService;
    private final FoodService foodService;
    private final DietRecordService dietRecordService;

    public AdminController(UserService userService, FoodService foodService,
                           DietRecordService dietRecordService) {
        this.userService = userService;
        this.foodService = foodService;
        this.dietRecordService = dietRecordService;
    }

    @Operation(summary = "仪表盘数据", description = "获取系统概览数据（用户数、食物数、记录数）")
    @GetMapping("/dashboard")
    public R<Map<String, Object>> getDashboard() {
        return R.ok(Map.of(
            "totalUsers", userService.count(),
            "totalFoods", foodService.count(),
            "totalRecords", dietRecordService.count()
        ));
    }

    @Operation(summary = "用户列表", description = "分页查询用户，支持关键词和角色筛选")
    @GetMapping("/users")
    public R<IPage<User>> listUsers(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") @javax.validation.constraints.Max(100) int size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "用户角色") @RequestParam(required = false) String role) {
        return R.ok(userService.listUsers(page, size, keyword, role));
    }

    @Operation(summary = "删除用户", description = "管理员删除用户（不能删除自己）")
    @DeleteMapping("/users/{id}")
    public R<?> deleteUser(@PathVariable Long id, org.springframework.security.core.Authentication auth) {
        Long currentUserId = (Long) auth.getPrincipal();
        if (currentUserId.equals(id)) {
            throw new com.diet.common.BusinessException(com.diet.common.ResultCode.BAD_REQUEST, "不能删除自己的账号");
        }
        userService.deleteUser(id);
        return R.ok("删除成功", null);
    }
}
