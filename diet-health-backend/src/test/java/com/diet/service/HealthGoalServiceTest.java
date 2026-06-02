package com.diet.service;

import com.diet.common.BusinessException;
import com.diet.entity.HealthGoal;
import com.diet.entity.WeightRecord;
import com.diet.mapper.HealthGoalMapper;
import com.diet.mapper.WeightRecordMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthGoalServiceTest {

    @Mock private HealthGoalMapper healthGoalMapper;
    @Mock private WeightRecordMapper weightRecordMapper;
    @InjectMocks private HealthGoalService healthGoalService;

    @Test
    void createGoal_setsDefaults() {
        HealthGoal goal = new HealthGoal();
        goal.setUserId(1L);
        goal.setGoalType("lose_weight");
        when(healthGoalMapper.insert(any())).thenReturn(1);

        healthGoalService.createGoal(goal);

        assertEquals(LocalDate.now(), goal.getStartDate());
        assertEquals("active", goal.getStatus());
        assertEquals(BigDecimal.ZERO, goal.getProgress());
    }

    @Test
    void updateGoal_notFound_throws() {
        when(healthGoalMapper.selectById(999L)).thenReturn(null);
        HealthGoal goal = new HealthGoal();
        goal.setId(999L);

        assertThrows(BusinessException.class,
                () -> healthGoalService.updateGoal(goal, 1L));
    }

    @Test
    void updateGoal_forbidden_throws() {
        HealthGoal existing = new HealthGoal();
        existing.setId(1L);
        existing.setUserId(1L);
        when(healthGoalMapper.selectById(1L)).thenReturn(existing);

        HealthGoal update = new HealthGoal();
        update.setId(1L);

        assertThrows(BusinessException.class,
                () -> healthGoalService.updateGoal(update, 2L));
    }

    @Test
    void updateGoal_success() {
        HealthGoal existing = new HealthGoal();
        existing.setId(1L);
        existing.setUserId(1L);
        when(healthGoalMapper.selectById(1L)).thenReturn(existing);
        when(healthGoalMapper.updateById(any())).thenReturn(1);

        HealthGoal update = new HealthGoal();
        update.setId(1L);
        update.setGoalType("muscle");
        update.setTargetWeight(new BigDecimal("75"));

        assertDoesNotThrow(() -> healthGoalService.updateGoal(update, 1L));
        assertEquals("muscle", existing.getGoalType());
        verify(healthGoalMapper).updateById(existing);
    }

    @Test
    void completeGoal_setsStatusAndEndDate() {
        HealthGoal goal = new HealthGoal();
        goal.setId(1L);
        goal.setUserId(1L);
        goal.setStatus("active");
        when(healthGoalMapper.selectById(1L)).thenReturn(goal);
        when(healthGoalMapper.updateById(any())).thenReturn(1);

        assertDoesNotThrow(() -> healthGoalService.completeGoal(1L, 1L));
        assertEquals("completed", goal.getStatus());
        assertEquals(LocalDate.now(), goal.getEndDate());
        assertEquals(new BigDecimal("100"), goal.getProgress());
    }

    @Test
    void cancelGoal_setsStatusAndEndDate() {
        HealthGoal goal = new HealthGoal();
        goal.setId(1L);
        goal.setUserId(1L);
        goal.setStatus("active");
        when(healthGoalMapper.selectById(1L)).thenReturn(goal);
        when(healthGoalMapper.updateById(any())).thenReturn(1);

        assertDoesNotThrow(() -> healthGoalService.cancelGoal(1L, 1L));
        assertEquals("cancelled", goal.getStatus());
        assertEquals(LocalDate.now(), goal.getEndDate());
    }

    @Test
    void listGoals_empty() {
        when(healthGoalMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<HealthGoal> result = healthGoalService.listGoals(1L, null, null);
        assertTrue(result.isEmpty());
    }

    @Test
    void listGoals_withStatusFilter() {
        when(healthGoalMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<HealthGoal> result = healthGoalService.listGoals(1L, null, "active");
        assertTrue(result.isEmpty());
    }

    @Test
    void updateProgress_noGoal_doesNothing() {
        when(healthGoalMapper.selectById(999L)).thenReturn(null);
        assertDoesNotThrow(() -> healthGoalService.updateProgress(999L, new BigDecimal("70")));
    }

    @Test
    void updateProgress_calculatesPercentage() {
        HealthGoal goal = new HealthGoal();
        goal.setId(1L);
        goal.setUserId(1L);
        goal.setStatus("active");
        goal.setTargetWeight(new BigDecimal("65"));
        when(healthGoalMapper.selectById(1L)).thenReturn(goal);
        when(healthGoalMapper.updateById(any())).thenReturn(1);

        WeightRecord startRecord = new WeightRecord();
        startRecord.setWeight(new BigDecimal("80"));
        when(weightRecordMapper.selectOne(any())).thenReturn(startRecord);

        healthGoalService.updateProgress(1L, new BigDecimal("70"));
        assertNotNull(goal.getProgress());
    }
}
