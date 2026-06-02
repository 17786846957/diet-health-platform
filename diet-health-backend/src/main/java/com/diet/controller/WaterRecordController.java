package com.diet.controller;

import com.diet.common.R;
import com.diet.dto.WaterRecordRequest;
import com.diet.entity.WaterRecord;
import com.diet.service.WaterRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "饮水记录管理", description = "饮水记录的增删改查及统计")
@RestController
@RequestMapping("/water")
public class WaterRecordController {

    private final WaterRecordService waterRecordService;

    public WaterRecordController(WaterRecordService waterRecordService) {
        this.waterRecordService = waterRecordService;
    }

    @Operation(summary = "添加饮水记录")
    @PostMapping
    public R<?> addRecord(@Valid @RequestBody WaterRecordRequest request, Authentication auth) {
        WaterRecord record = new WaterRecord();
        record.setUserId((Long) auth.getPrincipal());
        record.setRecordDate(request.getRecordDate());
        record.setAmount(request.getAmount());
        record.setDrinkType(request.getDrinkType());
        record.setRecordTime(request.getRecordTime());
        record.setMemberId(request.getMemberId());
        waterRecordService.addRecord(record);
        return R.ok("添加成功", null);
    }

    @Operation(summary = "删除饮水记录")
    @DeleteMapping("/{id}")
    public R<?> deleteRecord(@PathVariable Long id, Authentication auth) {
        waterRecordService.deleteRecord(id, (Long) auth.getPrincipal());
        return R.ok("删除成功", null);
    }

    @Operation(summary = "查询某日饮水记录")
    @GetMapping("/list")
    public R<List<WaterRecord>> listByDate(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam(required = false) Long memberId,
            Authentication auth) {
        return R.ok(waterRecordService.listByDate((Long) auth.getPrincipal(), memberId, date));
    }

    @Operation(summary = "每日饮水统计")
    @GetMapping("/daily-stats")
    public R<Map<String, Object>> getDailyStats(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam(required = false) Long memberId,
            Authentication auth) {
        return R.ok(waterRecordService.getDailyStats((Long) auth.getPrincipal(), memberId, date));
    }

    @Operation(summary = "每周饮水趋势")
    @GetMapping("/weekly-stats")
    public R<Map<String, Object>> getWeeklyStats(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate start,
            @RequestParam(required = false) Long memberId,
            Authentication auth) {
        LocalDate startDate = start != null ? start : LocalDate.now().minusDays(6);
        return R.ok(waterRecordService.getWeeklyStats((Long) auth.getPrincipal(), memberId, startDate));
    }
}
