package com.diet.service;

import com.diet.common.BusinessException;
import com.diet.entity.WaterRecord;
import com.diet.mapper.WaterRecordMapper;
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
class WaterRecordServiceTest {

    @Mock private WaterRecordMapper waterRecordMapper;
    @InjectMocks private WaterRecordService waterRecordService;

    @Test
    void addRecord_success() {
        WaterRecord record = new WaterRecord();
        record.setUserId(1L);
        record.setRecordDate(LocalDate.now());
        record.setAmount(new BigDecimal("250"));
        when(waterRecordMapper.insert(any())).thenReturn(1);

        assertDoesNotThrow(() -> waterRecordService.addRecord(record));
        verify(waterRecordMapper).insert(record);
    }

    @Test
    void deleteRecord_notFound_throws() {
        when(waterRecordMapper.selectById(999L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> waterRecordService.deleteRecord(999L, 1L));
    }

    @Test
    void deleteRecord_forbidden_throws() {
        WaterRecord record = new WaterRecord();
        record.setId(1L);
        record.setUserId(1L);
        when(waterRecordMapper.selectById(1L)).thenReturn(record);

        assertThrows(BusinessException.class, () -> waterRecordService.deleteRecord(1L, 2L));
    }

    @Test
    void deleteRecord_success() {
        WaterRecord record = new WaterRecord();
        record.setId(1L);
        record.setUserId(1L);
        when(waterRecordMapper.selectById(1L)).thenReturn(record);
        when(waterRecordMapper.deleteById(1L)).thenReturn(1);

        assertDoesNotThrow(() -> waterRecordService.deleteRecord(1L, 1L));
        verify(waterRecordMapper).deleteById(1L);
    }

    @Test
    void getDailyStats_empty() {
        when(waterRecordMapper.selectList(any())).thenReturn(Collections.emptyList());

        Map<String, Object> stats = waterRecordService.getDailyStats(1L, null, LocalDate.now());
        assertEquals(BigDecimal.ZERO, stats.get("totalAmount"));
        assertEquals(0, stats.get("recordCount"));
    }

    @Test
    void getWeeklyStats_returnsDatesAndAmounts() {
        when(waterRecordMapper.selectList(any())).thenReturn(Collections.emptyList());

        Map<String, Object> result = waterRecordService.getWeeklyStats(1L, null, LocalDate.now());
        assertNotNull(result.get("dates"));
        assertNotNull(result.get("amounts"));
    }
}
