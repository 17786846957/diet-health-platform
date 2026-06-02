package com.diet.dto;

import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class FavoriteRequest {
    @NotNull(message = "foodId 不能为空")
    private Long foodId;
}
