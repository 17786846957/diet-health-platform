package com.diet.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;

@Data
public class DietRecordDetailRequest {

    @NotNull(message = "食物ID不能为空")
    private Long foodId;

    @NotNull(message = "食用量不能为空")
    @DecimalMin(value = "0.1", message = "食用量必须大于0")
    private Double amount;
}
