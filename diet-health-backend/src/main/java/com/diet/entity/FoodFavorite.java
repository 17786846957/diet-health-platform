package com.diet.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("food_favorite")
public class FoodFavorite {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long foodId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String foodName;

    @TableField(exist = false)
    private String category;

    @TableField(exist = false)
    private Double calories;

    @TableField(exist = false)
    private Double protein;

    @TableField(exist = false)
    private Double fat;

    @TableField(exist = false)
    private Double carbs;

    @TableField(exist = false)
    private Double fiber;
}
