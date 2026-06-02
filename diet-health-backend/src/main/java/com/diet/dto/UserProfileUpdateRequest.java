package com.diet.dto;

import lombok.Data;

import javax.validation.constraints.*;

@Data
public class UserProfileUpdateRequest {

    @Email(message = "邮箱格式不正确")
    private String email;

    @Pattern(regexp = "^(male|female)$", message = "性别只能为male或female")
    private String gender;

    @Min(value = 1, message = "年龄最小为1")
    @Max(value = 150, message = "年龄最大为150")
    private Integer age;

    @DecimalMin(value = "50", message = "身高最小50cm")
    @DecimalMax(value = "250", message = "身高最大250cm")
    private Double height;

    @DecimalMin(value = "20", message = "体重最小20kg")
    @DecimalMax(value = "300", message = "体重最大300kg")
    private Double weight;

    @Pattern(regexp = "^(sedentary|light|moderate|active|very_active)$", message = "活动水平不合法")
    private String activityLevel;

    @Pattern(regexp = "^(lose|maintain|gain)$", message = "目标不合法")
    private String goal;
}
