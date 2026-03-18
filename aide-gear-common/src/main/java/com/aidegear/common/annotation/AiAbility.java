package com.aidegear.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注在类/接口上，声明该 Bean 是一个 AI 可感知的"能力分组"。
 * <p>
 * 框架在启动时自动扫描所有标记了 {@code @AiAbility} 的 Bean，
 * 提取其内部标记了 {@code @AiAction} 的方法作为可调度技能。
 * </p>
 *
 * <pre>{@code
 * @Service
 * @AiAbility(name = "保单服务", description = "提供保单查询与管理能力")
 * public class PolicyService {
 *     // ...
 * }
 * }</pre>
 *
 * @author wayne
 * @since 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AiAbility {

    /**
     * 能力分组名称（面向 AI 的语义描述）
     */
    String name();

    /**
     * 能力分组描述
     */
    String description() default "";
}
