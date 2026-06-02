package com.diet.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
public class BodySymptomRequest {

    @NotNull(message = "记录日期不能为空")
    private LocalDate recordDate;

    @NotBlank(message = "症状类型不能为空")
    private String symptomType;

    @NotNull(message = "严重程度不能为空")
    @Min(value = 1, message = "严重程度最小为1")
    @Max(value = 10, message = "严重程度最大为10")
    private Integer severity;

    private String description;
    private String possibleCause;
    private Long memberId;
}
