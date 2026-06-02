package com.diet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diet.common.BusinessException;
import com.diet.common.ResultCode;
import com.diet.entity.WeightRecord;
import com.diet.mapper.WeightRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeightRecordService {

    private final WeightRecordMapper weightRecordMapper;

    @Transactional(rollbackFor = Exception.class)
    public void addOrUpdateRecord(WeightRecord record) {
        log.info("添加/更新体重记录: userId={}, date={}, weight={}", record.getUserId(), record.getRecordDate(), record.getWeight());
        LambdaQueryWrapper<WeightRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WeightRecord::getUserId, record.getUserId())
               .eq(WeightRecord::getRecordDate, record.getRecordDate());
        if (record.getMemberId() != null) {
            wrapper.eq(WeightRecord::getMemberId, record.getMemberId());
        } else {
            wrapper.isNull(WeightRecord::getMemberId);
        }

        WeightRecord existing = weightRecordMapper.selectOne(wrapper);
        if (existing != null) {
            existing.setWeight(record.getWeight());
            existing.setBodyFat(record.getBodyFat());
            existing.setNotes(record.getNotes());
            weightRecordMapper.updateById(existing);
        } else {
            weightRecordMapper.insert(record);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteRecord(Long id, Long userId) {
        WeightRecord record = weightRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ResultCode.WEIGHT_RECORD_NOT_FOUND);
        }
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.WEIGHT_RECORD_FORBIDDEN);
        }
        weightRecordMapper.deleteById(id);
    }

    public WeightRecord getLatest(Long userId, Long memberId) {
        LambdaQueryWrapper<WeightRecord> wrapper = buildWrapper(userId, memberId);
        wrapper.orderByDesc(WeightRecord::getRecordDate)
               .last("LIMIT 1");
        return weightRecordMapper.selectOne(wrapper);
    }

    public Map<String, Object> getWeightTrend(Long userId, Long memberId, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        LambdaQueryWrapper<WeightRecord> wrapper = buildWrapper(userId, memberId);
        wrapper.ge(WeightRecord::getRecordDate, startDate)
               .le(WeightRecord::getRecordDate, endDate)
               .orderByAsc(WeightRecord::getRecordDate);
        List<WeightRecord> records = weightRecordMapper.selectList(wrapper);

        List<String> dates = new ArrayList<>();
        List<BigDecimal> weights = new ArrayList<>();
        List<BigDecimal> bodyFats = new ArrayList<>();

        for (WeightRecord record : records) {
            dates.add(record.getRecordDate().toString());
            weights.add(record.getWeight());
            bodyFats.add(record.getBodyFat() != null ? record.getBodyFat() : BigDecimal.ZERO);
        }

        // 计算趋势
        String trend = "stable";
        BigDecimal change = BigDecimal.ZERO;
        if (records.size() >= 2) {
            BigDecimal first = records.get(0).getWeight();
            BigDecimal last = records.get(records.size() - 1).getWeight();
            change = last.subtract(first).setScale(1, RoundingMode.HALF_UP);
            if (change.compareTo(BigDecimal.ONE) > 0) {
                trend = "increasing";
            } else if (change.compareTo(BigDecimal.ONE.negate()) < 0) {
                trend = "decreasing";
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dates", dates);
        result.put("weights", weights);
        result.put("bodyFats", bodyFats);
        result.put("trend", trend);
        result.put("change", change);
        result.put("recordCount", records.size());
        return result;
    }

    private LambdaQueryWrapper<WeightRecord> buildWrapper(Long userId, Long memberId) {
        LambdaQueryWrapper<WeightRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WeightRecord::getUserId, userId);
        if (memberId == null) {
            wrapper.isNull(WeightRecord::getMemberId);
        } else {
            wrapper.eq(WeightRecord::getMemberId, memberId);
        }
        return wrapper;
    }
}