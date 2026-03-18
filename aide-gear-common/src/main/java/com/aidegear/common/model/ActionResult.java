package com.aidegear.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 动作执行结果封装。
 *
 * @author wayne
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionResult {

    /**
     * 是否执行成功
     */
    private boolean success;

    /**
     * 执行结果数据（JSON 序列化后的字符串）
     */
    private String data;

    /**
     * 错误信息（失败时填充）
     */
    private String errorMessage;

    /**
     * 执行耗时（毫秒）
     */
    private long costMs;

    /**
     * 被调用的动作ID
     */
    private String actionId;

    public static ActionResult success(String actionId, String data, long costMs) {
        return ActionResult.builder()
                .success(true)
                .actionId(actionId)
                .data(data)
                .costMs(costMs)
                .build();
    }

    public static ActionResult fail(String actionId, String errorMessage, long costMs) {
        return ActionResult.builder()
                .success(false)
                .actionId(actionId)
                .errorMessage(errorMessage)
                .costMs(costMs)
                .build();
    }
}
