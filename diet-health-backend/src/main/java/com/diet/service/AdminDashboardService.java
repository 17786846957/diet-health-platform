package com.diet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diet.entity.DietRecord;
import com.diet.entity.Food;
import com.diet.entity.User;
import com.diet.mapper.DietRecordMapper;
import com.diet.mapper.FoodMapper;
import com.diet.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final UserMapper userMapper;
    private final FoodMapper foodMapper;
    private final DietRecordMapper dietRecordMapper;

    public List<Map<String, Object>> getUserRegistrationTrend(int days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1);
        LambdaQueryWrapper<User> w = new LambdaQueryWrapper<>();
        w.ge(User::getCreateTime, start.atStartOfDay())
         .lt(User::getCreateTime, end.plusDays(1).atStartOfDay())
         .select(User::getCreateTime);
        List<User> users = userMapper.selectList(w);

        Map<String, Long> countByDate = users.stream()
                .collect(Collectors.groupingBy(
                        u -> u.getCreateTime().toLocalDate().toString(),
                        Collectors.counting()));

        return buildTrendResult(start, end, countByDate);
    }

    public List<Map<String, Object>> getFoodCategoryDistribution() {
        List<Food> foods = foodMapper.selectList(null);
        Map<String, Long> countByCategory = foods.stream()
                .filter(f -> f.getCategory() != null)
                .collect(Collectors.groupingBy(Food::getCategory, Collectors.counting()));

        return countByCategory.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("category", e.getKey());
                    item.put("count", e.getValue());
                    return item;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getDailyRecordTrend(int days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1);
        LambdaQueryWrapper<DietRecord> w = new LambdaQueryWrapper<>();
        w.ge(DietRecord::getRecordDate, start)
         .le(DietRecord::getRecordDate, end)
         .select(DietRecord::getRecordDate);
        List<DietRecord> records = dietRecordMapper.selectList(w);

        Map<String, Long> countByDate = records.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getRecordDate().toString(),
                        Collectors.counting()));

        return buildTrendResult(start, end, countByDate);
    }

    private List<Map<String, Object>> buildTrendResult(LocalDate start, LocalDate end,
                                                        Map<String, Long> countByDate) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", d.toString());
            item.put("count", countByDate.getOrDefault(d.toString(), 0L));
            result.add(item);
        }
        return result;
    }
}
