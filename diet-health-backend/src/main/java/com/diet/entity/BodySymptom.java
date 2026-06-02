package com.diet.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("body_symptom")
public class BodySymptom {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long memberId;
    private LocalDate recordDate;
    private String symptomType;
    @Min(value = 1, message = "严重程度最小为1")
    @Max(value = 10, message = "严重程度最大为10")
    private Integer severity;
    private String description;
    private String possibleCause;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}