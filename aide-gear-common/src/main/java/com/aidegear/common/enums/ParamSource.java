package com.aidegear.common.enums;

/**
 * 参数来源枚举 - 参数来源隔离机制的核心定义。
 * <p>
 * 通过定义参数的来源类型，确保 AI 仅能填充业务参数，
 * 而身份信息（如 UserID）由系统强制从安全上下文中提取。
 * </p>
 *
 * @author wayne
 * @since 1.0.0
 */
public enum ParamSource {

    /**
     * 对话内容提取 - AI 从对话上下文中识别并填充。
     * <p>该类型参数对 AI 可见，会出现在 ToolSpecification 的参数定义中。</p>
     */
    CONVERSATION,

    /**
     * JWT 鉴权令牌解析 - 框架从 Authorization Header 中自动解析。
     * <p>该类型参数对 AI 不可见，在暴露给 AI 的能力列表中自动隐藏。</p>
     */
    JWT,

    /**
     * 系统环境注入 - 从 ThreadLocal 或系统上下文中提取。
     * <p>该类型参数对 AI 不可见，用于租户ID、请求链路ID等系统级信息。</p>
     */
    SYSTEM
}
