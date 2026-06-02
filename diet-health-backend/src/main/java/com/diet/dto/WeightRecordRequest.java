package com.diet.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class WeightRecordRequest {

    @NotNull(message = "记录日期不能为空")
    private LocalDate recordDate;

    @NotNull(message = "体重不能为空")
    @DecimalMin(value = "1", message = "体重必须大于0")
    private BigDecimal weight;

    private BigDecimal bodyFat;
    private String notes;
    private Long memberId;
}
