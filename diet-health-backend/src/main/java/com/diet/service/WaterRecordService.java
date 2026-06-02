package com.diet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diet.common.BusinessException;
import com.diet.common.ResultCode;
import com.diet.entity.WaterRecord;
import com.diet.mapper.WaterRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WaterRecordService {

    private final WaterRecordMapper waterRecordMapper;

    @Transactional(rollbackFor = Exception.class)
    public void addRecord(WaterRecord record) {
        log.info("添加饮水记录: userId={}, date={}, amount={}ml", record.getUserId(), record.getRecordDate(), record.getAmount());
        waterRecordMapper.insert(record);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteRecord(Long id, Long userId) {
        WaterRecord record = waterRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ResultCode.WATER_RECORD_NOT_FOUND);
        }
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.WATER_RECORD_FORBIDDEN);
        }
        waterRecordMapper.deleteById(id);
    }

    public List<WaterRecord> listByDate(Long userId, Long memberId, LocalDate date) {
        LambdaQueryWrapper<WaterRecord> wrapper = buildWrapper(userId, memberId);
        wrapper.eq(WaterRecord::getRecordDate, date)
               .orderByDesc(WaterRecord::getCreateTime);
        return waterRecordMapper.selectList(wrapper);
    }

    public Map<String, Object> getDailyStats(Long userId, Long memberId, LocalDate date) {
        List<WaterRecord> records = listByDate(userId, memberId, date);
        BigDecimal totalAmount = records.stream()
                .map(WaterRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> byType = new LinkedHashMap<>();
        for (WaterRecord record : records) {
            String type = record.getDrinkType() != null ? record.getDrinkType() : "water";
            byType.merge(type, record.getAmount(), BigDecimal::add);
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("date", date);
        stats.put("totalAmount", totalAmount);
        stats.put("recordCount", records.size());
        stats.put("byType", byType);
        stats.put("records", records);
        return stats;
    }

    public Map<String, Object> getWeeklyStats(Long userId, Long memberId, LocalDate startDate) {
        LocalDate endDate = startDate.plusDays(6);
        LambdaQueryWrapper<WaterRecord> wrapper = buildWrapper(userId, memberId);
        wrapper.ge(WaterRecord::getRecordDate, startDate)
               .le(WaterRecord::getRecordDate, endDate);
        List<WaterRecord> records = waterRecordMapper.selectList(wrapper);

        Map<LocalDate, BigDecimal> dailyMap = new LinkedHashMap<>();
        for (int i = 0; i < 7; i++) {
            dailyMap.put(startDate.plusDays(i), BigDecimal.ZERO);
        }
        for (WaterRecord record : records) {
            dailyMap.merge(record.getRecordDate(), record.getAmount(), BigDecimal::add);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        List<String> dates = new ArrayList<>();
        List<BigDecimal> amounts = new ArrayList<>();
        dailyMap.forEach((date, amount) -> {
            dates.add(date.toString());
            amounts.add(amount);
        });
        result.put("dates", dates);
        result.put("amounts", amounts);
        return result;
    }

    private LambdaQueryWrapper<WaterRecord> buildWrapper(Long userId, Long memberId) {
        LambdaQueryWrapper<WaterRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WaterRecord::getUserId, userId);
        if (memberId == null) {
            wrapper.isNull(WaterRecord::getMemberId);
        } else {
            wrapper.eq(WaterRecord::getMemberId, memberId);
        }
        return wrapper;
    }
}