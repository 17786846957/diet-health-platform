package com.diet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diet.entity.*;
import com.diet.mapper.*;
import com.diet.util.NutritionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 食物推荐服务
 * 从 DietRecordService 中拆出，职责单一：基于营养缺口 + 历史偏好 + 收藏推荐食物
 */
@Slf4j
@Service
public class FoodRecommendationService {

    private final FoodMapper foodMapper;
    private final FoodFavoriteMapper foodFavoriteMapper;
    private final DietRecordMapper dietRecordMapper;
    private final DietRecordDetailMapper detailMapper;
    private final UserService userService;
    private final DietRecordService dietRecordService;

    public FoodRecommendationService(FoodMapper foodMapper,
                                     FoodFavoriteMapper foodFavoriteMapper,
                                     DietRecordMapper dietRecordMapper,
                                     DietRecordDetailMapper detailMapper,
                                     UserService userService,
                                     DietRecordService dietRecordService) {
        this.foodMapper = foodMapper;
        this.foodFavoriteMapper = foodFavoriteMapper;
        this.dietRecordMapper = dietRecordMapper;
        this.detailMapper = detailMapper;
        this.userService = userService;
        this.dietRecordService = dietRecordService;
    }

    /**
     * 饮食推荐：基于营养缺口 + 历史偏好 + 收藏，推荐下一餐食物
     */
    public List<Map<String, Object>> recommendFoods(Long userId, Long memberId, String mealType) {
        log.info("饮食推荐: userId={}, memberId={}, mealType={}", userId, memberId, mealType);

        // 1. 获取今日营养缺口
        LocalDate today = LocalDate.now();
        Map<String, Object> dailyStats = dietRecordService.getDailyStats(userId, memberId, today);
        double actualCalories = NutritionUtil.safeDouble(dailyStats.get("totalCalories"));
        double actualProtein = NutritionUtil.safeDouble(dailyStats.get("totalProtein"));
        double actualFat = NutritionUtil.safeDouble(dailyStats.get("totalFat"));
        double actualCarbs = NutritionUtil.safeDouble(dailyStats.get("totalCarbs"));

        double[] targets = (memberId != null)
                ? userService.calculateTargetsForMember(memberId)
                : userService.calculateTargets(userId);

        int remainingMeals = getRemainingMeals(mealType);
        double needCalories = Math.max(0, (targets[0] - actualCalories) / remainingMeals);
        double needProtein = Math.max(0, (targets[1] - actualProtein) / remainingMeals);
        double needFat = Math.max(0, (targets[2] - actualFat) / remainingMeals);
        double needCarbs = Math.max(0, (targets[3] - actualCarbs) / remainingMeals);

        // 2. 获取用户近30天食物偏好
        Map<Long, Integer> historyCount = new HashMap<>();
        try {
            List<Map<String, Object>> recent = dietRecordService.getRecentFoods(userId, memberId, 30);
            for (Map<String, Object> item : recent) {
                Long foodId = ((Number) item.get("foodId")).longValue();
                int count = ((Number) item.get("count")).intValue();
                historyCount.put(foodId, count);
            }
        } catch (Exception e) {
            log.warn("获取用户近30天食物偏好失败，将使用无历史偏好的推荐: userId={}", userId, e);
        }

        // 3. 获取收藏食物集合
        Set<Long> favoriteIds = new HashSet<>();
        LambdaQueryWrapper<FoodFavorite> fw = new LambdaQueryWrapper<>();
        fw.eq(FoodFavorite::getUserId, userId);
        List<FoodFavorite> favorites = foodFavoriteMapper.selectList(fw);
        for (FoodFavorite fav : favorites) {
            favoriteIds.add(fav.getFoodId());
        }

        // 4. 获取今日已吃食物（用于去重）
        List<DietRecord> todayRecords = dietRecordService.listByDate(userId, memberId, today);
        Set<Long> eatenToday = new HashSet<>();
        for (DietRecord record : todayRecords) {
            if (record.getDetails() != null) {
                for (DietRecordDetail d : record.getDetails()) {
                    eatenToday.add(d.getFoodId());
                }
            }
        }

        // 5. SQL 预筛选
        String maxGapType = getMaxGapType(needProtein, needFat, needCarbs);
        LambdaQueryWrapper<Food> foodWrapper = new LambdaQueryWrapper<>();
        switch (maxGapType) {
            case "protein": foodWrapper.ge(Food::getProtein, 10); break;
            case "fat": foodWrapper.ge(Food::getFat, 5); break;
            case "carbs": foodWrapper.ge(Food::getCarbs, 15); break;
            default: break;
        }
        List<Food> allFoods = foodMapper.selectList(foodWrapper);
        if (allFoods.size() < 10) {
            allFoods = foodMapper.selectList(null);
        }
        int maxHistoryCount = historyCount.values().stream().mapToInt(Integer::intValue).max().orElse(1);

        // 6. 对每种食物评分
        List<Map<String, Object>> scored = new ArrayList<>();
        for (Food food : allFoods) {
            if (eatenToday.contains(food.getId()) && allFoods.size() > 15) {
                continue;
            }

            double nutritionScore = calculateNutritionScore(
                    food, needCalories, needProtein, needFat, needCarbs);
            double historyScore = historyCount.containsKey(food.getId())
                    ? (double) historyCount.get(food.getId()) / maxHistoryCount : 0;
            double favoriteScore = favoriteIds.contains(food.getId()) ? 1.0 : 0;
            double diversityScore = calculateDiversityScore(food, todayRecords);
            double mealFitScore = calculateMealFitScore(food, mealType);
            double healthScore = calculateHealthScore(food);

            double totalScore = nutritionScore * 0.30
                    + mealFitScore * 0.25
                    + healthScore * 0.20
                    + historyScore * 0.15
                    + favoriteScore * 0.05
                    + diversityScore * 0.05;

            String reason = buildRecommendReason(food, nutritionScore, historyScore, favoriteScore, mealFitScore, healthScore);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("foodId", food.getId());
            item.put("foodName", food.getName());
            item.put("category", food.getCategory());
            item.put("calories", food.getCalories());
            item.put("protein", food.getProtein());
            item.put("fat", food.getFat());
            item.put("carbs", food.getCarbs());
            item.put("score", Math.round(totalScore * 100) / 100.0);
            item.put("reason", reason);
            scored.add(item);
        }

        // 7. 按分数降序，返回前10
        scored.sort((a, b) -> Double.compare(
                ((Number) b.get("score")).doubleValue(),
                ((Number) a.get("score")).doubleValue()));
        List<Map<String, Object>> result = scored.size() > 10 ? scored.subList(0, 10) : scored;
        log.info("推荐完成: userId={}, mealType={}, resultCount={}", userId, mealType, result.size());
        return result;
    }

