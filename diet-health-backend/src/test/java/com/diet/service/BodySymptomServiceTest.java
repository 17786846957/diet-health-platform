package com.diet.service;

import com.diet.common.BusinessException;
import com.diet.entity.BodySymptom;
import com.diet.mapper.BodySymptomMapper;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BodySymptomServiceTest {

    @Mock private BodySymptomMapper bodySymptomMapper;
    @InjectMocks private BodySymptomService bodySymptomService;

    @Test
    void addRecord_success() {
        BodySymptom record = new BodySymptom();
        record.setUserId(1L);
        record.setSymptomType("headache");
        record.setSeverity(3);
        record.setRecordDate(LocalDate.now());
        when(bodySymptomMapper.insert(any())).thenReturn(1);

        assertDoesNotThrow(() -> bodySymptomService.addRecord(record));
        verify(bodySymptomMapper).insert(record);
    }

    @Test
    void deleteRecord_notFound_throws() {
        when(bodySymptomMapper.selectById(999L)).thenReturn(null);
        assertThrows(BusinessException.class,
                () -> bodySymptomService.deleteRecord(999L, 1L));
    }

    @Test
    void deleteRecord_forbidden_throws() {
        BodySymptom record = new BodySymptom();
        record.setId(1L);
        record.setUserId(1L);
        when(bodySymptomMapper.selectById(1L)).thenReturn(record);

        assertThrows(BusinessException.class,
                () -> bodySymptomService.deleteRecord(1L, 2L));
    }

    @Test
    void deleteRecord_success() {
        BodySymptom record = new BodySymptom();
        record.setId(1L);
        record.setUserId(1L);
        when(bodySymptomMapper.selectById(1L)).thenReturn(record);
        when(bodySymptomMapper.deleteById(1L)).thenReturn(1);

        assertDoesNotThrow(() -> bodySymptomService.deleteRecord(1L, 1L));
        verify(bodySymptomMapper).deleteById(1L);
    }

    @Test
    void getSymptomAnalysis_empty() {
        when(bodySymptomMapper.selectList(any())).thenReturn(Collections.emptyList());

        Map<String, Object> result = bodySymptomService.getSymptomAnalysis(1L, null, 7);
        assertEquals(0, result.get("totalRecords"));
    }

    @Test
    void getSymptomAnalysis_withRecords_groupsByType() {
        BodySymptom s1 = new BodySymptom();
        s1.setSymptomType("headache");
        s1.setSeverity(3);
        s1.setRecordDate(LocalDate.now());

        BodySymptom s2 = new BodySymptom();
        s2.setSymptomType("headache");
        s2.setSeverity(5);
        s2.setRecordDate(LocalDate.now().minusDays(1));

        when(bodySymptomMapper.selectList(any())).thenReturn(java.util.Arrays.asList(s1, s2));

        Map<String, Object> result = bodySymptomService.getSymptomAnalysis(1L, null, 7);
        assertEquals(2, result.get("totalRecords"));
        Map<String, Integer> typeCount = (Map<String, Integer>) result.get("typeCount");
        assertEquals(2, typeCount.get("headache"));
    }
}
