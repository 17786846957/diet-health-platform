package com.diet.controller;

import com.diet.common.AuditLog;
import com.diet.common.R;
import com.diet.dto.DietRecordRequest;
import com.diet.dto.DietRecordDetailRequest;
import com.diet.entity.DietRecord;
import com.diet.entity.DietRecordDetail;
import com.diet.service.DietRecordService;
import com.diet.service.FoodRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "饮食记录管理", description = "饮食记录的增删改查及统计分析")
@RestController
@RequestMapping("/diet")
public class DietRecordController {

    private final DietRecordService dietRecordService;
    private final FoodRecommendationService foodRecommendationService;

    public DietRecordController(DietRecordService dietRecordService,
                                FoodRecommendationService foodRecommendationService) {
        this.dietRecordService = dietRecordService;
        this.foodRecommendationService = foodRecommendationService;
    }

    @AuditLog(value = "添加饮食记录", resource = "diet_record")
    @Operation(summary = "添加饮食记录", description = "添加一条饮食记录，包含食物明细")
    @PostMapping
    public R<?> addRecord(@Valid @RequestBody DietRecordRequest request, Authentication auth) {
        DietRecord record = new DietRecord();
        record.setUserId((Long) auth.getPrincipal());
        record.setRecordDate(request.getRecordDate());
        record.setMealType(request.getMealType());
        record.setMemberId(request.getMemberId());
        if (request.getDetails() != null) {
            List<DietRecordDetail> details = new java.util.ArrayList<>();
            for (DietRecordDetailRequest d : request.getDetails()) {
                DietRecordDetail detail = new DietRecordDetail();
                detail.setFoodId(d.getFoodId());
                detail.setAmount(d.getAmount());
                details.add(detail);
            }
            record.setDetails(details);
        }
        dietRecordService.addRecord(record);
        return R.ok("添加成功", null);
    }

    @Operation(summary = "更新饮食记录", description = "更新已有的饮食记录")
    @PutMapping
    public R<?> updateRecord(@Valid @RequestBody DietRecordRequest request, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        DietRecord record = new DietRecord();
        record.setId(request.getId());
        record.setUserId(userId);
        record.setRecordDate(request.getRecordDate());
        record.setMealType(request.getMealType());
        record.setMemberId(request.getMemberId());
        if (request.getDetails() != null) {
            List<DietRecordDetail> details = new java.util.ArrayList<>();
            for (DietRecordDetailRequest d : request.getDetails()) {
                DietRecordDetail detail = new DietRecordDetail();
                detail.setFoodId(d.getFoodId());
                detail.setAmount(d.getAmount());
                details.add(detail);
            }
            record.setDetails(details);
        }
        dietRecordService.updateRecord(record, userId);
        return R.ok("更新成功", null);
    }

    @AuditLog(value = "删除饮食记录", resource = "diet_record")
    @Operation(summary = "删除饮食记录", description = "根据 ID 删除饮食记录及其明细")
    @DeleteMapping("/{id}")
    public R<?> deleteRecord(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        dietRecordService.deleteRecord(id, userId);
        return R.ok("删除成功", null);
    }

    @Operation(summary = "按日期查询记录", description = "查询指定日期的饮食记录列表")
    @GetMapping("/list")
    public R<List<DietRecord>> listByDate(
            @Parameter(description = "日期，格式 YYYY-MM-DD") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @Parameter(description = "家庭成员ID，不传则查询主用户") @RequestParam(required = false) Long memberId,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return R.ok(dietRecordService.listByDate(userId, memberId, date));
    }

    @Operation(summary = "每日统计", description = "获取指定日期的营养摄入统计")
    @GetMapping("/daily-stats")
    public R<Map<String, Object>> getDailyStats(
            @Parameter(description = "日期") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam(required = false) Long memberId,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return R.ok(dietRecordService.getDailyStats(userId, memberId, date));
    }

    @Operation(summary = "每周趋势", description = "获取指定日期范围的热量趋势数据")
    @GetMapping("/weekly-stats")
    public R<Map<String, Object>> getWeeklyStats(
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate start,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end,
            @RequestParam(required = false) Long memberId,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return R.ok(dietRecordService.getWeeklyStats(userId, memberId, start, end));
    }

    @Operation(summary = "每月统计", description = "获取指定月份的每日营养明细")
    @GetMapping("/monthly-stats")
    public R<Map<String, Object>> getMonthlyStats(
            @Parameter(description = "年份") @RequestParam int year,
            @Parameter(description = "月份") @RequestParam int month,
            @RequestParam(required = false) Long memberId,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return R.ok(dietRecordService.getMonthlyStats(userId, memberId, year, month));
    }

    @Operation(summary = "营养缺口分析", description = "对比实际摄入与目标，分析营养缺口")
    @GetMapping("/nutrition-gap")
    public R<Map<String, Object>> getNutritionGap(
            @Parameter(description = "日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam(required = false) Long memberId,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        LocalDate d = (date != null) ? date : LocalDate.now();
        return R.ok(dietRecordService.getNutritionGap(userId, memberId, d));
    }

    @Operation(summary = "最近常吃食物", description = "获取最近N天最常食用的食物排行")
    @GetMapping("/recent-foods")
    public R<List<Map<String, Object>>> getRecentFoods(
            @Parameter(description = "统计天数，默认7天") @RequestParam(defaultValue = "7") int days,
            @RequestParam(required = false) Long memberId,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return R.ok(dietRecordService.getRecentFoods(userId, memberId, days));
    }

    @Operation(summary = "饮食推荐", description = "基于历史偏好和营养缺口，推荐下一餐食物")
    @GetMapping("/recommend")
    public R<List<Map<String, Object>>> getRecommendations(
            @Parameter(description = "餐次类型") @RequestParam(defaultValue = "lunch") String mealType,
            @RequestParam(required = false) Long memberId,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return R.ok(foodRecommendationService.recommendFoods(userId, memberId, mealType));
    }
}
