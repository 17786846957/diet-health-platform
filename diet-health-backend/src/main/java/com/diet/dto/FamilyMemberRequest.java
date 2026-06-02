package com.diet.dto;

import lombok.Data;

import javax.validation.constraints.*;

@Data
public class FamilyMemberRequest {

    private Long id;

    @NotBlank(message = "成员姓名不能为空")
    @Size(min = 1, max = 50, message = "姓名长度1-50个字符")
    private String name;

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
    private String avatar;
}
