package com.aidegear.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注在方法上，声明该方法为 AI 可调度的"动作/技能"。
 * <p>
 * 只有在标记了 {@code @AiAbility} 的类中的方法才会被扫描。
 * 框架会将方法签名转化为 LangChain4j 的 ToolSpecification，
 * 供 AI 模型在对话中自动发现和调用。
 * </p>
 *
 * <pre>{@code
 * @AiAction(name = "查询用户保单", desc = "获取指定用户的所有有效保单列表")
 * public List<Policy> getUserPolicies(
 *     @AiParam(value = "用户ID", source = ParamSource.JWT) Long userId,
 *     @AiParam("保单类型") String type
 * ) {
 *     // ...
 * }
 * }</pre>
 *
 * @author wayne
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AiAction {

    /**
     * 动作名称（面向 AI 的语义描述）
     */
    String name();

    /**
     * 动作的详细描述，帮助 AI 理解该动作的用途和使用场景
     */
    String desc() default "";

    /**
     * 返回值描述，帮助 AI 理解调用结果
     */
    String returnDesc() default "";
}
