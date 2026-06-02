package com.diet.controller;

import com.diet.common.R;
import com.diet.service.DietAdviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "智能饮食建议", description = "基于算法的智能饮食建议服务")
@RestController
@RequestMapping("/diet-advice")
public class DietAdviceController {

    private final DietAdviceService dietAdviceService;

    public DietAdviceController(DietAdviceService dietAdviceService) {
        this.dietAdviceService = dietAdviceService;
    }

    @Operation(summary = "获取每日饮食建议", description = "获取今日的饮食建议和营养分析")
    @GetMapping("/daily")
    public R<Map<String, Object>> getDailyAdvice(
            @Parameter(description = "家庭成员ID") @RequestParam(required = false) Long memberId,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return R.ok(dietAdviceService.getDailyAdvice(userId, memberId));
    }

    @Operation(summary = "获取饮食分析报告", description = "分析近N天的饮食习惯")
    @GetMapping("/analysis")
    public R<Map<String, Object>> getDietAnalysis(
            @Parameter(description = "统计天数") @RequestParam(defaultValue = "7") int days,
            @Parameter(description = "家庭成员ID") @RequestParam(required = false) Long memberId,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return R.ok(dietAdviceService.getDietAnalysis(userId, memberId, days));
    }

    @Operation(summary = "获取健康建议", description = "基于用户目标获取个性化健康建议")
    @GetMapping("/health")
    public R<Map<String, Object>> getHealthAdvice(
            @Parameter(description = "家庭成员ID") @RequestParam(required = false) Long memberId,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return R.ok(dietAdviceService.getHealthAdvice(userId, memberId));
    }
}