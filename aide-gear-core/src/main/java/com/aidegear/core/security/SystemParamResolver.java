package com.aidegear.core.security;

import com.aidegear.common.model.AiParamMeta;

/**
 * 系统参数解析器接口 - 从 ThreadLocal 或系统上下文中提取参数。
 * <p>
 * 用于注入租户ID、请求链路TraceId 等系统级信息。
 * </p>
 *
 * @author wayne
 * @since 1.0.0
 */
public interface SystemParamResolver {

    /**
     * 从系统上下文中解析参数值
     *
     * @param paramMeta 参数元数据（包含 systemKey 等信息）
     * @return 解析出的参数值
     */
    Object resolve(AiParamMeta paramMeta);
}
