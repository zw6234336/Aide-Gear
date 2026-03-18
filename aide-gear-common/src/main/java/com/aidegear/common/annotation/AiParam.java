package com.aidegear.common.annotation;

import com.aidegear.common.enums.ParamSource;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注在方法参数上，描述参数含义及其来源。
 * <p>
 * 通过 {@code source} 属性实现<b>参数来源隔离</b>，是整个安全隔离机制的基石：
 * <ul>
 *     <li>{@code CONVERSATION}（默认）：由 AI 从对话上下文中提取，暴露给 AI 可见</li>
 *     <li>{@code JWT}：由框架从 Authorization Header 中解析，AI 不可见、不可篡改</li>
 *     <li>{@code SYSTEM}：由系统从 ThreadLocal 或环境变量中注入，AI 不可见</li>
 * </ul>
 * </p>
 *
 * <pre>{@code
 * @AiAction(name = "查询我的保单")
 * public List<Policy> getMyPolicies(
 *     @AiParam(value = "用户ID", source = ParamSource.JWT) Long userId,
 *     @AiParam(value = "保单类型", required = false) String type
 * ) { ... }
 * }</pre>
 *
 * @author wayne
 * @since 1.0.0
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AiParam {

    /**
     * 参数的语义描述（面向 AI 的说明）
     */
    String value();

    /**
     * 参数来源，默认为 CONVERSATION（AI 对话提取）
     */
    ParamSource source() default ParamSource.CONVERSATION;

    /**
     * 参数是否必填，默认 true
     */
    boolean required() default true;

    /**
     * JWT 来源时，对应的 JWT Claim Key
     * <p>例如：jwtKey = "userId"，则从 JWT payload 的 userId 字段提取</p>
     */
    String jwtKey() default "";

    /**
     * SYSTEM 来源时，对应的系统属性 Key
     */
    String systemKey() default "";

    /**
     * 参数示例值，帮助 AI 理解期望的输入格式
     */
    String example() default "";
}
