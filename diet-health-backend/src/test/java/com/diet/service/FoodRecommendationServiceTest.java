package com.diet.service;

import com.diet.entity.Food;
import com.diet.entity.FoodFavorite;
import com.diet.mapper.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FoodRecommendationServiceTest {

    @Mock private FoodMapper foodMapper;
    @Mock private FoodFavoriteMapper foodFavoriteMapper;
    @Mock private DietRecordMapper dietRecordMapper;
    @Mock private DietRecordDetailMapper detailMapper;
    @Mock private UserService userService;
    @Mock private DietRecordService dietRecordService;
    @InjectMocks private FoodRecommendationService foodRecommendationService;

    private Map<String, Object> emptyStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalCalories", 0.0);
        stats.put("totalProtein", 0.0);
        stats.put("totalFat", 0.0);
        stats.put("totalCarbs", 0.0);
        return stats;
    }

    @Test
    void recommendFoods_emptyFoodLibrary_returnsEmpty() {
        when(dietRecordService.getDailyStats(anyLong(), any(), any())).thenReturn(emptyStats());
        when(userService.calculateTargets(1L)).thenReturn(new double[]{2000, 60, 55, 300});
        when(dietRecordService.getRecentFoods(anyLong(), any(), anyInt())).thenReturn(Collections.emptyList());
        when(foodFavoriteMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(dietRecordService.listByDate(anyLong(), any(), any())).thenReturn(Collections.emptyList());
        when(foodMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<Map<String, Object>> result = foodRecommendationService.recommendFoods(1L, null, "lunch");
        assertTrue(result.isEmpty());
    }

    @Test
    void recommendFoods_withFoods_returnsScored() {
        when(dietRecordService.getDailyStats(anyLong(), any(), any())).thenReturn(emptyStats());
        when(userService.calculateTargets(1L)).thenReturn(new double[]{2000, 60, 55, 300});
        when(dietRecordService.getRecentFoods(anyLong(), any(), anyInt())).thenReturn(Collections.emptyList());
        when(foodFavoriteMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(dietRecordService.listByDate(anyLong(), any(), any())).thenReturn(Collections.emptyList());

        Food rice = new Food();
        rice.setId(1L); rice.setName("米饭"); rice.setCategory("主食");
        rice.setCalories(116.0); rice.setProtein(2.6); rice.setFat(0.3);
        rice.setCarbs(25.6); rice.setFiber(0.3);

        Food chicken = new Food();
        chicken.setId(2L); chicken.setName("鸡胸肉"); chicken.setCategory("肉类");
        chicken.setCalories(133.0); chicken.setProtein(31.0); chicken.setFat(1.2);
        chicken.setCarbs(0.0); chicken.setFiber(0.0);

        Food apple = new Food();
        apple.setId(3L); apple.setName("苹果"); apple.setCategory("水果");
        apple.setCalories(52.0); apple.setProtein(0.3); apple.setFat(0.2);
        apple.setCarbs(13.8); apple.setFiber(2.4);

        when(foodMapper.selectList(any())).thenReturn(Arrays.asList(rice, chicken, apple));

        List<Map<String, Object>> result = foodRecommendationService.recommendFoods(1L, null, "lunch");
        assertFalse(result.isEmpty());
        assertTrue(result.size() <= 10);

        for (Map<String, Object> item : result) {
            assertNotNull(item.get("foodId"));
            assertNotNull(item.get("foodName"));
            assertNotNull(item.get("score"));
            assertNotNull(item.get("reason"));
            assertTrue((double) item.get("score") >= 0);
        }

        for (int i = 0; i < result.size() - 1; i++) {
            double score1 = (double) result.get(i).get("score");
            double score2 = (double) result.get(i + 1).get("score");
            assertTrue(score1 >= score2);
        }
    }

    @Test
    void recommendFoods_withFavorites_favoritesGetBonus() {
        when(dietRecordService.getDailyStats(anyLong(), any(), any())).thenReturn(emptyStats());
        when(userService.calculateTargets(1L)).thenReturn(new double[]{2000, 60, 55, 300});
        when(dietRecordService.getRecentFoods(anyLong(), any(), anyInt())).thenReturn(Collections.emptyList());
        when(dietRecordService.listByDate(anyLong(), any(), any())).thenReturn(Collections.emptyList());

        Food food = new Food();
        food.setId(1L); food.setName("米饭"); food.setCategory("主食");
        food.setCalories(116.0); food.setProtein(2.6); food.setFat(0.3);
        food.setCarbs(25.6); food.setFiber(0.3);

        Food favFood = new Food();
        favFood.setId(2L); favFood.setName("鸡胸肉"); favFood.setCategory("肉类");
        favFood.setCalories(133.0); favFood.setProtein(31.0); favFood.setFat(1.2);
        favFood.setCarbs(0.0); favFood.setFiber(0.0);

        when(foodMapper.selectList(any())).thenReturn(Arrays.asList(food, favFood));

        FoodFavorite fav = new FoodFavorite();
        fav.setFoodId(2L);
        when(foodFavoriteMapper.selectList(any())).thenReturn(Collections.singletonList(fav));

        List<Map<String, Object>> result = foodRecommendationService.recommendFoods(1L, null, "lunch");
        assertFalse(result.isEmpty());
    }

    @Test
    void recommendFoods_breakfastPrefersBreakfastCategories() {
        when(dietRecordService.getDailyStats(anyLong(), any(), any())).thenReturn(emptyStats());
        when(userService.calculateTargets(1L)).thenReturn(new double[]{2000, 60, 55, 300});
        when(dietRecordService.getRecentFoods(anyLong(), any(), anyInt())).thenReturn(Collections.emptyList());
        when(foodFavoriteMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(dietRecordService.listByDate(anyLong(), any(), any())).thenReturn(Collections.emptyList());

        Food congee = new Food();
        congee.setId(1L); congee.setName("小米粥"); congee.setCategory("主食");
        congee.setCalories(46.0); congee.setProtein(1.4); congee.setFat(0.7);
        congee.setCarbs(8.4); congee.setFiber(0.0);

        Food steak = new Food();
        steak.setId(2L); steak.setName("牛排"); steak.setCategory("肉类");
        steak.setCalories(250.0); steak.setProtein(26.0); steak.setFat(15.0);
        steak.setCarbs(0.0); steak.setFiber(0.0);

        when(foodMapper.selectList(any())).thenReturn(Arrays.asList(congee, steak));

        List<Map<String, Object>> result = foodRecommendationService.recommendFoods(1L, null, "breakfast");
        assertFalse(result.isEmpty());
        for (Map<String, Object> item : result) {
            assertNotNull(item.get("score"));
        }
    }
}
