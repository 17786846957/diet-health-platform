package com.diet.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.*;

import java.time.LocalDateTime;

@Data
@TableName("`user`")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 20, message = "用户名长度2-20个字符")
    private String username;

    @Size(min = 6, max = 50, message = "密码长度6-50个字符")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

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
    private String activityLevel;
    private String goal;
    private String role;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
