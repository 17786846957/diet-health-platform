package com.diet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diet.common.BusinessException;
import com.diet.common.ResultCode;
import com.diet.entity.HealthGoal;
import com.diet.entity.WeightRecord;
import com.diet.mapper.HealthGoalMapper;
import com.diet.mapper.WeightRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthGoalService {

    private final HealthGoalMapper healthGoalMapper;
    private final WeightRecordMapper weightRecordMapper;

    @Transactional(rollbackFor = Exception.class)
    public void createGoal(HealthGoal goal) {
        log.info("创建健康目标: userId={}, type={}", goal.getUserId(), goal.getGoalType());
        goal.setStartDate(LocalDate.now());
        goal.setStatus("active");
        goal.setProgress(BigDecimal.ZERO);
        healthGoalMapper.insert(goal);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateGoal(HealthGoal goal, Long userId) {
        HealthGoal existing = healthGoalMapper.selectById(goal.getId());
        if (existing == null) {
            throw new BusinessException(ResultCode.GOAL_NOT_FOUND);
        }
        if (!existing.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.GOAL_FORBIDDEN);
        }
        // 只更新允许修改的业务字段，不更新 userId、status、progress 等系统字段
        existing.setGoalType(goal.getGoalType());
        existing.setTargetWeight(goal.getTargetWeight());
        existing.setTargetCalories(goal.getTargetCalories());
        existing.setTargetProtein(goal.getTargetProtein());
        existing.setTargetFat(goal.getTargetFat());
        existing.setTargetCarbs(goal.getTargetCarbs());
        existing.setTargetWater(goal.getTargetWater());
        healthGoalMapper.updateById(existing);
    }

    @Transactional(rollbackFor = Exception.class)
    public void completeGoal(Long id, Long userId) {
        HealthGoal goal = healthGoalMapper.selectById(id);
        if (goal == null) {
            throw new BusinessException(ResultCode.GOAL_NOT_FOUND);
        }
        if (!goal.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.GOAL_FORBIDDEN);
        }
        goal.setStatus("completed");
        goal.setEndDate(LocalDate.now());
        goal.setProgress(new BigDecimal("100"));
        healthGoalMapper.updateById(goal);
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelGoal(Long id, Long userId) {
        HealthGoal goal = healthGoalMapper.selectById(id);
        if (goal == null) {
            throw new BusinessException(ResultCode.GOAL_NOT_FOUND);
        }
        if (!goal.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.GOAL_FORBIDDEN);
        }
        goal.setStatus("cancelled");
        goal.setEndDate(LocalDate.now());
        healthGoalMapper.updateById(goal);
    }

    public HealthGoal getActiveGoal(Long userId, Long memberId) {
        LambdaQueryWrapper<HealthGoal> wrapper = buildWrapper(userId, memberId);
        wrapper.eq(HealthGoal::getStatus, "active")
               .orderByDesc(HealthGoal::getCreateTime)
               .last("LIMIT 1");
        return healthGoalMapper.selectOne(wrapper);
    }

    public List<HealthGoal> listGoals(Long userId, Long memberId, String status) {
        LambdaQueryWrapper<HealthGoal> wrapper = buildWrapper(userId, memberId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(HealthGoal::getStatus, status);
        }
        wrapper.orderByDesc(HealthGoal::getCreateTime);
        return healthGoalMapper.selectList(wrapper);
    }

    public void updateProgress(Long goalId, BigDecimal currentWeight) {
        HealthGoal goal = healthGoalMapper.selectById(goalId);
        if (goal == null || !"active".equals(goal.getStatus())) {
            return;
        }

        if (goal.getTargetWeight() != null && goal.getTargetWeight().compareTo(BigDecimal.ZERO) > 0
                && currentWeight != null && currentWeight.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal targetWeight = goal.getTargetWeight();
            BigDecimal startWeight = getStartWeight(goal);
            BigDecimal totalToLose = startWeight.subtract(targetWeight).abs();
            if (totalToLose.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal lost = startWeight.subtract(currentWeight).abs();
                BigDecimal progress = lost.divide(totalToLose, 2, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                        .min(new BigDecimal("100"));
                goal.setProgress(progress);
            }
        }
        healthGoalMapper.updateById(goal);
    }

    private BigDecimal getStartWeight(HealthGoal goal) {
        LambdaQueryWrapper<WeightRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WeightRecord::getUserId, goal.getUserId());
        if (goal.getMemberId() != null) {
            wrapper.eq(WeightRecord::getMemberId, goal.getMemberId());
        } else {
            wrapper.isNull(WeightRecord::getMemberId);
        }
        if (goal.getStartDate() != null) {
            wrapper.le(WeightRecord::getRecordDate, goal.getStartDate());
        }
        wrapper.orderByAsc(WeightRecord::getRecordDate).last("LIMIT 1");
        WeightRecord first = weightRecordMapper.selectOne(wrapper);
        return first != null ? first.getWeight() : null;
    }

    private LambdaQueryWrapper<HealthGoal> buildWrapper(Long userId, Long memberId) {
        LambdaQueryWrapper<HealthGoal> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthGoal::getUserId, userId);
        if (memberId == null) {
            wrapper.isNull(HealthGoal::getMemberId);
        } else {
            wrapper.eq(HealthGoal::getMemberId, memberId);
        }
        return wrapper;
    }
}