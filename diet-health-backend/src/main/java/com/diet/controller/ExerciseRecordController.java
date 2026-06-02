package com.diet.controller;

import com.diet.common.R;
import com.diet.dto.ExerciseRecordRequest;
import com.diet.entity.ExerciseRecord;
import com.diet.service.ExerciseRecordService;
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

@Tag(name = "运动记录管理", description = "运动记录的增删改查及统计")
@RestController
@RequestMapping("/exercise")
public class ExerciseRecordController {

    private final ExerciseRecordService exerciseRecordService;

    public ExerciseRecordController(ExerciseRecordService exerciseRecordService) {
        this.exerciseRecordService = exerciseRecordService;
    }

    @Operation(summary = "添加运动记录")
    @PostMapping
    public R<?> addRecord(@Valid @RequestBody ExerciseRecordRequest request, Authentication auth) {
        ExerciseRecord record = new ExerciseRecord();
        record.setUserId((Long) auth.getPrincipal());
        record.setRecordDate(request.getRecordDate());
        record.setExerciseType(request.getExerciseType());
        record.setDuration(request.getDuration());
        record.setCaloriesBurned(request.getCaloriesBurned());
        record.setIntensity(request.getIntensity());
        record.setNotes(request.getNotes());
        record.setMemberId(request.getMemberId());
        exerciseRecordService.addRecord(record);
        return R.ok("添加成功", null);
    }

    @Operation(summary = "更新运动记录")
    @PutMapping
    public R<?> updateRecord(@Valid @RequestBody ExerciseRecordRequest request, Authentication auth) {
        ExerciseRecord record = new ExerciseRecord();
        record.setId(request.getId());
        record.setRecordDate(request.getRecordDate());
        record.setExerciseType(request.getExerciseType());
        record.setDuration(request.getDuration());
        record.setCaloriesBurned(request.getCaloriesBurned());
        record.setIntensity(request.getIntensity());
        record.setNotes(request.getNotes());
        record.setMemberId(request.getMemberId());
        exerciseRecordService.updateRecord(record, (Long) auth.getPrincipal());
        return R.ok("更新成功", null);
    }

    @Operation(summary = "删除运动记录")
    @DeleteMapping("/{id}")
    public R<?> deleteRecord(@PathVariable Long id, Authentication auth) {
        exerciseRecordService.deleteRecord(id, (Long) auth.getPrincipal());
        return R.ok("删除成功", null);
    }

    @Operation(summary = "查询某日运动记录")
    @GetMapping("/list")
    public R<List<ExerciseRecord>> listByDate(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam(required = false) Long memberId,
            Authentication auth) {
        return R.ok(exerciseRecordService.listByDate((Long) auth.getPrincipal(), memberId, date));
    }

    @Operation(summary = "每日运动统计")
    @GetMapping("/daily-stats")
    public R<Map<String, Object>> getDailyStats(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam(required = false) Long memberId,
            Authentication auth) {
        return R.ok(exerciseRecordService.getDailyStats((Long) auth.getPrincipal(), memberId, date));
    }

    @Operation(summary = "每周运动趋势")
    @GetMapping("/weekly-stats")
    public R<Map<String, Object>> getWeeklyStats(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate start,
            @RequestParam(required = false) Long memberId,
            Authentication auth) {
        LocalDate startDate = start != null ? start : LocalDate.now().minusDays(6);
        return R.ok(exerciseRecordService.getWeeklyStats((Long) auth.getPrincipal(), memberId, startDate));
    }
}
