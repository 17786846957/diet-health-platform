package com.diet.controller;

import com.diet.common.R;
import com.diet.dto.WeightRecordRequest;
import com.diet.entity.WeightRecord;
import com.diet.service.WeightRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@Tag(name = "体重记录管理", description = "体重记录及趋势分析")
@RestController
@RequestMapping("/weight")
public class WeightRecordController {

    private final WeightRecordService weightRecordService;

    public WeightRecordController(WeightRecordService weightRecordService) {
        this.weightRecordService = weightRecordService;
    }

    @Operation(summary = "添加/更新体重记录")
    @PostMapping
    public R<?> addOrUpdateRecord(@Valid @RequestBody WeightRecordRequest request, Authentication auth) {
        WeightRecord record = new WeightRecord();
        record.setUserId((Long) auth.getPrincipal());
        record.setRecordDate(request.getRecordDate());
        record.setWeight(request.getWeight());
        record.setBodyFat(request.getBodyFat());
        record.setNotes(request.getNotes());
        record.setMemberId(request.getMemberId());
        weightRecordService.addOrUpdateRecord(record);
        return R.ok("保存成功", null);
    }

    @Operation(summary = "删除体重记录")
    @DeleteMapping("/{id}")
    public R<?> deleteRecord(@PathVariable Long id, Authentication auth) {
        weightRecordService.deleteRecord(id, (Long) auth.getPrincipal());
        return R.ok("删除成功", null);
    }

    @Operation(summary = "获取最新体重")
    @GetMapping("/latest")
    public R<WeightRecord> getLatest(
            @RequestParam(required = false) Long memberId,
            Authentication auth) {
        return R.ok(weightRecordService.getLatest((Long) auth.getPrincipal(), memberId));
    }

    @Operation(summary = "体重趋势")
    @GetMapping("/trend")
    public R<Map<String, Object>> getWeightTrend(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(required = false) Long memberId,
            Authentication auth) {
        return R.ok(weightRecordService.getWeightTrend((Long) auth.getPrincipal(), memberId, days));
    }
}
