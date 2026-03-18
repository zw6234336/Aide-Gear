package com.aidegear.core.security;

import com.aidegear.common.model.AiParamMeta;
import lombok.extern.slf4j.Slf4j;

/**
 * 默认系统参数解析器 - 当用户未提供自定义实现时使用。
 *
 * @author wayne
 * @since 1.0.0
 */
@Slf4j
public class DefaultSystemParamResolver implements SystemParamResolver {

    @Override
    public Object resolve(AiParamMeta paramMeta) {
        log.warn("[AideGear] 使用默认 System 解析器, 参数 [{}] 将返回 null。" +
                        "请实现 SystemParamResolver 接口并注册为 Spring Bean (name='systemParamResolver')",
                paramMeta.getParamName());
        return null;
    }
}
