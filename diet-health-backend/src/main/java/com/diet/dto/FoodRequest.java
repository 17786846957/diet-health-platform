package com.diet.dto;

import lombok.Data;

import javax.validation.constraints.*;

@Data
public class FoodRequest {

    private Long id;

    @NotBlank(message = "食物名称不能为空")
    @Size(min = 1, max = 100, message = "食物名称长度1-100个字符")
    private String name;

    @Size(max = 50, message = "分类名称最长50个字符")
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

    @Size(max = 255, message = "图片URL最长255个字符")
    private String imageUrl;
}
