package com.diet.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;

@Data
public class HealthGoalRequest {

    private Long id;

    @NotBlank(message = "目标类型不能为空")
    private String goalType;

    private BigDecimal targetWeight;
    private BigDecimal targetCalories;
    private BigDecimal targetProtein;
    private BigDecimal targetFat;
    private BigDecimal targetCarbs;
    private BigDecimal targetWater;
    private Long memberId;
}
