package com.diet.util;

/**
 * 营养计算公共工具类
 * 提取自 DietRecordService / DietAdviceService 中重复的工具方法
 */
public final class NutritionUtil {

    private NutritionUtil() {}

    /**
     * 四舍五入保留一位小数
     */
    public static double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }

    /**
     * 安全地将 Object 转为 double，null 或非法值返回 0.0
     */
    public static double safeDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return 0.0;
        }
    }
}
