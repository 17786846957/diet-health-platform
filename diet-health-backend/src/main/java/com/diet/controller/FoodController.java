package com.diet.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diet.common.R;
import com.diet.dto.FoodRequest;
import com.diet.entity.Food;
import com.diet.service.FoodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Tag(name = "食物库管理", description = "食物数据的查询与管理")
@RestController
@RequestMapping("/food")
public class FoodController {

    private final FoodService foodService;

    public FoodController(FoodService foodService) {
        this.foodService = foodService;
    }

    @Operation(summary = "食物列表", description = "分页查询食物，支持按分类和关键词筛选")
    @GetMapping("/list")
    public R<Page<Food>> listFoods(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") @javax.validation.constraints.Max(100) int size,
            @Parameter(description = "食物分类") @RequestParam(required = false) String category,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword) {
        return R.ok(foodService.listFoods(page, size, category, keyword));
    }

    @Operation(summary = "食物详情", description = "根据 ID 获取食物营养信息")
    @GetMapping("/{id}")
    public R<Food> getById(@PathVariable Long id) {
        Food food = foodService.getById(id);
        if (food == null) {
            throw new com.diet.common.BusinessException(com.diet.common.ResultCode.FOOD_NOT_FOUND);
        }
        return R.ok(food);
    }

    @Operation(summary = "添加食物", description = "管理员添加新食物（需管理员权限）")
    @PostMapping
    @PreAuthorize("hasRole('admin')")
    public R<?> addFood(@Valid @RequestBody FoodRequest request) {
        Food food = new Food();
        food.setName(request.getName());
        food.setCategory(request.getCategory());
        food.setCalories(request.getCalories());
        food.setProtein(request.getProtein() != null ? request.getProtein() : 0.0);
        food.setFat(request.getFat() != null ? request.getFat() : 0.0);
        food.setCarbs(request.getCarbs() != null ? request.getCarbs() : 0.0);
        food.setFiber(request.getFiber() != null ? request.getFiber() : 0.0);
        food.setImageUrl(request.getImageUrl());
        foodService.addFood(food);
        return R.ok("添加成功", null);
    }

    @Operation(summary = "更新食物", description = "管理员更新食物信息（需管理员权限）")
    @PutMapping
    @PreAuthorize("hasRole('admin')")
    public R<?> updateFood(@Valid @RequestBody FoodRequest request) {
        Food food = new Food();
        food.setId(request.getId());
        food.setName(request.getName());
        food.setCategory(request.getCategory());
        food.setCalories(request.getCalories());
        food.setProtein(request.getProtein() != null ? request.getProtein() : 0.0);
        food.setFat(request.getFat() != null ? request.getFat() : 0.0);
        food.setCarbs(request.getCarbs() != null ? request.getCarbs() : 0.0);
        food.setFiber(request.getFiber() != null ? request.getFiber() : 0.0);
        food.setImageUrl(request.getImageUrl());
        foodService.updateFood(food);
        return R.ok("更新成功", null);
    }

    @Operation(summary = "删除食物", description = "管理员删除食物（需管理员权限）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public R<?> deleteFood(@PathVariable Long id) {
        foodService.deleteFood(id);
        return R.ok("删除成功", null);
    }

    @Operation(summary = "删除食物图片", description = "管理员删除食物图片（需管理员权限）")
    @DeleteMapping("/{id}/image")
    @PreAuthorize("hasRole('admin')")
    public R<?> removeFoodImage(@PathVariable Long id) {
        foodService.removeFoodImage(id);
        return R.ok("图片已删除", null);
    }
}
