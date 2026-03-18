package com.aidegear.common.exception;

/**
 * 动作执行异常 - 在调用 AI 技能方法时发生的异常。
 *
 * @author wayne
 * @since 1.0.0
 */
public class ActionExecutionException extends AideGearException {

    private final String actionId;

    public ActionExecutionException(String actionId, String message) {
        super(String.format("动作[%s]执行失败: %s", actionId, message));
        this.actionId = actionId;
    }

    public ActionExecutionException(String actionId, String message, Throwable cause) {
        super(String.format("动作[%s]执行失败: %s", actionId, message), cause);
        this.actionId = actionId;
    }

    public String getActionId() {
        return actionId;
    }
}
