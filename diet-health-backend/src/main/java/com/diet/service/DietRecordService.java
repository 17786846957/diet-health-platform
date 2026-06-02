package com.diet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diet.entity.DietRecord;
import com.diet.entity.DietRecordDetail;
import com.diet.entity.Food;
import com.diet.mapper.DietRecordDetailMapper;
import com.diet.mapper.DietRecordMapper;
import com.diet.common.BusinessException;
import com.diet.common.ResultCode;
import com.diet.mapper.FoodMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.diet.util.NutritionUtil;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DietRecordService {

    private final DietRecordMapper dietRecordMapper;
    private final DietRecordDetailMapper detailMapper;
    private final FoodMapper foodMapper;
    private final UserService userService;

    public DietRecordService(DietRecordMapper dietRecordMapper,
                             DietRecordDetailMapper detailMapper,
                             FoodMapper foodMapper,
                             UserService userService) {
        this.dietRecordMapper = dietRecordMapper;
        this.detailMapper = detailMapper;
        this.foodMapper = foodMapper;
        this.userService = userService;
    }

    public long count() {
        return dietRecordMapper.selectCount(null);
    }

    @Transactional(rollbackFor = Exception.class)
    public void addRecord(DietRecord record) {
        log.info("添加饮食记录: userId={}, date={}, mealType={}, memberId={}",
                record.getUserId(), record.getRecordDate(), record.getMealType(), record.getMemberId());
        double totalCalories = calculateDetailCalories(record.getDetails());
        record.setTotalCalories(NutritionUtil.round1(totalCalories));
        dietRecordMapper.insert(record);

        if (record.getDetails() != null && !record.getDetails().isEmpty()) {
            for (DietRecordDetail detail : record.getDetails()) {
                detail.setRecordId(record.getId());
            }
            detailMapper.batchInsert(record.getDetails());
        }
        log.info("饮食记录添加成功: recordId={}, totalCalories={}", record.getId(), record.getTotalCalories());
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateRecord(DietRecord record, Long userId) {
        log.info("更新饮食记录: recordId={}, userId={}", record.getId(), userId);
        getRecordAndCheckPermission(record.getId(), userId);

        double totalCalories = calculateDetailCalories(record.getDetails());

        LambdaQueryWrapper<DietRecordDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DietRecordDetail::getRecordId, record.getId());
        detailMapper.delete(wrapper);

        if (record.getDetails() != null && !record.getDetails().isEmpty()) {
            for (DietRecordDetail detail : record.getDetails()) {
                detail.setRecordId(record.getId());
            }
            detailMapper.batchInsert(record.getDetails());
        }

        record.setTotalCalories(NutritionUtil.round1(totalCalories));
        dietRecordMapper.updateById(record);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteRecord(Long id, Long userId) {
        log.info("删除饮食记录: recordId={}, userId={}", id, userId);
        getRecordAndCheckPermission(id, userId);
        LambdaQueryWrapper<DietRecordDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DietRecordDetail::getRecordId, id);
        detailMapper.delete(wrapper);
        dietRecordMapper.deleteById(id);
    }

    public List<DietRecord> listByDate(Long userId, Long memberId, LocalDate date) {
        LambdaQueryWrapper<DietRecord> wrapper = userMemberWrapper(userId, memberId);
        wrapper.eq(DietRecord::getRecordDate, date)
               .orderByAsc(DietRecord::getMealType);
        List<DietRecord> records = dietRecordMapper.selectList(wrapper);
        if (records.isEmpty()) {
            return records;
        }
        List<DietRecordDetail> allDetails = loadDetailsForRecords(records);
        enrichDetailsWithFoodInfo(records, allDetails);
        return records;
    }

    public List<DietRecord> listByDateRange(Long userId, Long memberId, LocalDate start, LocalDate end) {
        LambdaQueryWrapper<DietRecord> wrapper = userMemberWrapper(userId, memberId);
        wrapper.ge(DietRecord::getRecordDate, start)
               .le(DietRecord::getRecordDate, end)
               .orderByAsc(DietRecord::getRecordDate);
        return dietRecordMapper.selectList(wrapper);
    }

    public Map<String, Object> getDailyStats(Long userId, Long memberId, LocalDate date) {
        List<DietRecord> records = listByDate(userId, memberId, date);
        double totalCalories = 0;
        double totalProtein = 0;
        double totalFat = 0;
        double totalCarbs = 0;

        for (DietRecord record : records) {
            totalCalories += NutritionUtil.safeDouble(record.getTotalCalories());
            if (record.getDetails() != null) {
                for (DietRecordDetail detail : record.getDetails()) {
                    totalProtein += NutritionUtil.safeDouble(detail.getProtein());
                    totalFat += NutritionUtil.safeDouble(detail.getFat());
                    totalCarbs += NutritionUtil.safeDouble(detail.getCarbs());
                }
            }
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("date", date);
        stats.put("totalCalories", NutritionUtil.round1(totalCalories));
        stats.put("totalProtein", NutritionUtil.round1(totalProtein));
        stats.put("totalFat", NutritionUtil.round1(totalFat));
        stats.put("totalCarbs", NutritionUtil.round1(totalCarbs));
        stats.put("records", records);
        return stats;
    }

    public Map<String, Object> getWeeklyStats(Long userId, Long memberId, LocalDate start, LocalDate end) {
        LocalDate today = LocalDate.now();
        LocalDate effectiveEnd = (end != null) ? end : today;
        LocalDate effectiveStart = (start != null) ? start : effectiveEnd.minusDays(6);
        List<DietRecord> records = listByDateRange(userId, memberId, effectiveStart, effectiveEnd);

        long days = java.time.temporal.ChronoUnit.DAYS.between(effectiveStart, effectiveEnd) + 1;
        Map<LocalDate, Double> dailyCalories = new LinkedHashMap<>();
        for (int i = 0; i < days; i++) {
            dailyCalories.put(effectiveStart.plusDays(i), 0.0);
        }
        for (DietRecord record : records) {
            dailyCalories.merge(record.getRecordDate(), NutritionUtil.safeDouble(record.getTotalCalories()), Double::sum);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        List<String> dates = new ArrayList<>();
        List<Double> calories = new ArrayList<>();
        dailyCalories.forEach((date, cal) -> {
            dates.add(date.toString());
            calories.add(NutritionUtil.round1(cal));
        });
        result.put("dates", dates);
        result.put("calories", calories);
        return result;
    }

    public Map<String, Object> getMonthlyStats(Long userId, Long memberId, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        LambdaQueryWrapper<DietRecord> wrapper = userMemberWrapper(userId, memberId);
        wrapper.ge(DietRecord::getRecordDate, start)
               .le(DietRecord::getRecordDate, end)
               .orderByAsc(DietRecord::getRecordDate);
        List<DietRecord> records = dietRecordMapper.selectList(wrapper);

        Map<LocalDate, double[]> dailyMap = new LinkedHashMap<>();
        for (int i = 0; i < start.lengthOfMonth(); i++) {
            dailyMap.put(start.plusDays(i), new double[4]);
        }
        for (DietRecord record : records) {
            double[] arr = dailyMap.get(record.getRecordDate());
            if (arr != null) {
                arr[0] += NutritionUtil.safeDouble(record.getTotalCalories());
            }
        }

        List<DietRecordDetail> details = loadDetailsForRecords(records);
        if (!details.isEmpty()) {
            Map<Long, Food> foodMap = loadFoodMap(details);
            Map<Long, LocalDate> recordDateMap = records.stream()
                    .collect(Collectors.toMap(DietRecord::getId, DietRecord::getRecordDate));

            for (DietRecordDetail detail : details) {
                LocalDate date = recordDateMap.get(detail.getRecordId());
                double[] arr = dailyMap.get(date);
                if (arr != null) {
                    Food food = foodMap.get(detail.getFoodId());
                    if (food != null) {
                        double ratio = detail.getAmount() / 100.0;
                        arr[1] += food.getProtein() * ratio;
                        arr[2] += food.getFat() * ratio;
                        arr[3] += food.getCarbs() * ratio;
                    }
                }
            }
        }

        double avgCal = dailyMap.values().stream().mapToDouble(a -> a[0]).average().orElse(0);

        Map<String, Object> result = new LinkedHashMap<>();
        List<String> dates = new ArrayList<>();
        List<Double> calories = new ArrayList<>();
        List<Double> proteins = new ArrayList<>();
        List<Double> fats = new ArrayList<>();
        List<Double> carbsList = new ArrayList<>();

        dailyMap.forEach((date, arr) -> {
            dates.add(date.toString());
            calories.add(NutritionUtil.round1(arr[0]));
            proteins.add(NutritionUtil.round1(arr[1]));
            fats.add(NutritionUtil.round1(arr[2]));
            carbsList.add(NutritionUtil.round1(arr[3]));
        });

        result.put("dates", dates);
        result.put("calories", calories);
        result.put("proteins", proteins);
        result.put("fats", fats);
        result.put("carbs", carbsList);
        result.put("avgCalories", NutritionUtil.round1(avgCal));
        return result;
    }

    public Map<String, Object> getNutritionGap(Long userId, Long memberId, LocalDate date) {
        log.debug("营养缺口分析: userId={}, memberId={}, date={}", userId, memberId, date);
        Map<String, Object> dailyStats = getDailyStats(userId, memberId, date);
        double actualCalories = (double) dailyStats.get("totalCalories");
        double actualProtein = (double) dailyStats.get("totalProtein");
        double actualFat = (double) dailyStats.get("totalFat");
        double actualCarbs = (double) dailyStats.get("totalCarbs");

        double[] targets = (memberId != null)
                ? userService.calculateTargetsForMember(memberId)
                : userService.calculateTargets(userId);
        double targetCalories = targets[0];
        double targetProtein = targets[1];
        double targetFat = targets[2];
        double targetCarbs = targets[3];

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date", date);

        List<Map<String, Object>> gaps = new ArrayList<>();
        gaps.add(buildGap("热量", "kcal", actualCalories, targetCalories));
        gaps.add(buildGap("蛋白质", "g", actualProtein, targetProtein));
        gaps.add(buildGap("脂肪", "g", actualFat, targetFat));
        gaps.add(buildGap("碳水化合物", "g", actualCarbs, targetCarbs));

        result.put("gaps", gaps);
        return result;
    }

    private Map<String, Object> buildGap(String name, String unit,
                                          double actual, double target) {
        Map<String, Object> gap = new LinkedHashMap<>();
        gap.put("nutrient", name);
        gap.put("unit", unit);
        gap.put("actual", NutritionUtil.round1(actual));
        gap.put("target", NutritionUtil.round1(target));
        gap.put("gap", NutritionUtil.round1(target - actual));
        gap.put("percentage", target > 0 ? Math.round(actual / target * 100) : 0);
        return gap;
    }

    private double calculateDetailCalories(List<DietRecordDetail> details) {
        if (details == null || details.isEmpty()) {
            return 0;
        }
        Map<Long, Food> foodMap = loadFoodMap(details);

        double totalCalories = 0;
        for (DietRecordDetail detail : details) {
            Food food = foodMap.get(detail.getFoodId());
            if (food == null) {
                throw new BusinessException(ResultCode.FOOD_NOT_FOUND);
            }
            double cal = food.getCalories() * detail.getAmount() / 100.0;
            detail.setCalories(NutritionUtil.round1(cal));
            totalCalories += detail.getCalories();
        }
        return totalCalories;
    }

    private DietRecord getRecordAndCheckPermission(Long id, Long userId) {
        DietRecord existing = dietRecordMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.RECORD_NOT_FOUND);
        }
        if (!existing.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.RECORD_FORBIDDEN);
        }
        return existing;
    }

    private LambdaQueryWrapper<DietRecord> userMemberWrapper(Long userId, Long memberId) {
        LambdaQueryWrapper<DietRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DietRecord::getUserId, userId);
        if (memberId == null) {
            wrapper.isNull(DietRecord::getMemberId);
        } else {
            wrapper.eq(DietRecord::getMemberId, memberId);
        }
        return wrapper;
    }

    private List<DietRecordDetail> loadDetailsForRecords(List<DietRecord> records) {
        List<Long> recordIds = records.stream().map(DietRecord::getId).collect(Collectors.toList());
        if (recordIds.isEmpty()) return Collections.emptyList();
        LambdaQueryWrapper<DietRecordDetail> dw = new LambdaQueryWrapper<>();
        dw.in(DietRecordDetail::getRecordId, recordIds);
        return detailMapper.selectList(dw);
    }

    private Map<Long, Food> loadFoodMap(List<DietRecordDetail> details) {
        List<Long> foodIds = details.stream()
                .map(DietRecordDetail::getFoodId).distinct().collect(Collectors.toList());
        if (foodIds.isEmpty()) return Collections.emptyMap();
        return foodMapper.selectBatchIds(foodIds).stream()
                .collect(Collectors.toMap(Food::getId, f -> f, (a, b) -> a));
    }

    private void enrichDetailsWithFoodInfo(List<DietRecord> records, List<DietRecordDetail> allDetails) {
        Map<Long, Food> foodMap = loadFoodMap(allDetails);
        Map<Long, List<DietRecordDetail>> detailsByRecordId = allDetails.stream()
                .collect(Collectors.groupingBy(DietRecordDetail::getRecordId));
        for (DietRecord record : records) {
            List<DietRecordDetail> details = detailsByRecordId.getOrDefault(record.getId(), Collections.emptyList());
            for (DietRecordDetail detail : details) {
                Food food = foodMap.get(detail.getFoodId());
                if (food != null) {
                    detail.setFoodName(food.getName());
                    detail.setProtein(food.getProtein() * detail.getAmount() / 100.0);
                    detail.setFat(food.getFat() * detail.getAmount() / 100.0);
                    detail.setCarbs(food.getCarbs() * detail.getAmount() / 100.0);
                }
            }
            record.setDetails(details);
        }
    }

    public List<Map<String, Object>> getRecentFoods(Long userId, Long memberId, int days) {
        LocalDate start = LocalDate.now().minusDays(days);
        LambdaQueryWrapper<DietRecord> rw = userMemberWrapper(userId, memberId);
        rw.ge(DietRecord::getRecordDate, start);
        List<DietRecord> records = dietRecordMapper.selectList(rw);
        if (records.isEmpty()) {
            return Collections.emptyList();
        }

        List<DietRecordDetail> details = loadDetailsForRecords(records);
        if (details.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Food> foodMap = loadFoodMap(details);

        Map<Long, long[]> usageMap = new HashMap<>();
        for (DietRecordDetail detail : details) {
            usageMap.merge(detail.getFoodId(), new long[]{1, detail.getAmount().longValue()},
                    (a, b) -> new long[]{a[0] + 1, a[1]});
        }

        List<Map<String, Object>> result = new ArrayList<>();
        usageMap.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]))
                .limit(20)
                .forEach(entry -> {
                    Food food = foodMap.get(entry.getKey());
                    if (food != null) {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("foodId", food.getId());
                        item.put("foodName", food.getName());
                        item.put("category", food.getCategory());
                        item.put("calories", food.getCalories());
                        item.put("protein", food.getProtein());
                        item.put("fat", food.getFat());
                        item.put("carbs", food.getCarbs());
                        item.put("count", entry.getValue()[0]);
                        result.add(item);
                    }
                });
        return result;
    }

}
