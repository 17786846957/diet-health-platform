package com.diet.service;

import com.diet.common.BusinessException;
import com.diet.entity.ExerciseRecord;
import com.diet.mapper.ExerciseRecordMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExerciseRecordServiceTest {

    @Mock private ExerciseRecordMapper exerciseRecordMapper;
    @InjectMocks private ExerciseRecordService exerciseRecordService;

    @Test
    void addRecord_autoCalculatesCalories() {
        ExerciseRecord record = new ExerciseRecord();
        record.setUserId(1L);
        record.setRecordDate(LocalDate.now());
        record.setExerciseType("跑步");
        record.setDuration(30);
        record.setIntensity("moderate");
        when(exerciseRecordMapper.insert(any())).thenReturn(1);

        exerciseRecordService.addRecord(record);

        assertNotNull(record.getCaloriesBurned());
        assertTrue(record.getCaloriesBurned().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void addRecord_withCalories_usesProvided() {
        ExerciseRecord record = new ExerciseRecord();
        record.setUserId(1L);
        record.setRecordDate(LocalDate.now());
        record.setExerciseType("跑步");
        record.setDuration(30);
        record.setCaloriesBurned(new BigDecimal("300"));
        when(exerciseRecordMapper.insert(any())).thenReturn(1);

        exerciseRecordService.addRecord(record);
        assertEquals(new BigDecimal("300"), record.getCaloriesBurned());
    }

    @Test
    void updateRecord_notFound_throws() {
        when(exerciseRecordMapper.selectById(999L)).thenReturn(null);
        ExerciseRecord record = new ExerciseRecord();
        record.setId(999L);

        assertThrows(BusinessException.class,
                () -> exerciseRecordService.updateRecord(record, 1L));
    }

    @Test
    void updateRecord_forbidden_throws() {
        ExerciseRecord existing = new ExerciseRecord();
        existing.setId(1L);
        existing.setUserId(1L);
        when(exerciseRecordMapper.selectById(1L)).thenReturn(existing);

        ExerciseRecord update = new ExerciseRecord();
        update.setId(1L);

        assertThrows(BusinessException.class,
                () -> exerciseRecordService.updateRecord(update, 2L));
    }

    @Test
    void updateRecord_success() {
        ExerciseRecord existing = new ExerciseRecord();
        existing.setId(1L);
        existing.setUserId(1L);
        when(exerciseRecordMapper.selectById(1L)).thenReturn(existing);
        when(exerciseRecordMapper.updateById(any())).thenReturn(1);

        ExerciseRecord update = new ExerciseRecord();
        update.setId(1L);
        update.setExerciseType("游泳");
        update.setDuration(45);
        update.setIntensity("high");

        assertDoesNotThrow(() -> exerciseRecordService.updateRecord(update, 1L));
        verify(exerciseRecordMapper).updateById(existing);
    }

    @Test
    void deleteRecord_notFound_throws() {
        when(exerciseRecordMapper.selectById(999L)).thenReturn(null);
        assertThrows(BusinessException.class,
                () -> exerciseRecordService.deleteRecord(999L, 1L));
    }

    @Test
    void deleteRecord_forbidden_throws() {
        ExerciseRecord record = new ExerciseRecord();
        record.setId(1L);
        record.setUserId(1L);
        when(exerciseRecordMapper.selectById(1L)).thenReturn(record);

        assertThrows(BusinessException.class,
                () -> exerciseRecordService.deleteRecord(1L, 2L));
    }

    @Test
    void deleteRecord_success() {
        ExerciseRecord record = new ExerciseRecord();
        record.setId(1L);
        record.setUserId(1L);
        when(exerciseRecordMapper.selectById(1L)).thenReturn(record);
        when(exerciseRecordMapper.deleteById(1L)).thenReturn(1);

        assertDoesNotThrow(() -> exerciseRecordService.deleteRecord(1L, 1L));
        verify(exerciseRecordMapper).deleteById(1L);
    }

    @Test
    void getDailyStats_empty() {
        when(exerciseRecordMapper.selectList(any())).thenReturn(Collections.emptyList());

        Map<String, Object> stats = exerciseRecordService.getDailyStats(1L, null, LocalDate.now());
        assertEquals(0, stats.get("totalDuration"));
        assertEquals(0, stats.get("recordCount"));
    }

    @Test
    void getWeeklyStats_returnsDatesAndDurations() {
        when(exerciseRecordMapper.selectList(any())).thenReturn(Collections.emptyList());

        Map<String, Object> result = exerciseRecordService.getWeeklyStats(1L, null, LocalDate.now());
        assertNotNull(result.get("dates"));
        assertNotNull(result.get("durations"));
        assertNotNull(result.get("calories"));
    }
}
