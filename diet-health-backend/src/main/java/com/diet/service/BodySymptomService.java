package com.diet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diet.common.BusinessException;
import com.diet.common.ResultCode;
import com.diet.entity.BodySymptom;
import com.diet.mapper.BodySymptomMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BodySymptomService {

    private final BodySymptomMapper bodySymptomMapper;

    @Transactional(rollbackFor = Exception.class)
    public void addRecord(BodySymptom record) {
        log.info("添加身体症状记录: userId={}, type={}, severity={}", record.getUserId(), record.getSymptomType(), record.getSeverity());
        bodySymptomMapper.insert(record);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteRecord(Long id, Long userId) {
        BodySymptom record = bodySymptomMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "症状记录不存在");
        }
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        bodySymptomMapper.deleteById(id);
    }

    public List<BodySymptom> listByDateRange(Long userId, Long memberId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<BodySymptom> wrapper = buildWrapper(userId, memberId);
        wrapper.ge(BodySymptom::getRecordDate, startDate)
               .le(BodySymptom::getRecordDate, endDate)
               .orderByDesc(BodySymptom::getRecordDate);
        return bodySymptomMapper.selectList(wrapper);
    }

    public Map<String, Object> getSymptomAnalysis(Long userId, Long memberId, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        List<BodySymptom> records = listByDateRange(userId, memberId, startDate, endDate);

        // 按症状类型统计
        Map<String, Integer> typeCount = new LinkedHashMap<>();
        Map<String, List<Integer>> severityByType = new LinkedHashMap<>();
        for (BodySymptom record : records) {
            typeCount.merge(record.getSymptomType(), 1, Integer::sum);
            severityByType.computeIfAbsent(record.getSymptomType(), k -> new ArrayList<>()).add(record.getSeverity());
        }

        // 计算平均严重程度
        Map<String, Double> avgSeverity = new LinkedHashMap<>();
        severityByType.forEach((type, severities) -> {
            double avg = severities.stream().mapToInt(Integer::intValue).average().orElse(0);
            avgSeverity.put(type, Math.round(avg * 10) / 10.0);
        });

        // 生成分析建议
        List<String> analysis = new ArrayList<>();
        if (records.isEmpty()) {
            analysis.add("近" + days + "天没有症状记录，继续保持健康的生活方式！");
        } else {
            analysis.add("近" + days + "天共记录" + records.size() + "次症状。");
            typeCount.forEach((type, count) -> {
                analysis.add(getSymptomName(type) + "出现" + count + "次，平均严重程度" + avgSeverity.get(type) + "/5");
            });
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("period", startDate + " ~ " + endDate);
        result.put("totalRecords", records.size());
        result.put("typeCount", typeCount);
        result.put("avgSeverity", avgSeverity);
        result.put("analysis", analysis);
        result.put("records", records);
        return result;
    }

    private String getSymptomName(String type) {
        switch (type) {
            case "headache": return "头痛";
            case "fatigue": return "疲劳";
            case "nausea": return "恶心";
            case "bloating": return "腹胀";
            case "insomnia": return "失眠";
            case "constipation": return "便秘";
            case "diarrhea": return "腹泻";
            case "heartburn": return "胃灼热";
            default: return type;
        }
    }

    private LambdaQueryWrapper<BodySymptom> buildWrapper(Long userId, Long memberId) {
        LambdaQueryWrapper<BodySymptom> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BodySymptom::getUserId, userId);
        if (memberId == null) {
            wrapper.isNull(BodySymptom::getMemberId);
        } else {
            wrapper.eq(BodySymptom::getMemberId, memberId);
        }
        return wrapper;
    }
}