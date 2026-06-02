package com.diet.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("health_goal")
public class HealthGoal {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long memberId;
    private String goalType;
    private BigDecimal targetWeight;
    private BigDecimal targetCalories;
    private BigDecimal targetProtein;
    private BigDecimal targetFat;
    private BigDecimal targetCarbs;
    private BigDecimal targetWater;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private BigDecimal progress;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}