package com.aidegear.common.model;

import com.aidegear.common.enums.ParamSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 参数元数据 - 描述单个方法参数的完整信息。
 *
 * @author wayne
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiParamMeta {

    /**
     * 参数名（Java 反射获取的参数名）
     */
    private String paramName;

    /**
     * 参数描述（@AiParam 的 value）
     */
    private String description;

    /**
     * 参数 Java 类型
     */
    private Class<?> paramType;

    /**
     * 参数来源
     */
    private ParamSource source;

    /**
     * 是否必填
     */
    private boolean required;

    /**
     * JWT Claim Key（source=JWT 时有效）
     */
    private String jwtKey;

    /**
     * 系统属性 Key（source=SYSTEM 时有效）
     */
    private String systemKey;

    /**
     * 参数示例值
     */
    private String example;

    /**
     * 参数在方法中的位置索引
     */
    private int paramIndex;

    /**
     * 判断是否对 AI 可见
     */
    public boolean isAiVisible() {
        return source == ParamSource.CONVERSATION;
    }
}
