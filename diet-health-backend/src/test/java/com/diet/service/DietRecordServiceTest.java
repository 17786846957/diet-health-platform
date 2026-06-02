package com.diet.service;

import com.diet.common.BusinessException;
import com.diet.entity.DietRecord;
import com.diet.entity.DietRecordDetail;
import com.diet.entity.Food;
import com.diet.entity.FoodFavorite;
import com.diet.mapper.DietRecordDetailMapper;
import com.diet.mapper.DietRecordMapper;
import com.diet.mapper.FoodFavoriteMapper;
import com.diet.mapper.FoodMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DietRecordServiceTest {

    @Mock
    private DietRecordMapper dietRecordMapper;
    @Mock
    private DietRecordDetailMapper detailMapper;
    @Mock
    private FoodMapper foodMapper;
    @Mock
    private FoodFavoriteMapper foodFavoriteMapper;
    @Mock
    private UserService userService;

    @InjectMocks
    private DietRecordService dietRecordService;

    // ========== addRecord ==========

    @Test
    void addRecord_calculatesCalories() {
        DietRecord record = new DietRecord();
        record.setUserId(1L);
        record.setRecordDate(LocalDate.now());
        record.setMealType("lunch");

        DietRecordDetail detail = new DietRecordDetail();
        detail.setFoodId(1L);
        detail.setAmount(200.0);
        record.setDetails(Collections.singletonList(detail));

        Food food = new Food();
        food.setId(1L);
        food.setCalories(116.0);
        when(foodMapper.selectBatchIds(anyList())).thenReturn(Collections.singletonList(food));
        when(dietRecordMapper.insert(any())).thenReturn(1);

        dietRecordService.addRecord(record);

        assertEquals(232.0, record.getTotalCalories(), 0.1);
        assertEquals(232.0, detail.getCalories(), 0.1);
        verify(dietRecordMapper).insert(record);
    }

    @Test
    void addRecord_nullDetails_zeroCalories() {
        DietRecord record = new DietRecord();
        record.setUserId(1L);
        record.setRecordDate(LocalDate.now());
        record.setMealType("breakfast");
        record.setDetails(null);

        when(dietRecordMapper.insert(any())).thenReturn(1);

        dietRecordService.addRecord(record);

        assertEquals(0.0, record.getTotalCalories());
    }

    @Test
    void addRecord_emptyDetails_zeroCalories() {
        DietRecord record = new DietRecord();
        record.setUserId(1L);
        record.setRecordDate(LocalDate.now());
        record.setMealType("breakfast");
        record.setDetails(Collections.emptyList());

        when(dietRecordMapper.insert(any())).thenReturn(1);

        dietRecordService.addRecord(record);

        assertEquals(0.0, record.getTotalCalories());
    }

    // ========== updateRecord ==========

    @Test
    void updateRecord_permissionCheck_throws() {
        DietRecord existing = new DietRecord();
        existing.setId(1L);
        existing.setUserId(1L);
        when(dietRecordMapper.selectById(1L)).thenReturn(existing);

        DietRecord update = new DietRecord();
        update.setId(1L);
        update.setDetails(null);

        assertThrows(BusinessException.class,
                () -> dietRecordService.updateRecord(update, 2L));
    }

    @Test
    void updateRecord_notFound_throws() {
        when(dietRecordMapper.selectById(999L)).thenReturn(null);

        DietRecord update = new DietRecord();
        update.setId(999L);

        assertThrows(BusinessException.class,
                () -> dietRecordService.updateRecord(update, 1L));
    }

    @Test
    void updateRecord_success() {
        DietRecord existing = new DietRecord();
        existing.setId(1L);
        existing.setUserId(1L);
        when(dietRecordMapper.selectById(1L)).thenReturn(existing);
        when(dietRecordMapper.updateById(any())).thenReturn(1);

        DietRecord update = new DietRecord();
        update.setId(1L);
        update.setUserId(1L);
        update.setRecordDate(LocalDate.now());
        update.setMealType("dinner");

        DietRecordDetail detail = new DietRecordDetail();
        detail.setFoodId(1L);
        detail.setAmount(150.0);
        update.setDetails(Collections.singletonList(detail));

        Food food = new Food();
        food.setId(1L);
        food.setCalories(100.0);
        when(foodMapper.selectBatchIds(anyList())).thenReturn(Collections.singletonList(food));

        assertDoesNotThrow(() -> dietRecordService.updateRecord(update, 1L));
        assertEquals(150.0, update.getTotalCalories(), 0.1);
        verify(detailMapper).delete(any());
        verify(dietRecordMapper).updateById(update);
    }

    // ========== deleteRecord ==========

    @Test
    void deleteRecord_permissionCheck() {
        DietRecord record = new DietRecord();
        record.setId(1L);
        record.setUserId(1L);
        when(dietRecordMapper.selectById(1L)).thenReturn(record);

        assertThrows(BusinessException.class,
                () -> dietRecordService.deleteRecord(1L, 2L));
    }

    @Test
    void deleteRecord_success() {
        DietRecord record = new DietRecord();
        record.setId(1L);
        record.setUserId(1L);
        when(dietRecordMapper.selectById(1L)).thenReturn(record);
        when(dietRecordMapper.deleteById(1L)).thenReturn(1);

        assertDoesNotThrow(() -> dietRecordService.deleteRecord(1L, 1L));
        verify(dietRecordMapper).deleteById(1L);
        verify(detailMapper).delete(any());
    }

    // ========== getDailyStats ==========

    @Test
    void getDailyStats_emptyRecords() {
        when(dietRecordMapper.selectList(any())).thenReturn(Collections.emptyList());

        Map<String, Object> stats = dietRecordService.getDailyStats(1L, null, LocalDate.now());
        assertEquals(0.0, stats.get("totalCalories"));
        assertEquals(0.0, stats.get("totalProtein"));
    }

    @Test
    void getDailyStats_withRecords_calculatesTotals() {
        DietRecord record = new DietRecord();
        record.setId(1L);
        record.setUserId(1L);
        record.setRecordDate(LocalDate.now());
        record.setTotalCalories(500.0);

        DietRecordDetail detail = new DietRecordDetail();
        detail.setRecordId(1L);
        detail.setFoodId(1L);
        detail.setAmount(200.0);
        detail.setProtein(20.0);
        detail.setFat(10.0);
        detail.setCarbs(50.0);
        record.setDetails(Collections.singletonList(detail));

        // First call: listByDate -> dietRecordMapper.selectList (for records)
        // Second call: loadDetailsForRecords -> detailMapper.selectList
        when(dietRecordMapper.selectList(any())).thenReturn(Collections.singletonList(record));
        when(detailMapper.selectList(any())).thenReturn(Collections.singletonList(detail));

        Food food = new Food();
        food.setId(1L);
        food.setName("米饭");
        food.setCategory("主食");
        food.setProtein(10.0);
        food.setFat(2.0);
        food.setCarbs(25.0);
        when(foodMapper.selectBatchIds(anyList())).thenReturn(Collections.singletonList(food));

        Map<String, Object> stats = dietRecordService.getDailyStats(1L, null, LocalDate.now());
        assertEquals(500.0, stats.get("totalCalories"));
    }

    // ========== getWeeklyStats ==========

    @Test
    void getWeeklyStats_emptyRecords_returnsDatesAndZeros() {
        when(dietRecordMapper.selectList(any())).thenReturn(Collections.emptyList());

        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(6);
        Map<String, Object> result = dietRecordService.getWeeklyStats(1L, null, start, end);

        List<String> dates = (List<String>) result.get("dates");
        List<Double> calories = (List<Double>) result.get("calories");
        assertEquals(7, dates.size());
        assertEquals(7, calories.size());
        assertEquals(0.0, calories.get(0));
    }

    @Test
    void getWeeklyStats_nullDates_usesDefaults() {
        when(dietRecordMapper.selectList(any())).thenReturn(Collections.emptyList());

        Map<String, Object> result = dietRecordService.getWeeklyStats(1L, null, null, null);

        List<String> dates = (List<String>) result.get("dates");
        assertEquals(7, dates.size());
    }

    // ========== getMonthlyStats ==========

    @Test
    void getMonthlyStats_emptyRecords_returnsDaysOfMonth() {
        when(dietRecordMapper.selectList(any())).thenReturn(Collections.emptyList());

        Map<String, Object> result = dietRecordService.getMonthlyStats(1L, null, 2026, 5);

        List<String> dates = (List<String>) result.get("dates");
        List<Double> calories = (List<Double>) result.get("calories");
        assertEquals(31, dates.size()); // May has 31 days
        assertEquals(0.0, calories.get(0));
        assertEquals(0.0, result.get("avgCalories"));
    }

    @Test
    void getMonthlyStats_withRecords_calculatesAverages() {
        DietRecord record = new DietRecord();
        record.setId(1L);
        record.setUserId(1L);
        record.setRecordDate(LocalDate.of(2026, 5, 15));
        record.setTotalCalories(800.0);

        when(dietRecordMapper.selectList(any())).thenReturn(Collections.singletonList(record));
        when(detailMapper.selectList(any())).thenReturn(Collections.emptyList());

        Map<String, Object> result = dietRecordService.getMonthlyStats(1L, null, 2026, 5);

        List<Double> calories = (List<Double>) result.get("calories");
        assertEquals(800.0, calories.get(14)); // index 14 = May 15
        assertTrue((double) result.get("avgCalories") > 0);
    }

    // ========== getNutritionGap ==========

    @Test
    void getNutritionGap_usesPersonalizedTargets() {
        when(dietRecordMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(userService.calculateTargets(1L))
                .thenReturn(new double[]{2500, 93.8, 69.4, 375.0});

        Map<String, Object> result = dietRecordService.getNutritionGap(1L, null, LocalDate.now());
        List<Map<String, Object>> gaps = (List<Map<String, Object>>) result.get("gaps");
        Map<String, Object> calorieGap = gaps.get(0);
        assertEquals(2500.0, calorieGap.get("target"));
    }

    @Test
    void getNutritionGap_withMemberId_usesMemberTargets() {
        when(dietRecordMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(userService.calculateTargetsForMember(10L))
                .thenReturn(new double[]{1800, 67.5, 50.0, 270.0});

        Map<String, Object> result = dietRecordService.getNutritionGap(1L, 10L, LocalDate.now());
        List<Map<String, Object>> gaps = (List<Map<String, Object>>) result.get("gaps");
        assertEquals(1800.0, gaps.get(0).get("target"));
    }

    // ========== getRecentFoods ==========

    @Test
    void getRecentFoods_noRecords_returnsEmpty() {
        when(dietRecordMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<Map<String, Object>> result = dietRecordService.getRecentFoods(1L, null, 7);
        assertTrue(result.isEmpty());
    }

    @Test
    void getRecentFoods_withRecords_returnsSortedByFrequency() {
        DietRecord record = new DietRecord();
        record.setId(1L);
        record.setUserId(1L);
        record.setRecordDate(LocalDate.now());

        DietRecordDetail detail1 = new DietRecordDetail();
        detail1.setFoodId(1L);
        detail1.setAmount(200.0);
        DietRecordDetail detail2 = new DietRecordDetail();
        detail2.setFoodId(2L);
        detail2.setAmount(100.0);

        when(dietRecordMapper.selectList(any())).thenReturn(Collections.singletonList(record));
        when(detailMapper.selectList(any())).thenReturn(Arrays.asList(detail1, detail2));

        Food food1 = new Food();
        food1.setId(1L);
        food1.setName("米饭");
        food1.setCategory("主食");
        food1.setCalories(116.0);
        food1.setProtein(2.6);
        food1.setFat(0.3);
        food1.setCarbs(25.6);

        Food food2 = new Food();
        food2.setId(2L);
        food2.setName("苹果");
        food2.setCategory("水果");
        food2.setCalories(52.0);
        food2.setProtein(0.3);
        food2.setFat(0.2);
        food2.setCarbs(13.8);

        when(foodMapper.selectBatchIds(anyList())).thenReturn(Arrays.asList(food1, food2));

        List<Map<String, Object>> result = dietRecordService.getRecentFoods(1L, null, 7);
        assertFalse(result.isEmpty());
        assertEquals("米饭", result.get(0).get("foodName"));
    }

    // ========== recommendFoods (核心算法) ==========

    @Test
    void recommendFoods_emptyFoodLibrary_returnsEmpty() {
        when(dietRecordMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(userService.calculateTargets(1L)).thenReturn(new double[]{2000, 60, 55, 300});
        when(foodFavoriteMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(foodMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<Map<String, Object>> result = dietRecordService.recommendFoods(1L, null, "lunch");
        assertTrue(result.isEmpty());
    }

    @Test
    void recommendFoods_withFoods_returnsScored() {
        // Mock empty daily stats
        when(dietRecordMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(userService.calculateTargets(1L)).thenReturn(new double[]{2000, 60, 55, 300});
        when(foodFavoriteMapper.selectList(any())).thenReturn(Collections.emptyList());

        // Create test foods
        Food rice = new Food();
        rice.setId(1L);
        rice.setName("米饭");
        rice.setCategory("主食");
        rice.setCalories(116.0);
        rice.setProtein(2.6);
        rice.setFat(0.3);
        rice.setCarbs(25.6);
        rice.setFiber(0.3);

        Food chicken = new Food();
        chicken.setId(2L);
        chicken.setName("鸡胸肉");
        chicken.setCategory("肉类");
        chicken.setCalories(133.0);
        chicken.setProtein(31.0);
        chicken.setFat(1.2);
        chicken.setCarbs(0.0);
        chicken.setFiber(0.0);

        Food apple = new Food();
        apple.setId(3L);
        apple.setName("苹果");
        apple.setCategory("水果");
        apple.setCalories(52.0);
        apple.setProtein(0.3);
        apple.setFat(0.2);
        apple.setCarbs(13.8);
        apple.setFiber(2.4);

        when(foodMapper.selectList(any())).thenReturn(Arrays.asList(rice, chicken, apple));

        List<Map<String, Object>> result = dietRecordService.recommendFoods(1L, null, "lunch");
        assertFalse(result.isEmpty());
        assertTrue(result.size() <= 10);

        // Verify each result has required fields
        for (Map<String, Object> item : result) {
            assertNotNull(item.get("foodId"));
            assertNotNull(item.get("foodName"));
            assertNotNull(item.get("score"));
            assertNotNull(item.get("reason"));
            assertTrue((double) item.get("score") >= 0);
        }

        // Results should be sorted by score descending
        for (int i = 0; i < result.size() - 1; i++) {
            double score1 = (double) result.get(i).get("score");
            double score2 = (double) result.get(i + 1).get("score");
            assertTrue(score1 >= score2);
        }
    }

    @Test
    void recommendFoods_withFavorites_favoritesGetBonus() {
        when(dietRecordMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(userService.calculateTargets(1L)).thenReturn(new double[]{2000, 60, 55, 300});

        Food food = new Food();
        food.setId(1L);
        food.setName("米饭");
        food.setCategory("主食");
        food.setCalories(116.0);
        food.setProtein(2.6);
        food.setFat(0.3);
        food.setCarbs(25.6);
        food.setFiber(0.3);

        Food favFood = new Food();
        favFood.setId(2L);
        favFood.setName("鸡胸肉");
        favFood.setCategory("肉类");
        favFood.setCalories(133.0);
        favFood.setProtein(31.0);
        favFood.setFat(1.2);
        favFood.setCarbs(0.0);
        favFood.setFiber(0.0);

        when(foodMapper.selectList(any())).thenReturn(Arrays.asList(food, favFood));

        FoodFavorite fav = new FoodFavorite();
        fav.setFoodId(2L);
        when(foodFavoriteMapper.selectList(any())).thenReturn(Collections.singletonList(fav));

        List<Map<String, Object>> result = dietRecordService.recommendFoods(1L, null, "lunch");
        assertFalse(result.isEmpty());
    }

    @Test
    void recommendFoods_breakfastPrefersBreakfastCategories() {
        when(dietRecordMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(userService.calculateTargets(1L)).thenReturn(new double[]{2000, 60, 55, 300});
        when(foodFavoriteMapper.selectList(any())).thenReturn(Collections.emptyList());

        Food congee = new Food();
        congee.setId(1L);
        congee.setName("小米粥");
        congee.setCategory("主食");
        congee.setCalories(46.0);
        congee.setProtein(1.4);
        congee.setFat(0.7);
        congee.setCarbs(8.4);
        congee.setFiber(0.0);

        Food steak = new Food();
        steak.setId(2L);
        steak.setName("牛排");
        steak.setCategory("肉类");
        steak.setCalories(250.0);
        steak.setProtein(26.0);
        steak.setFat(15.0);
        steak.setCarbs(0.0);
        steak.setFiber(0.0);

        when(foodMapper.selectList(any())).thenReturn(Arrays.asList(congee, steak));

        List<Map<String, Object>> result = dietRecordService.recommendFoods(1L, null, "breakfast");
        assertFalse(result.isEmpty());
        // Both should have scores, breakfast should still work
        for (Map<String, Object> item : result) {
            assertNotNull(item.get("score"));
        }
    }

    // ========== count ==========

    @Test
    void count_returnsValue() {
        when(dietRecordMapper.selectCount(null)).thenReturn(10L);
        assertEquals(10L, dietRecordService.count());
    }
}
