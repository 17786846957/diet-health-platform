package com.diet.common;

import java.lang.annotation.*;

/**
 * 操作审计日志注解
 * 标注在 Controller 方法上，自动记录操作人、操作类型、目标资源
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {
    /** 操作描述 */
    String value() default "";
    /** 资源类型 */
    String resource() default "";
}
