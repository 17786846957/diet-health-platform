package com.diet.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class WaterRecordRequest {

    @NotNull(message = "记录日期不能为空")
    private LocalDate recordDate;

    @NotNull(message = "饮水量不能为空")
    @DecimalMin(value = "1", message = "饮水量必须大于0")
    private BigDecimal amount;

    private String drinkType;
    private LocalTime recordTime;
    private Long memberId;
}
