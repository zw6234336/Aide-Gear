package com.aidegear.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * LLM 解析后的 Action 调用请求 —— 描述「要调用哪个动作、用哪些参数」。
 * <p>
 * 在 AbstractAideGearChat 的 Action 选择阶段，框架将 LLM 返回的 JSON 串
 * 解析为 ActionCallRequest 列表，再逐一交给 ActionExecutor 执行。
 * </p>
 *
 * <pre>
 * 对应的 LLM 输出格式（JSON 数组）：
 * [
 *   {
 *     "actionId": "产品查询服务.查询产品详情",
 *     "arguments": {"riskCode": "RISK001"}
 *   }
 * ]
 * </pre>
 *
 * @author aide-gear
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionCallRequest {

    /**
     * 要调用的动作唯一标识（格式: abilityName.actionName）
     */
    private String actionId;

    /**
     * AI 从用户对话中提取的参数（仅 CONVERSATION 来源的参数）。
     * JWT / SYSTEM 来源的参数由 ActionExecutor 自动注入，此处不包含。
     */
    private Map<String, Object> arguments;
}
