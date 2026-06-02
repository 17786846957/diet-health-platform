package com.diet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diet.common.BusinessException;
import com.diet.common.ResultCode;
import com.diet.entity.Food;
import com.diet.entity.FoodFavorite;
import com.diet.mapper.FoodFavoriteMapper;
import com.diet.mapper.FoodMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class FavoriteService {

    private final FoodFavoriteMapper favoriteMapper;
    private final FoodMapper foodMapper;

    public FavoriteService(FoodFavoriteMapper favoriteMapper, FoodMapper foodMapper) {
        this.favoriteMapper = favoriteMapper;
        this.foodMapper = foodMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public void addFavorite(Long userId, Long foodId) {
        Food food = foodMapper.selectById(foodId);
        if (food == null) {
            throw new BusinessException(ResultCode.FOOD_NOT_FOUND);
        }

        LambdaQueryWrapper<FoodFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FoodFavorite::getUserId, userId)
               .eq(FoodFavorite::getFoodId, foodId);
        if (favoriteMapper.selectCount(wrapper) > 0) {
            return; // already favorited
        }

        FoodFavorite fav = new FoodFavorite();
        fav.setUserId(userId);
        fav.setFoodId(foodId);
        favoriteMapper.insert(fav);
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeFavorite(Long userId, Long foodId) {
        LambdaQueryWrapper<FoodFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FoodFavorite::getUserId, userId)
               .eq(FoodFavorite::getFoodId, foodId);
        favoriteMapper.delete(wrapper);
    }

    public List<FoodFavorite> getFavorites(Long userId) {
        LambdaQueryWrapper<FoodFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FoodFavorite::getUserId, userId)
               .orderByDesc(FoodFavorite::getCreateTime);
        List<FoodFavorite> favorites = favoriteMapper.selectList(wrapper);

        if (favorites.isEmpty()) {
            return favorites;
        }

        List<Long> foodIds = favorites.stream()
                .map(FoodFavorite::getFoodId)
                .collect(Collectors.toList());
        Map<Long, Food> foodMap = foodMapper.selectBatchIds(foodIds).stream()
                .collect(Collectors.toMap(Food::getId, f -> f, (a, b) -> a));

        for (FoodFavorite fav : favorites) {
            Food food = foodMap.get(fav.getFoodId());
            if (food != null) {
                fav.setFoodName(food.getName());
                fav.setCategory(food.getCategory());
                fav.setCalories(food.getCalories());
                fav.setProtein(food.getProtein());
                fav.setFat(food.getFat());
                fav.setCarbs(food.getCarbs());
                fav.setFiber(food.getFiber());
            }
        }
        return favorites;
    }

    public Set<Long> getFavoriteFoodIds(Long userId) {
        LambdaQueryWrapper<FoodFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FoodFavorite::getUserId, userId)
               .select(FoodFavorite::getFoodId);
        return favoriteMapper.selectList(wrapper).stream()
                .map(FoodFavorite::getFoodId)
                .collect(Collectors.toSet());
    }
}
