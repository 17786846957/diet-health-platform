package com.diet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;

@Data
@TableName("diet_record_detail")
public class DietRecordDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    @NotNull(message = "记录ID不能为空")
    private Long recordId;
    @NotNull(message = "食物ID不能为空")
    private Long foodId;

    @DecimalMin(value = "0.1", message = "食用量最小为0.1g")
    private Double amount;
    private Double calories;

    @TableField(exist = false)
    private String foodName;

    @TableField(exist = false)
    private Double protein;

    @TableField(exist = false)
    private Double fat;

    @TableField(exist = false)
    private Double carbs;
}
