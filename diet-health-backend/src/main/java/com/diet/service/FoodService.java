package com.diet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diet.entity.Food;
import com.diet.mapper.FoodMapper;
import com.diet.common.BusinessException;
import com.diet.common.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class FoodService {

    private final FoodMapper foodMapper;

    public FoodService(FoodMapper foodMapper) {
        this.foodMapper = foodMapper;
    }

    public Page<Food> listFoods(int page, int size, String category, String keyword) {
        LambdaQueryWrapper<Food> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(category)) {
            wrapper.eq(Food::getCategory, category);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Food::getName, keyword);
        }
        wrapper.orderByDesc(Food::getCreateTime);
        return foodMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public Food getById(Long id) {
        return foodMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void addFood(Food food) {
        foodMapper.insert(food);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateFood(Food food) {
        if (foodMapper.selectById(food.getId()) == null) {
            throw new BusinessException(ResultCode.FOOD_NOT_FOUND);
        }
        foodMapper.updateById(food);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteFood(Long id) {
        if (foodMapper.selectById(id) == null) {
            throw new BusinessException(ResultCode.FOOD_NOT_FOUND);
        }
        foodMapper.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeFoodImage(Long id) {
        Food food = foodMapper.selectById(id);
        if (food == null) {
            throw new BusinessException(ResultCode.FOOD_NOT_FOUND);
        }
        food.setImageUrl(null);
        foodMapper.updateById(food);
    }

    public long count() {
        return foodMapper.selectCount(null);
    }
}
