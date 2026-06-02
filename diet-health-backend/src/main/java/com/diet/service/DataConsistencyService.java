package com.diet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diet.entity.DietRecord;
import com.diet.entity.DietRecordDetail;
import com.diet.entity.Food;
import com.diet.mapper.DietRecordDetailMapper;
import com.diet.mapper.DietRecordMapper;
import com.diet.mapper.FoodMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据一致性校验服务
 * 定时检查 diet_record.total_calories 是否与 detail 计算值一致
 * 防止直接操作数据库导致的数据不一致
 */
@Slf4j
@Service
public class DataConsistencyService {

    private static final int BATCH_SIZE = 500;

    private final DietRecordMapper dietRecordMapper;
    private final DietRecordDetailMapper detailMapper;
    private final FoodMapper foodMapper;

    public DataConsistencyService(DietRecordMapper dietRecordMapper,
                                   DietRecordDetailMapper detailMapper,
                                   FoodMapper foodMapper) {
        this.dietRecordMapper = dietRecordMapper;
        this.detailMapper = detailMapper;
        this.foodMapper = foodMapper;
    }

    /**
     * 每天凌晨 3 点校验一次 total_calories 一致性
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void verifyTotalCalories() {
        log.info("[数据校验] 开始校验 diet_record.total_calories 一致性...");
        int totalChecked = 0;
        int fixedCount = 0;
        long page = 1;

        while (true) {
            Page<DietRecord> pageParam = new Page<>(page, BATCH_SIZE);
            Page<DietRecord> result = dietRecordMapper.selectPage(pageParam, null);
            List<DietRecord> records = result.getRecords();
            if (records.isEmpty()) {
                break;
            }

            List<Long> recordIds = records.stream().map(DietRecord::getId).collect(Collectors.toList());
            LambdaQueryWrapper<DietRecordDetail> dw = new LambdaQueryWrapper<>();
            dw.in(DietRecordDetail::getRecordId, recordIds);
            List<DietRecordDetail> allDetails = detailMapper.selectList(dw);

            List<Long> foodIds = allDetails.stream()
                    .map(DietRecordDetail::getFoodId).distinct().collect(Collectors.toList());
            Map<Long, Food> foodMap = foodIds.isEmpty() ? Map.of() :
                    foodMapper.selectBatchIds(foodIds).stream()
                            .collect(Collectors.toMap(Food::getId, f -> f, (a, b) -> a));

            Map<Long, List<DietRecordDetail>> detailsByRecord = allDetails.stream()
                    .collect(Collectors.groupingBy(DietRecordDetail::getRecordId));

            for (DietRecord record : records) {
                List<DietRecordDetail> details = detailsByRecord.getOrDefault(record.getId(), List.of());
                double calculated = 0;
                for (DietRecordDetail d : details) {
                    Food food = foodMap.get(d.getFoodId());
                    if (food != null) {
                        calculated += food.getCalories() * d.getAmount() / 100.0;
                    }
                }
                calculated = Math.round(calculated * 10) / 10.0;
                double stored = record.getTotalCalories() != null ? record.getTotalCalories() : 0;
                if (Math.abs(calculated - stored) > 0.1) {
                    log.warn("[数据校验] recordId={}, stored={}, calculated={}, 差异={}",
                            record.getId(), stored, calculated, Math.abs(calculated - stored));
                    record.setTotalCalories(calculated);
                    dietRecordMapper.updateById(record);
                    fixedCount++;
                }
            }
            totalChecked += records.size();

            if (!result.hasNext()) {
                break;
            }
            page++;
        }
        log.info("[数据校验] 完成，共检查 {} 条记录，修正 {} 条", totalChecked, fixedCount);
    }
}
