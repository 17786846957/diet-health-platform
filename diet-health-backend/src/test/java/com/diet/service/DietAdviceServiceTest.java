package com.diet.service;

import com.diet.entity.DietRecord;
import com.diet.entity.Food;
import com.diet.entity.HealthGoal;
import com.diet.entity.User;
import com.diet.mapper.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DietAdviceServiceTest {

    @Mock private DietRecordService dietRecordService;
    @Mock private UserService userService;
    @Mock private FoodMapper foodMapper;
    @Mock private DietRecordMapper dietRecordMapper;
    @Mock private DietRecordDetailMapper detailMapper;
    @Mock private HealthGoalMapper healthGoalMapper;
    @InjectMocks private DietAdviceService dietAdviceService;

    @Test
    void getDailyAdvice_noRecords_statusNotStarted() {
        Map<String, Object> emptyStats = new java.util.LinkedHashMap<>();
        emptyStats.put("totalCalories", 0.0);
        emptyStats.put("totalProtein", 0.0);
        emptyStats.put("totalFat", 0.0);
        emptyStats.put("totalCarbs", 0.0);

        when(dietRecordService.getDailyStats(anyLong(), any(), any())).thenReturn(emptyStats);
        when(userService.calculateTargets(1L)).thenReturn(new double[]{2000, 60, 55, 300});
        when(dietRecordService.recommendFoods(anyLong(), any(), any())).thenReturn(Collections.emptyList());

        Map<String, Object> result = dietAdviceService.getDailyAdvice(1L, null);
        assertEquals("未开始", result.get("status"));
    }

    @Test
    void getDailyAdvice_targetMet_statusReached() {
        Map<String, Object> stats = new java.util.LinkedHashMap<>();
        stats.put("totalCalories", 2100.0);
        stats.put("totalProtein", 65.0);
        stats.put("totalFat", 60.0);
        stats.put("totalCarbs", 320.0);

        when(dietRecordService.getDailyStats(anyLong(), any(), any())).thenReturn(stats);
        when(userService.calculateTargets(1L)).thenReturn(new double[]{2000, 60, 55, 300});
        when(dietRecordService.recommendFoods(anyLong(), any(), any())).thenReturn(Collections.emptyList());

        Map<String, Object> result = dietAdviceService.getDailyAdvice(1L, null);
        assertEquals("已达标", result.get("status"));
    }

    @Test
    void getDailyAdvice_nearTarget_statusNear() {
        Map<String, Object> stats = new java.util.LinkedHashMap<>();
        stats.put("totalCalories", 1700.0);
        stats.put("totalProtein", 50.0);
        stats.put("totalFat", 45.0);
        stats.put("totalCarbs", 250.0);

        when(dietRecordService.getDailyStats(anyLong(), any(), any())).thenReturn(stats);
        when(userService.calculateTargets(1L)).thenReturn(new double[]{2000, 60, 55, 300});
        when(dietRecordService.recommendFoods(anyLong(), any(), any())).thenReturn(Collections.emptyList());

        Map<String, Object> result = dietAdviceService.getDailyAdvice(1L, null);
        assertEquals("接近达标", result.get("status"));
    }

    @Test
    void getDailyAdvice_withMemberId_usesMemberTargets() {
        Map<String, Object> emptyStats = new java.util.LinkedHashMap<>();
        emptyStats.put("totalCalories", 0.0);
        emptyStats.put("totalProtein", 0.0);
        emptyStats.put("totalFat", 0.0);
        emptyStats.put("totalCarbs", 0.0);

        when(dietRecordService.getDailyStats(anyLong(), any(), any())).thenReturn(emptyStats);
        when(userService.calculateTargetsForMember(10L)).thenReturn(new double[]{1800, 67, 50, 270});
        when(dietRecordService.recommendFoods(anyLong(), any(), any())).thenReturn(Collections.emptyList());

        Map<String, Object> result = dietAdviceService.getDailyAdvice(1L, 10L);
        assertNotNull(result.get("nutritionGap"));
    }

    @Test
    void getDietAnalysis_noRecords() {
        when(dietRecordService.listByDateRange(anyLong(), any(), any(), any())).thenReturn(Collections.emptyList());

        Map<String, Object> result = dietAdviceService.getDietAnalysis(1L, null, 7);
        assertNotNull(result.get("analysis"));
        assertNotNull(result.get("avgNutrition"));
    }

    @Test
    void getHealthAdvice_userNotFound() {
        when(userService.getById(999L)).thenReturn(null);

        Map<String, Object> result = dietAdviceService.getHealthAdvice(999L, null);
        assertEquals("用户信息不存在", ((java.util.List<?>) result.get("advice")).get(0));
    }

    @Test
    void getHealthAdvice_loseWeightGoal() {
        User user = new User();
        user.setId(1L);
        user.setGoal("lose_weight");
        when(userService.getById(1L)).thenReturn(user);
        when(healthGoalMapper.selectOne(any())).thenReturn(null);

        Map<String, Object> result = dietAdviceService.getHealthAdvice(1L, null);
        java.util.List<String> advice = (java.util.List<String>) result.get("advice");
        assertTrue(advice.get(0).contains("减脂"));
    }

    @Test
    void getHealthAdvice_muscleGoal() {
        User user = new User();
        user.setId(1L);
        user.setGoal("muscle");
        when(userService.getById(1L)).thenReturn(user);
        when(healthGoalMapper.selectOne(any())).thenReturn(null);

        Map<String, Object> result = dietAdviceService.getHealthAdvice(1L, null);
        java.util.List<String> advice = (java.util.List<String>) result.get("advice");
        assertTrue(advice.get(0).contains("增肌"));
    }

    @Test
    void getHealthAdvice_healthGoal() {
        User user = new User();
        user.setId(1L);
        user.setGoal("health");
        when(userService.getById(1L)).thenReturn(user);
        when(healthGoalMapper.selectOne(any())).thenReturn(null);

        Map<String, Object> result = dietAdviceService.getHealthAdvice(1L, null);
        java.util.List<String> advice = (java.util.List<String>) result.get("advice");
        assertTrue(advice.get(0).contains("保持健康"));
    }

    @Test
    void getHealthAdvice_noGoal() {
        User user = new User();
        user.setId(1L);
        user.setGoal(null);
        when(userService.getById(1L)).thenReturn(user);
        when(healthGoalMapper.selectOne(any())).thenReturn(null);

        Map<String, Object> result = dietAdviceService.getHealthAdvice(1L, null);
        java.util.List<String> advice = (java.util.List<String>) result.get("advice");
        assertTrue(advice.get(0).contains("建议设置"));
    }

    @Test
    void getHealthAdvice_activeGoalFromTable() {
        User user = new User();
        user.setId(1L);
        user.setGoal("health");
        when(userService.getById(1L)).thenReturn(user);

        HealthGoal goal = new HealthGoal();
        goal.setGoalType("gain_weight");
        when(healthGoalMapper.selectOne(any())).thenReturn(goal);

        Map<String, Object> result = dietAdviceService.getHealthAdvice(1L, null);
        assertEquals("gain_weight", result.get("goal"));
    }
}
