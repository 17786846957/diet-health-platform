package com.diet.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diet.entity.DietRecord;
import com.diet.entity.DietRecordDetail;
import com.diet.entity.Food;
import com.diet.mapper.DietRecordDetailMapper;
import com.diet.mapper.DietRecordMapper;
import com.diet.mapper.FoodMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataConsistencyServiceTest {

    @Mock private DietRecordMapper dietRecordMapper;
    @Mock private DietRecordDetailMapper detailMapper;
    @Mock private FoodMapper foodMapper;
    @InjectMocks private DataConsistencyService dataConsistencyService;

    @Test
    void verifyTotalCalories_emptyRecords_doesNothing() {
        Page<DietRecord> emptyPage = new Page<>(1, 500);
        when(dietRecordMapper.selectPage(any(), any())).thenReturn(emptyPage);

        dataConsistencyService.verifyTotalCalories();
        verify(dietRecordMapper, never()).updateById(any());
    }

    @Test
    void verifyTotalCalories_consistentRecords_noFix() {
        DietRecord record = new DietRecord();
        record.setId(1L);
        record.setTotalCalories(232.0);

        Page<DietRecord> page = new Page<>(1, 500);
        page.setRecords(Collections.singletonList(record));
        when(dietRecordMapper.selectPage(any(), any())).thenReturn(page);

        DietRecordDetail detail = new DietRecordDetail();
        detail.setRecordId(1L);
        detail.setFoodId(1L);
        detail.setAmount(200.0);
        when(detailMapper.selectList(any())).thenReturn(Collections.singletonList(detail));

        Food food = new Food();
        food.setId(1L);
        food.setCalories(116.0);
        when(foodMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(food));

        dataConsistencyService.verifyTotalCalories();
        verify(dietRecordMapper, never()).updateById(any());
    }

    @Test
    void verifyTotalCalories_inconsistent_fixesRecord() {
        DietRecord record = new DietRecord();
        record.setId(1L);
        record.setTotalCalories(999.0); // wrong value

        Page<DietRecord> page = new Page<>(1, 500);
        page.setRecords(Collections.singletonList(record));
        when(dietRecordMapper.selectPage(any(), any())).thenReturn(page);

        DietRecordDetail detail = new DietRecordDetail();
        detail.setRecordId(1L);
        detail.setFoodId(1L);
        detail.setAmount(200.0);
        when(detailMapper.selectList(any())).thenReturn(Collections.singletonList(detail));

        Food food = new Food();
        food.setId(1L);
        food.setCalories(116.0); // 116 * 200 / 100 = 232
        when(foodMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(food));
        when(dietRecordMapper.updateById(any())).thenReturn(1);

        dataConsistencyService.verifyTotalCalories();
        verify(dietRecordMapper).updateById(record);
    }
}
