package com.diet.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.*;

import java.time.LocalDateTime;

@Data
@TableName("food")
public class Food {

    @TableId(type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "食物名称不能为空")
    private String name;
    @NotBlank(message = "分类不能为空")
    private String category;
    @NotNull(message = "热量不能为空")
    @DecimalMin(value = "0", message = "热量不能为负数")
    private Double calories;
    @DecimalMin(value = "0", message = "蛋白质不能为负数")
    private Double protein;
    @DecimalMin(value = "0", message = "脂肪不能为负数")
    private Double fat;
    @DecimalMin(value = "0", message = "碳水化合物不能为负数")
    private Double carbs;
    @DecimalMin(value = "0", message = "膳食纤维不能为负数")
    private Double fiber;
    private String imageUrl;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
