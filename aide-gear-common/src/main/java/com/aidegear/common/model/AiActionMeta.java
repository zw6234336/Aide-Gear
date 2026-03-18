package com.aidegear.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.Method;
import java.util.List;

/**
 * AI 动作元数据 - 描述一个可被 AI 调度的方法的完整信息。
 *
 * @author wayne
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiActionMeta {

    /**
     * 动作唯一标识（格式: abilityName.actionName）
     */
    private String actionId;

    /**
     * 动作名称（来自 @AiAction 的 name）
     */
    private String actionName;

    /**
     * 动作描述（来自 @AiAction 的 desc）
     */
    private String description;

    /**
     * 返回值描述
     */
    private String returnDesc;

    /**
     * 所属能力分组名
     */
    private String abilityName;

    /**
     * 所属能力分组描述
     */
    private String abilityDescription;

    /**
     * 目标 Bean 的 Spring Bean 名称
     */
    private String beanName;

    /**
     * 目标 Bean 实例
     */
    private transient Object targetBean;

    /**
     * 目标方法
     */
    private transient Method targetMethod;

    /**
     * 参数元数据列表（包含所有参数，含 AI 不可见的参数）
     */
    private List<AiParamMeta> paramMetas;

    /**
     * 获取仅对 AI 可见的参数列表
     */
    public List<AiParamMeta> getAiVisibleParams() {
        if (paramMetas == null) {
            return List.of();
        }
        return paramMetas.stream()
                .filter(AiParamMeta::isAiVisible)
                .toList();
    }

    /**
     * 获取需要 JWT 注入的参数列表
     */
    public List<AiParamMeta> getJwtParams() {
        if (paramMetas == null) {
            return List.of();
        }
        return paramMetas.stream()
                .filter(p -> p.getSource() == com.aidegear.common.enums.ParamSource.JWT)
                .toList();
    }

    /**
     * 获取需要系统注入的参数列表
     */
    public List<AiParamMeta> getSystemParams() {
        if (paramMetas == null) {
            return List.of();
        }
        return paramMetas.stream()
                .filter(p -> p.getSource() == com.aidegear.common.enums.ParamSource.SYSTEM)
                .toList();
    }
}
