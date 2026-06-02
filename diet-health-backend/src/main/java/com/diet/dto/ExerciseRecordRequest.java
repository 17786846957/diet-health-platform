package com.diet.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ExerciseRecordRequest {

    private Long id;

    @NotNull(message = "记录日期不能为空")
    private LocalDate recordDate;

    @NotBlank(message = "运动类型不能为空")
    private String exerciseType;

    @NotNull(message = "运动时长不能为空")
    @DecimalMin(value = "1", message = "运动时长必须大于0")
    private Integer duration;

    private BigDecimal caloriesBurned;
    private String intensity;
    private String notes;
    private Long memberId;
}
