package com.diet.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.List;

@Data
public class DietRecordRequest {

    private Long id;

    @NotNull(message = "记录日期不能为空")
    private LocalDate recordDate;

    @NotBlank(message = "餐次类型不能为空")
    @Pattern(regexp = "^(breakfast|lunch|dinner|snack)$", message = "餐次类型无效")
    private String mealType;

    @NotEmpty(message = "饮食明细不能为空")
    @Valid
    private List<DietRecordDetailRequest> details;

    private Long memberId;
}