    private double calculateNutritionScore(Food food,
            double needCal, double needProtein, double needFat, double needCarbs) {
        if (needCal <= 0 && needProtein <= 0) {
            return 0.5;
        }
        double totalNeed = Math.max(needCal, 1) + Math.max(needProtein, 1)
                + Math.max(needFat, 1) + Math.max(needCarbs, 1);
        double calWeight = needCal / totalNeed;
        double proWeight = needProtein / totalNeed;
        double fatWeight = needFat / totalNeed;
        double carbWeight = needCarbs / totalNeed;

        double calFit = Math.min(food.getCalories() / Math.max(needCal, 1), 1.5);
        double proFit = Math.min(food.getProtein() / Math.max(needProtein, 1), 1.5);
        double fatFit = Math.min(food.getFat() / Math.max(needFat, 1), 1.5);
        double carbFit = Math.min(food.getCarbs() / Math.max(needCarbs, 1), 1.5);

        double score = calFit * calWeight + proFit * proWeight
                + fatFit * fatWeight + carbFit * carbWeight;
        return Math.min(score, 1.0);
    }

    private double calculateDiversityScore(Food food, List<DietRecord> todayRecords) {
        Set<String> eatenCategories = new HashSet<>();
        Set<Long> eatenFoodIds = new HashSet<>();
        for (DietRecord record : todayRecords) {
            if (record.getDetails() != null) {
                for (DietRecordDetail d : record.getDetails()) {
                    eatenFoodIds.add(d.getFoodId());
                }
            }
        }

        if (!eatenFoodIds.isEmpty()) {
            List<Food> eatenFoods = foodMapper.selectBatchIds(eatenFoodIds);
            for (Food f : eatenFoods) {
                if (f.getCategory() != null) {
                    eatenCategories.add(f.getCategory());
                }
            }
        }

        if (eatenCategories.isEmpty()) {
            return 1.0;
        }
        if (!eatenCategories.contains(food.getCategory())) {
            return 1.0;
        }

        double diversityRatio = 1.0 - (eatenCategories.size() / 10.0);
        return Math.max(0.3, diversityRatio);
    }

    private String getMaxGapType(double needProtein, double needFat, double needCarbs) {
        if (needProtein >= needFat && needProtein >= needCarbs) return "protein";
        if (needFat >= needProtein && needFat >= needCarbs) return "fat";
        return "carbs";
    }

