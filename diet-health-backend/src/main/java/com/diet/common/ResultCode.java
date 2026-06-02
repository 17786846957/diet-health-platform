package com.diet.common;

import lombok.Getter;

@Getter
public enum ResultCode {
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有操作权限"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    USER_EXISTS(1001, "用户名已存在"),
    RECORD_NOT_FOUND(1002, "饮食记录不存在"),
    RECORD_FORBIDDEN(1003, "无权操作该记录"),
    FOOD_NOT_FOUND(1004, "食物不存在"),
    MEMBER_NOT_FOUND(1005, "家庭成员不存在"),
    MEMBER_FORBIDDEN(1006, "无权操作该家庭成员"),
    WATER_RECORD_NOT_FOUND(1007, "饮水记录不存在"),
    WATER_RECORD_FORBIDDEN(1008, "无权操作该饮水记录"),
    EXERCISE_RECORD_NOT_FOUND(1009, "运动记录不存在"),
    EXERCISE_RECORD_FORBIDDEN(1010, "无权操作该运动记录"),
    WEIGHT_RECORD_NOT_FOUND(1011, "体重记录不存在"),
    WEIGHT_RECORD_FORBIDDEN(1012, "无权操作该体重记录"),
    GOAL_NOT_FOUND(1013, "健康目标不存在"),
    GOAL_FORBIDDEN(1014, "无权操作该健康目标");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
