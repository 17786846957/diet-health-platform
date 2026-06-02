package com.diet.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diet.common.BusinessException;
import com.diet.entity.Food;
import com.diet.mapper.FoodMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FoodServiceTest {

    @Mock
    private FoodMapper foodMapper;

    @InjectMocks
    private FoodService foodService;

    @Test
    void listFoods_returnsPage() {
        Page<Food> page = new Page<>(1, 20);
        when(foodMapper.selectPage(any(), any())).thenReturn(page);

        Page<Food> result = foodService.listFoods(1, 20, null, null);
        assertNotNull(result);
        verify(foodMapper).selectPage(any(), any());
    }

    @Test
    void listFoods_withKeyword_queriesByName() {
        Page<Food> page = new Page<>(1, 20);
        Food food = new Food();
        food.setName("鸡蛋");
        page.setRecords(Collections.singletonList(food));
        when(foodMapper.selectPage(any(), any())).thenReturn(page);

        Page<Food> result = foodService.listFoods(1, 20, null, "鸡蛋");

        assertNotNull(result);
        assertFalse(result.getRecords().isEmpty());
        verify(foodMapper).selectPage(any(), any());
    }

    @Test
    void listFoods_withCategory_filtersByCategory() {
        Page<Food> page = new Page<>(1, 20);
        when(foodMapper.selectPage(any(), any())).thenReturn(page);

        Page<Food> result = foodService.listFoods(1, 20, "水果", null);
        assertNotNull(result);
        verify(foodMapper).selectPage(any(), any());
    }

    @Test
    void getById_returnsFood() {
        Food food = new Food();
        food.setId(1L);
        food.setName("米饭");
        when(foodMapper.selectById(1L)).thenReturn(food);

        Food result = foodService.getById(1L);
        assertEquals("米饭", result.getName());
    }

    @Test
    void addFood_success() {
        when(foodMapper.insert(any())).thenReturn(1);
        assertDoesNotThrow(() -> foodService.addFood(new Food()));
    }

    @Test
    void updateFood_notFound_throws() {
        when(foodMapper.selectById(999L)).thenReturn(null);
        Food food = new Food();
        food.setId(999L);

        assertThrows(BusinessException.class, () -> foodService.updateFood(food));
    }

    @Test
    void updateFood_success() {
        Food existing = new Food();
        existing.setId(1L);
        when(foodMapper.selectById(1L)).thenReturn(existing);
        when(foodMapper.updateById(any())).thenReturn(1);

        Food update = new Food();
        update.setId(1L);
        update.setName("新名称");

        assertDoesNotThrow(() -> foodService.updateFood(update));
        verify(foodMapper).updateById(update);
    }

    @Test
    void deleteFood_notFound_throws() {
        when(foodMapper.selectById(999L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> foodService.deleteFood(999L));
    }

    @Test
    void deleteFood_success() {
        Food food = new Food();
        food.setId(1L);
        when(foodMapper.selectById(1L)).thenReturn(food);
        when(foodMapper.deleteById(1L)).thenReturn(1);

        assertDoesNotThrow(() -> foodService.deleteFood(1L));
        verify(foodMapper).deleteById(1L);
    }

    @Test
    void removeFoodImage_notFound_throws() {
        when(foodMapper.selectById(999L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> foodService.removeFoodImage(999L));
    }

    @Test
    void removeFoodImage_success() {
        Food food = new Food();
        food.setId(1L);
        food.setImageUrl("http://example.com/img.jpg");
        when(foodMapper.selectById(1L)).thenReturn(food);
        when(foodMapper.updateById(any())).thenReturn(1);

        assertDoesNotThrow(() -> foodService.removeFoodImage(1L));
        assertNull(food.getImageUrl());
        verify(foodMapper).updateById(food);
    }

    @Test
    void count_returnsValue() {
        when(foodMapper.selectCount(null)).thenReturn(5L);
        assertEquals(5L, foodService.count());
    }
}