    private int getRemainingMeals(String mealType) {
        switch (mealType) {
            case "breakfast": return 4;
            case "lunch": return 3;
            case "dinner": return 2;
            case "snack": return 1;
            default: return 3;
        }
    }

    private double calculateMealFitScore(Food food, String mealType) {
        String cat = food.getCategory();
        if (cat == null) return 0.5;

        switch (mealType) {
            case "breakfast":
                if ("主食".equals(cat)) return 1.0;
                if ("蛋奶".equals(cat)) return 0.95;
                if ("水果".equals(cat)) return 0.85;
                if ("饮品".equals(cat)) return 0.7;
                if ("家常菜".equals(cat)) return 0.5;
                if ("蔬菜".equals(cat)) return 0.4;
                if ("肉类".equals(cat)) return 0.3;
                if ("零食".equals(cat)) return 0.1;
                if ("调味".equals(cat) || "调味品".equals(cat)) return 0.0;
                return 0.3;
            case "lunch":
            case "dinner":
                if ("家常菜".equals(cat)) return 1.0;
                if ("主食".equals(cat)) return 0.9;
                if ("肉类".equals(cat)) return 0.85;
                if ("蔬菜".equals(cat)) return 0.85;
                if ("蛋奶".equals(cat)) return 0.6;
                if ("水果".equals(cat)) return 0.4;
                if ("饮品".equals(cat)) return 0.3;
                if ("零食".equals(cat)) return 0.1;
                if ("调味".equals(cat) || "调味品".equals(cat)) return 0.0;
                return 0.4;
            case "snack":
                if ("水果".equals(cat)) return 1.0;
                if ("蛋奶".equals(cat)) return 0.85;
                if ("饮品".equals(cat)) return 0.7;
                if ("蔬菜".equals(cat)) return 0.5;
                if ("零食".equals(cat)) return 0.4;
                if ("主食".equals(cat)) return 0.3;
                if ("肉类".equals(cat)) return 0.2;
                if ("家常菜".equals(cat)) return 0.3;
                if ("调味".equals(cat) || "调味品".equals(cat)) return 0.0;
                return 0.3;
            default:
                return 0.5;
        }
    }

    private double calculateHealthScore(Food food) {
        double cal = food.getCalories() != null ? food.getCalories() : 0;
        double protein = food.getProtein() != null ? food.getProtein() : 0;
        double fat = food.getFat() != null ? food.getFat() : 0;
        double carbs = food.getCarbs() != null ? food.getCarbs() : 0;
        double fiber = food.getFiber() != null ? food.getFiber() : 0;

        double proteinDensity = cal > 0 ? (protein * 4) / cal : 0;
        double proteinScore = Math.min(proteinDensity * 2, 1.0);

        double fiberScore = Math.min(fiber / 5.0, 1.0);

        double sugarRatio = cal > 0 ? (carbs * 4) / cal : 0;
        double junkPenalty = 0;
        if (protein < 1 && sugarRatio > 0.8) {
            junkPenalty = 0.8;
        } else if (protein < 2 && fat > 30) {
            junkPenalty = 0.5;
        } else if (cal > 400 && protein < 5) {
            junkPenalty = 0.4;
        }

        double score = proteinScore * 0.5 + fiberScore * 0.3 + 0.2 - junkPenalty;
        return Math.max(0, Math.min(1.0, score));
    }

    private String buildRecommendReason(Food food,
            double nutritionScore, double historyScore, double favoriteScore,
            double mealFitScore, double healthScore) {
        StringBuilder reason = new StringBuilder();

        if (healthScore > 0.6 && food.getProtein() > 10) {
            reason.append("高蛋白");
            if (food.getFiber() != null && food.getFiber() > 3) {
                reason.append("、富含膳食纤维");
            }
        } else if (food.getFiber() != null && food.getFiber() > 3) {
            reason.append("富含膳食纤维");
        } else if (healthScore > 0.5) {
            reason.append("营养均衡");
        }

        if (favoriteScore > 0) {
            reason.append(reason.length() > 0 ? "，" : "");
            reason.append("你的收藏");
        }
        if (historyScore > 0.5) {
            reason.append(reason.length() > 0 ? "，" : "");
            reason.append("常吃的食物");
        }

        if (reason.length() == 0) {
            if (nutritionScore > 0.5) {
                reason.append("适合补充当前营养缺口");
            } else {
                reason.append("均衡饮食搭配");
            }
        }
        return reason.toString();
    }
}
