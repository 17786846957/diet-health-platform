package com.diet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diet.common.BusinessException;
import com.diet.common.ResultCode;
import com.diet.entity.ExerciseRecord;
import com.diet.mapper.ExerciseRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExerciseRecordService {

    private final ExerciseRecordMapper exerciseRecordMapper;

    @Transactional(rollbackFor = Exception.class)
    public void addRecord(ExerciseRecord record) {
        log.info("添加运动记录: userId={}, type={}, duration={}min", record.getUserId(), record.getExerciseType(), record.getDuration());
        if (record.getCaloriesBurned() == null || record.getCaloriesBurned().compareTo(BigDecimal.ZERO) == 0) {
            record.setCaloriesBurned(calculateCalories(record.getExerciseType(), record.getDuration(), record.getIntensity()));
        }
        exerciseRecordMapper.insert(record);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateRecord(ExerciseRecord record, Long userId) {
        ExerciseRecord existing = exerciseRecordMapper.selectById(record.getId());
        if (existing == null) {
            throw new BusinessException(ResultCode.EXERCISE_RECORD_NOT_FOUND);
        }
        if (!existing.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.EXERCISE_RECORD_FORBIDDEN);
        }
        // 只更新允许修改的业务字段
        existing.setExerciseType(record.getExerciseType());
        existing.setDuration(record.getDuration());
        existing.setIntensity(record.getIntensity());
        existing.setNotes(record.getNotes());
        if (record.getCaloriesBurned() != null) {
            existing.setCaloriesBurned(record.getCaloriesBurned());
        }
        exerciseRecordMapper.updateById(existing);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteRecord(Long id, Long userId) {
        ExerciseRecord record = exerciseRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ResultCode.EXERCISE_RECORD_NOT_FOUND);
        }
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.EXERCISE_RECORD_FORBIDDEN);
        }
        exerciseRecordMapper.deleteById(id);
    }

    public List<ExerciseRecord> listByDate(Long userId, Long memberId, LocalDate date) {
        LambdaQueryWrapper<ExerciseRecord> wrapper = buildWrapper(userId, memberId);
        wrapper.eq(ExerciseRecord::getRecordDate, date)
               .orderByDesc(ExerciseRecord::getCreateTime);
        return exerciseRecordMapper.selectList(wrapper);
    }

    public Map<String, Object> getDailyStats(Long userId, Long memberId, LocalDate date) {
        List<ExerciseRecord> records = listByDate(userId, memberId, date);
        int totalDuration = records.stream()
                .mapToInt(r -> r.getDuration() != null ? r.getDuration() : 0)
                .sum();
        BigDecimal totalCalories = records.stream()
                .map(r -> r.getCaloriesBurned() != null ? r.getCaloriesBurned() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("date", date);
        stats.put("totalDuration", totalDuration);
        stats.put("totalCalories", totalCalories);
        stats.put("recordCount", records.size());
        stats.put("records", records);
        return stats;
    }

    public Map<String, Object> getWeeklyStats(Long userId, Long memberId, LocalDate startDate) {
        LocalDate endDate = startDate.plusDays(6);
        LambdaQueryWrapper<ExerciseRecord> wrapper = buildWrapper(userId, memberId);
        wrapper.ge(ExerciseRecord::getRecordDate, startDate)
               .le(ExerciseRecord::getRecordDate, endDate);
        List<ExerciseRecord> records = exerciseRecordMapper.selectList(wrapper);

        Map<LocalDate, int[]> dailyMap = new LinkedHashMap<>();
        for (int i = 0; i < 7; i++) {
            dailyMap.put(startDate.plusDays(i), new int[]{0, 0}); // [duration, calories]
        }
        for (ExerciseRecord record : records) {
            int[] arr = dailyMap.get(record.getRecordDate());
            if (arr != null) {
                arr[0] += record.getDuration();
                arr[1] += record.getCaloriesBurned() != null ? record.getCaloriesBurned().intValue() : 0;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        List<String> dates = new ArrayList<>();
        List<Integer> durations = new ArrayList<>();
        List<Integer> calories = new ArrayList<>();
        dailyMap.forEach((date, arr) -> {
            dates.add(date.toString());
            durations.add(arr[0]);
            calories.add(arr[1]);
        });
        result.put("dates", dates);
        result.put("durations", durations);
        result.put("calories", calories);
        return result;
    }

    private BigDecimal calculateCalories(String exerciseType, int duration, String intensity) {
        // 简化的热量消耗计算（基于MET值）
        double met = getMet(exerciseType, intensity);
        double caloriesPerMinute = met * 3.5 * 60 / 200; // 简化公式
        return BigDecimal.valueOf(Math.round(caloriesPerMinute * duration));
    }

    private double getMet(String exerciseType, String intensity) {
        String type = exerciseType.toLowerCase();
        if (type.contains("跑") || type.contains("run")) {
            return "high".equals(intensity) ? 10 : "low".equals(intensity) ? 6 : 8;
        } else if (type.contains("走") || type.contains("walk")) {
            return "high".equals(intensity) ? 5 : "low".equals(intensity) ? 2.5 : 3.5;
        } else if (type.contains("游泳") || type.contains("swim")) {
            return "high".equals(intensity) ? 10 : "low".equals(intensity) ? 5 : 7;
        } else if (type.contains("骑") || type.contains("bike") || type.contains("cycling")) {
            return "high".equals(intensity) ? 10 : "low".equals(intensity) ? 4 : 6;
        } else if (type.contains("瑜伽") || type.contains("yoga")) {
            return 3;
        } else if (type.contains("力量") || type.contains("strength") || type.contains("健身")) {
            return "high".equals(intensity) ? 6 : "low".equals(intensity) ? 3 : 5;
        } else if (type.contains("球") || type.contains("ball")) {
            return "high".equals(intensity) ? 8 : "low".equals(intensity) ? 4 : 6;
        }
        return 4; // 默认中等强度
    }

    private LambdaQueryWrapper<ExerciseRecord> buildWrapper(Long userId, Long memberId) {
        LambdaQueryWrapper<ExerciseRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExerciseRecord::getUserId, userId);
        if (memberId == null) {
            wrapper.isNull(ExerciseRecord::getMemberId);
        } else {
            wrapper.eq(ExerciseRecord::getMemberId, memberId);
        }
        return wrapper;
    }
}