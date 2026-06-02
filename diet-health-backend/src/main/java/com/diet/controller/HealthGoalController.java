package com.diet.controller;

import com.diet.common.R;
import com.diet.dto.HealthGoalRequest;
import com.diet.entity.HealthGoal;
import com.diet.service.HealthGoalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Tag(name = "健康目标管理", description = "健康目标的增删改查")
@RestController
@RequestMapping("/health-goal")
public class HealthGoalController {

    private final HealthGoalService healthGoalService;

    public HealthGoalController(HealthGoalService healthGoalService) {
        this.healthGoalService = healthGoalService;
    }

    @Operation(summary = "创建健康目标")
    @PostMapping
    public R<?> createGoal(@Valid @RequestBody HealthGoalRequest request, Authentication auth) {
        HealthGoal goal = new HealthGoal();
        goal.setUserId((Long) auth.getPrincipal());
        goal.setGoalType(request.getGoalType());
        goal.setTargetWeight(request.getTargetWeight());
        goal.setTargetCalories(request.getTargetCalories());
        goal.setTargetProtein(request.getTargetProtein());
        goal.setTargetFat(request.getTargetFat());
        goal.setTargetCarbs(request.getTargetCarbs());
        goal.setTargetWater(request.getTargetWater());
        goal.setMemberId(request.getMemberId());
        healthGoalService.createGoal(goal);
        return R.ok("创建成功", null);
    }

    @Operation(summary = "更新健康目标")
    @PutMapping
    public R<?> updateGoal(@Valid @RequestBody HealthGoalRequest request, Authentication auth) {
        HealthGoal goal = new HealthGoal();
        goal.setId(request.getId());
        goal.setGoalType(request.getGoalType());
        goal.setTargetWeight(request.getTargetWeight());
        goal.setTargetCalories(request.getTargetCalories());
        goal.setTargetProtein(request.getTargetProtein());
        goal.setTargetFat(request.getTargetFat());
        goal.setTargetCarbs(request.getTargetCarbs());
        goal.setTargetWater(request.getTargetWater());
        goal.setMemberId(request.getMemberId());
        healthGoalService.updateGoal(goal, (Long) auth.getPrincipal());
        return R.ok("更新成功", null);
    }

    @Operation(summary = "完成目标")
    @PostMapping("/{id}/complete")
    public R<?> completeGoal(@PathVariable Long id, Authentication auth) {
        healthGoalService.completeGoal(id, (Long) auth.getPrincipal());
        return R.ok("已完成", null);
    }

    @Operation(summary = "取消目标")
    @PostMapping("/{id}/cancel")
    public R<?> cancelGoal(@PathVariable Long id, Authentication auth) {
        healthGoalService.cancelGoal(id, (Long) auth.getPrincipal());
        return R.ok("已取消", null);
    }

    @Operation(summary = "获取当前活跃目标")
    @GetMapping("/active")
    public R<HealthGoal> getActiveGoal(
            @RequestParam(required = false) Long memberId,
            Authentication auth) {
        return R.ok(healthGoalService.getActiveGoal((Long) auth.getPrincipal(), memberId));
    }

    @Operation(summary = "目标列表")
    @GetMapping("/list")
    public R<List<HealthGoal>> listGoals(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long memberId,
            Authentication auth) {
        return R.ok(healthGoalService.listGoals((Long) auth.getPrincipal(), memberId, status));
    }
}
