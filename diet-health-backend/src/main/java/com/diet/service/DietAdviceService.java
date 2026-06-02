package com.diet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diet.entity.*;
import com.diet.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.diet.util.NutritionUtil;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DietAdviceService {

    private final DietRecordService dietRecordService;
    private final FoodRecommendationService foodRecommendationService;
    private final UserService userService;
    private final FoodMapper foodMapper;
    private final DietRecordMapper dietRecordMapper;
    private final DietRecordDetailMapper detailMapper;
    private final HealthGoalMapper healthGoalMapper;

    /**
     * 获取每日饮食建议
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDailyAdvice(Long userId, Long memberId) {
        LocalDate today = LocalDate.now();
        Map<String, Object> result = new LinkedHashMap<>();

        // 1. 获取今日营养状况
        Map<String, Object> dailyStats = dietRecordService.getDailyStats(userId, memberId, today);
        double actualCalories = NutritionUtil.safeDouble(dailyStats.get("totalCalories"));
        double actualProtein = NutritionUtil.safeDouble(dailyStats.get("totalProtein"));
        double actualFat = NutritionUtil.safeDouble(dailyStats.get("totalFat"));
        double actualCarbs = NutritionUtil.safeDouble(dailyStats.get("totalCarbs"));

        // 2. 获取目标值
        double[] targets = (memberId != null)
                ? userService.calculateTargetsForMember(memberId)
                : userService.calculateTargets(userId);
        double targetCalories = targets[0];
        double targetProtein = targets[1];
        double targetFat = targets[2];
        double targetCarbs = targets[3];

        // 3. 计算缺口
        double gapCalories = targetCalories - actualCalories;
        double gapProtein = targetProtein - actualProtein;
        double gapFat = targetFat - actualFat;
        double gapCarbs = targetCarbs - actualCarbs;

        // 4. 生成建议
        List<String> adviceList = new ArrayList<>();
        String status;

        if (actualCalories == 0) {
            status = "未开始";
            adviceList.add("今天还没有记录饮食，建议尽快补充早餐记录。");
            adviceList.add("早餐是一天中最重要的一餐，不要跳过哦！");
        } else if (gapCalories <= 0) {
            status = "已达标";
            adviceList.add("今日热量摄入已达标，注意控制后续饮食。");
            if (gapProtein < -10) {
                adviceList.add("蛋白质摄入偏多，晚餐可以多吃蔬菜水果。");
            }
        } else if (gapCalories < targetCalories * 0.3) {
            status = "接近达标";
            adviceList.add("今日热量摄入接近目标，剩余可摄入约" + Math.round(gapCalories) + "千卡。");
        } else {
            status = "未达标";
            adviceList.add("今日热量摄入不足，还需补充约" + Math.round(gapCalories) + "千卡。");
        }

        // 营养素建议
        if (gapProtein > 20) {
            adviceList.add("蛋白质摄入不足，建议食用鸡胸肉、鸡蛋、豆腐等高蛋白食物。");
        }
        if (gapFat > 20) {
            adviceList.add("脂肪摄入不足，适量食用坚果、橄榄油等健康脂肪。");
        }
        if (gapCarbs > 50) {
            adviceList.add("碳水化合物摄入不足，建议食用全谷物、薯类等。");
        }

        // 5. 推荐食物
        List<Map<String, Object>> recommendedFoods = foodRecommendationService.recommendFoods(userId, memberId, getNextMealType());

        result.put("date", today);
        result.put("status", status);
        result.put("advice", adviceList);
        result.put("nutritionGap", buildNutritionGap(actualCalories, targetCalories, actualProtein, targetProtein,
                actualFat, targetFat, actualCarbs, targetCarbs));
        result.put("recommendedFoods", recommendedFoods);

        return result;
    }

    /**
     * 获取饮食分析报告
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDietAnalysis(Long userId, Long memberId, int days) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 1. 获取历史数据
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        List<DietRecord> records = dietRecordService.listByDateRange(userId, memberId, startDate, endDate);

        // 2. 统计分析
        double totalCalories = 0;
        double totalProtein = 0;
        double totalFat = 0;
        double totalCarbs = 0;
        Map<String, Integer> mealTypeCount = new HashMap<>();
        Set<Long> uniqueFoodIds = new HashSet<>();

        for (DietRecord record : records) {
            totalCalories += NutritionUtil.safeDouble(record.getTotalCalories());
            mealTypeCount.merge(record.getMealType(), 1, Integer::sum);

            if (record.getDetails() != null) {
                for (DietRecordDetail detail : record.getDetails()) {
                    uniqueFoodIds.add(detail.getFoodId());
                }
            }
        }

        // 获取详情计算营养素
        if (!records.isEmpty()) {
            List<Long> recordIds = new ArrayList<>();
            for (DietRecord r : records) recordIds.add(r.getId());
            LambdaQueryWrapper<DietRecordDetail> dw = new LambdaQueryWrapper<>();
            dw.in(DietRecordDetail::getRecordId, recordIds);
            List<DietRecordDetail> details = detailMapper.selectList(dw);

            Map<Long, Food> foodMap = new HashMap<>();
            if (!uniqueFoodIds.isEmpty()) {
                List<Food> foods = foodMapper.selectBatchIds(new ArrayList<>(uniqueFoodIds));
                for (Food f : foods) foodMap.put(f.getId(), f);
            }

            for (DietRecordDetail detail : details) {
                Food food = foodMap.get(detail.getFoodId());
                if (food != null) {
                    double ratio = detail.getAmount() / 100.0;
                    totalProtein += food.getProtein() * ratio;
                    totalFat += food.getFat() * ratio;
                    totalCarbs += food.getCarbs() * ratio;
                }
            }
        }

        int recordDays = records.isEmpty() ? 1 : (int) records.stream()
                .map(DietRecord::getRecordDate).distinct().count();

        // 3. 生成分析
        List<String> analysisList = new ArrayList<>();

        double avgCalories = totalCalories / recordDays;
        if (avgCalories < 1500) {
            analysisList.add("近" + days + "天平均热量摄入偏低（" + Math.round(avgCalories) + "千卡），可能存在营养不良风险。");
        } else if (avgCalories > 2500) {
            analysisList.add("近" + days + "天平均热量摄入偏高（" + Math.round(avgCalories) + "千卡），注意控制饮食。");
        } else {
            analysisList.add("近" + days + "天平均热量摄入正常（" + Math.round(avgCalories) + "千卡），继续保持。");
        }

        // 饮食规律性分析
        if (mealTypeCount.containsKey("breakfast") && mealTypeCount.get("breakfast") < days / 2) {
            analysisList.add("早餐记录较少，建议养成每天吃早餐的习惯。");
        }
        if (!mealTypeCount.containsKey("breakfast")) {
            analysisList.add("没有早餐记录，早餐对健康非常重要，不要跳过。");
        }

        // 食物多样性分析
        if (uniqueFoodIds.size() < days * 2) {
            analysisList.add("食物种类较少，建议增加食物多样性，保证营养均衡。");
        } else {
            analysisList.add("食物多样性良好，继续保持。");
        }

        // 4. 构建结果
        result.put("period", startDate + " ~ " + endDate);
        result.put("recordDays", recordDays);
        result.put("totalRecords", records.size());
        result.put("uniqueFoods", uniqueFoodIds.size());
        result.put("analysis", analysisList);
        result.put("avgNutrition", buildAvgNutrition(avgCalories, totalProtein / recordDays,
                totalFat / recordDays, totalCarbs / recordDays));
        result.put("mealDistribution", mealTypeCount);

        return result;
    }

    /**
     * 获取健康建议（基于用户目标）
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getHealthAdvice(Long userId, Long memberId) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 获取用户信息
        User user = userService.getById(userId);
        if (user == null) {
            result.put("advice", Collections.singletonList("用户信息不存在"));
            return result;
        }

        List<String> adviceList = new ArrayList<>();

        // 优先从健康目标表获取活跃目标，其次从用户表获取
        String goal = null;
        LambdaQueryWrapper<HealthGoal> hgWrapper = new LambdaQueryWrapper<>();
        hgWrapper.eq(HealthGoal::getUserId, userId)
                 .eq(HealthGoal::getStatus, "active")
                 .orderByDesc(HealthGoal::getCreateTime)
                 .last("LIMIT 1");
        HealthGoal activeGoal = healthGoalMapper.selectOne(hgWrapper);
        if (activeGoal != null && activeGoal.getGoalType() != null) {
            goal = activeGoal.getGoalType();
        } else {
            goal = user.getGoal();
        }

        // 基于目标的建议
        if (goal != null) {
            switch (goal) {
                case "lose_weight":
                    adviceList.add("您的目标是减脂，建议：");
                    adviceList.add("1. 控制每日热量摄入，保持热量缺口");
                    adviceList.add("2. 增加蛋白质摄入，有助于保持肌肉量");
                    adviceList.add("3. 减少精制碳水和糖分摄入");
                    adviceList.add("4. 多吃蔬菜，增加膳食纤维摄入");
                    break;
                case "gain_weight":
                    adviceList.add("您的目标是增重，建议：");
                    adviceList.add("1. 适当增加每日热量摄入");
                    adviceList.add("2. 增加优质蛋白质摄入");
                    adviceList.add("3. 少食多餐，增加进餐频率");
                    adviceList.add("4. 配合适量力量训练");
                    break;
                case "muscle":
                    adviceList.add("您的目标是增肌，建议：");
                    adviceList.add("1. 每公斤体重摄入1.6-2.2克蛋白质");
                    adviceList.add("2. 训练后30分钟内补充蛋白质");
                    adviceList.add("3. 保持适量碳水化合物摄入");
                    adviceList.add("4. 保证充足睡眠和休息");
                    break;
                case "health":
                default:
                    adviceList.add("您的目标是保持健康，建议：");
                    adviceList.add("1. 保持饮食多样化");
                    adviceList.add("2. 每天摄入足够的蔬菜水果");
                    adviceList.add("3. 控制盐、油、糖的摄入");
                    adviceList.add("4. 规律进餐，不暴饮暴食");
                    break;
            }
        } else {
            adviceList.add("建议设置个人健康目标，以便获取更精准的饮食建议。");
            adviceList.add("您可以在个人中心设置减脂、增重、增肌或保持健康等目标。");
        }

        // 通用建议
        adviceList.add("\n通用健康建议：");
        adviceList.add("- 每天饮水1500-2000毫升");
        adviceList.add("- 每周至少运动3次，每次30分钟以上");
        adviceList.add("- 保证7-8小时充足睡眠");
        adviceList.add("- 减少外卖和加工食品摄入");

        result.put("goal", goal != null ? goal : "未设置");
        result.put("advice", adviceList);

        return result;
    }

    private Map<String, Object> buildNutritionGap(double actualCal, double targetCal,
                                                   double actualPro, double targetPro,
                                                   double actualFat, double targetFat,
                                                   double actualCarbs, double targetCarbs) {
        Map<String, Object> gap = new LinkedHashMap<>();
        gap.put("calories", Map.of("actual", NutritionUtil.round1(actualCal), "target", NutritionUtil.round1(targetCal), "gap", NutritionUtil.round1(targetCal - actualCal)));
        gap.put("protein", Map.of("actual", NutritionUtil.round1(actualPro), "target", NutritionUtil.round1(targetPro), "gap", NutritionUtil.round1(targetPro - actualPro)));
        gap.put("fat", Map.of("actual", NutritionUtil.round1(actualFat), "target", NutritionUtil.round1(targetFat), "gap", NutritionUtil.round1(targetFat - actualFat)));
        gap.put("carbs", Map.of("actual", NutritionUtil.round1(actualCarbs), "target", NutritionUtil.round1(targetCarbs), "gap", NutritionUtil.round1(targetCarbs - actualCarbs)));
        return gap;
    }

    private Map<String, Object> buildAvgNutrition(double calories, double protein, double fat, double carbs) {
        Map<String, Object> avg = new LinkedHashMap<>();
        avg.put("calories", NutritionUtil.round1(calories));
        avg.put("protein", NutritionUtil.round1(protein));
        avg.put("fat", NutritionUtil.round1(fat));
        avg.put("carbs", NutritionUtil.round1(carbs));
        return avg;
    }

    private String getNextMealType() {
        int hour = java.time.LocalTime.now().getHour();
        if (hour < 10) return "breakfast";
        if (hour < 14) return "lunch";
        if (hour < 17) return "snack";
        return "dinner";
    }
}