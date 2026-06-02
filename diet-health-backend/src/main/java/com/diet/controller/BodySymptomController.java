package com.diet.controller;

import com.diet.common.R;
import com.diet.dto.BodySymptomRequest;
import com.diet.entity.BodySymptom;
import com.diet.service.BodySymptomService;
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

@Tag(name = "身体症状管理", description = "身体症状记录及分析")
@RestController
@RequestMapping("/symptom")
public class BodySymptomController {

    private final BodySymptomService bodySymptomService;

    public BodySymptomController(BodySymptomService bodySymptomService) {
        this.bodySymptomService = bodySymptomService;
    }

    @Operation(summary = "添加症状记录")
    @PostMapping
    public R<?> addRecord(@Valid @RequestBody BodySymptomRequest request, Authentication auth) {
        BodySymptom record = new BodySymptom();
        record.setUserId((Long) auth.getPrincipal());
        record.setRecordDate(request.getRecordDate());
        record.setSymptomType(request.getSymptomType());
        record.setSeverity(request.getSeverity());
        record.setDescription(request.getDescription());
        record.setPossibleCause(request.getPossibleCause());
        record.setMemberId(request.getMemberId());
        bodySymptomService.addRecord(record);
        return R.ok("添加成功", null);
    }

    @Operation(summary = "删除症状记录")
    @DeleteMapping("/{id}")
    public R<?> deleteRecord(@PathVariable Long id, Authentication auth) {
        bodySymptomService.deleteRecord(id, (Long) auth.getPrincipal());
        return R.ok("删除成功", null);
    }

    @Operation(summary = "查询症状记录")
    @GetMapping("/list")
    public R<List<BodySymptom>> listByDateRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end,
            @RequestParam(required = false) Long memberId,
            Authentication auth) {
        return R.ok(bodySymptomService.listByDateRange((Long) auth.getPrincipal(), memberId, start, end));
    }

    @Operation(summary = "症状分析报告")
    @GetMapping("/analysis")
    public R<Map<String, Object>> getSymptomAnalysis(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(required = false) Long memberId,
            Authentication auth) {
        return R.ok(bodySymptomService.getSymptomAnalysis((Long) auth.getPrincipal(), memberId, days));
    }
}
