package com.diet.controller;

import com.diet.common.R;
import com.diet.dto.FavoriteRequest;
import com.diet.entity.FoodFavorite;
import com.diet.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

import java.util.List;
import java.util.Set;

@Tag(name = "食物收藏管理", description = "用户食物收藏的增删查")
@RestController
@RequestMapping("/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @Operation(summary = "收藏食物", description = "将食物加入收藏")
    @PostMapping
    public R<?> addFavorite(Authentication auth, @Valid @RequestBody FavoriteRequest body) {
        Long userId = (Long) auth.getPrincipal();
        favoriteService.addFavorite(userId, body.getFoodId());
        return R.ok("收藏成功", null);
    }

    @Operation(summary = "取消收藏", description = "将食物从收藏中移除")
    @DeleteMapping("/{foodId}")
    public R<?> removeFavorite(Authentication auth, @PathVariable Long foodId) {
        Long userId = (Long) auth.getPrincipal();
        favoriteService.removeFavorite(userId, foodId);
        return R.ok("取消收藏", null);
    }

    @Operation(summary = "收藏列表", description = "获取当前用户的所有收藏食物")
    @GetMapping
    public R<List<FoodFavorite>> getFavorites(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return R.ok(favoriteService.getFavorites(userId));
    }

    @Operation(summary = "收藏ID集合", description = "获取当前用户收藏的食物ID集合（用于标记已收藏状态）")
    @GetMapping("/ids")
    public R<Set<Long>> getFavoriteIds(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return R.ok(favoriteService.getFavoriteFoodIds(userId));
    }
}
