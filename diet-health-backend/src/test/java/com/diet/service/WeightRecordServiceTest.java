package com.diet.service;

import com.diet.common.BusinessException;
import com.diet.entity.WeightRecord;
import com.diet.mapper.WeightRecordMapper;
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
class WeightRecordServiceTest {

    @Mock private WeightRecordMapper weightRecordMapper;
    @InjectMocks private WeightRecordService weightRecordService;

    @Test
    void addOrUpdateRecord_insertsNew() {
        WeightRecord record = new WeightRecord();
        record.setUserId(1L);
        record.setRecordDate(LocalDate.now());
        record.setWeight(new BigDecimal("70"));
        when(weightRecordMapper.selectOne(any())).thenReturn(null);
        when(weightRecordMapper.insert(any())).thenReturn(1);

        weightRecordService.addOrUpdateRecord(record);
        verify(weightRecordMapper).insert(record);
    }

    @Test
    void addOrUpdateRecord_updatesExisting() {
        WeightRecord existing = new WeightRecord();
        existing.setId(1L);
        existing.setWeight(new BigDecimal("70"));
        when(weightRecordMapper.selectOne(any())).thenReturn(existing);
        when(weightRecordMapper.updateById(any())).thenReturn(1);

        WeightRecord record = new WeightRecord();
        record.setUserId(1L);
        record.setRecordDate(LocalDate.now());
        record.setWeight(new BigDecimal("71"));

        weightRecordService.addOrUpdateRecord(record);
        assertEquals(new BigDecimal("71"), existing.getWeight());
        verify(weightRecordMapper).updateById(existing);
    }

    @Test
    void deleteRecord_notFound_throws() {
        when(weightRecordMapper.selectById(999L)).thenReturn(null);
        assertThrows(BusinessException.class,
                () -> weightRecordService.deleteRecord(999L, 1L));
    }

    @Test
    void deleteRecord_forbidden_throws() {
        WeightRecord record = new WeightRecord();
        record.setId(1L);
        record.setUserId(1L);
        when(weightRecordMapper.selectById(1L)).thenReturn(record);

        assertThrows(BusinessException.class,
                () -> weightRecordService.deleteRecord(1L, 2L));
    }

    @Test
    void deleteRecord_success() {
        WeightRecord record = new WeightRecord();
        record.setId(1L);
        record.setUserId(1L);
        when(weightRecordMapper.selectById(1L)).thenReturn(record);
        when(weightRecordMapper.deleteById(1L)).thenReturn(1);

        assertDoesNotThrow(() -> weightRecordService.deleteRecord(1L, 1L));
        verify(weightRecordMapper).deleteById(1L);
    }

    @Test
    void getLatest_noRecords_returnsNull() {
        when(weightRecordMapper.selectOne(any())).thenReturn(null);
        assertNull(weightRecordService.getLatest(1L, null));
    }

    @Test
    void getWeightTrend_empty() {
        when(weightRecordMapper.selectList(any())).thenReturn(Collections.emptyList());

        Map<String, Object> result = weightRecordService.getWeightTrend(1L, null, 7);
        assertEquals("stable", result.get("trend"));
        assertEquals(0, result.get("recordCount"));
    }

    @Test
    void getWeightTrend_withRecords_calculatesTrend() {
        WeightRecord r1 = new WeightRecord();
        r1.setRecordDate(LocalDate.now().minusDays(6));
        r1.setWeight(new BigDecimal("75"));
        r1.setBodyFat(new BigDecimal("20"));

        WeightRecord r2 = new WeightRecord();
        r2.setRecordDate(LocalDate.now());
        r2.setWeight(new BigDecimal("73"));
        r2.setBodyFat(new BigDecimal("19"));

        when(weightRecordMapper.selectList(any())).thenReturn(java.util.Arrays.asList(r1, r2));

        Map<String, Object> result = weightRecordService.getWeightTrend(1L, null, 7);
        assertEquals("decreasing", result.get("trend"));
        assertEquals(2, result.get("recordCount"));
    }
}
