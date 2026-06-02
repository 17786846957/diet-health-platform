package com.diet.service;

import com.diet.common.BusinessException;
import com.diet.entity.Food;
import com.diet.entity.FoodFavorite;
import com.diet.mapper.FoodFavoriteMapper;
import com.diet.mapper.FoodMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock private FoodFavoriteMapper favoriteMapper;
    @Mock private FoodMapper foodMapper;
    @InjectMocks private FavoriteService favoriteService;

    @Test
    void addFavorite_foodNotFound_throws() {
        when(foodMapper.selectById(999L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> favoriteService.addFavorite(1L, 999L));
    }

    @Test
    void addFavorite_alreadyFavorited_noInsert() {
        Food food = new Food();
        food.setId(1L);
        when(foodMapper.selectById(1L)).thenReturn(food);
        when(favoriteMapper.selectCount(any())).thenReturn(1L);

        favoriteService.addFavorite(1L, 1L);
        verify(favoriteMapper, never()).insert(any());
    }

    @Test
    void addFavorite_success() {
        Food food = new Food();
        food.setId(1L);
        when(foodMapper.selectById(1L)).thenReturn(food);
        when(favoriteMapper.selectCount(any())).thenReturn(0L);
        when(favoriteMapper.insert(any())).thenReturn(1);

        assertDoesNotThrow(() -> favoriteService.addFavorite(1L, 1L));
        verify(favoriteMapper).insert(any());
    }

    @Test
    void removeFavorite_success() {
        when(favoriteMapper.delete(any())).thenReturn(1);
        assertDoesNotThrow(() -> favoriteService.removeFavorite(1L, 1L));
    }

    @Test
    void getFavorites_empty() {
        when(favoriteMapper.selectList(any())).thenReturn(Collections.emptyList());
        List<FoodFavorite> result = favoriteService.getFavorites(1L);
        assertTrue(result.isEmpty());
    }

    @Test
    void getFavorites_withData_enrichesFields() {
        FoodFavorite fav = new FoodFavorite();
        fav.setFoodId(1L);
        when(favoriteMapper.selectList(any())).thenReturn(Collections.singletonList(fav));

        Food food = new Food();
        food.setId(1L);
        food.setName("苹果");
        food.setCategory("水果");
        food.setCalories(52.0);
        when(foodMapper.selectBatchIds(anyList())).thenReturn(Collections.singletonList(food));

        List<FoodFavorite> result = favoriteService.getFavorites(1L);
        assertEquals(1, result.size());
        assertEquals("苹果", result.get(0).getFoodName());
    }

    @Test
    void getFavoriteFoodIds_returnsSet() {
        FoodFavorite fav = new FoodFavorite();
        fav.setFoodId(1L);
        when(favoriteMapper.selectList(any())).thenReturn(Collections.singletonList(fav));

        Set<Long> ids = favoriteService.getFavoriteFoodIds(1L);
        assertTrue(ids.contains(1L));
    }
}
