package com.aidegear.core.security;

import com.aidegear.common.model.AiParamMeta;
import lombok.extern.slf4j.Slf4j;

/**
 * 默认 JWT 参数解析器 - 当用户未提供自定义实现时使用。
 * <p>
 * 默认实现返回 null，并输出警告日志提示用户实现自定义解析器。
 * </p>
 *
 * @author wayne
 * @since 1.0.0
 */
@Slf4j
public class DefaultJwtParamResolver implements JwtParamResolver {

    @Override
    public Object resolve(AiParamMeta paramMeta) {
        log.warn("[AideGear] 使用默认 JWT 解析器, 参数 [{}] 将返回 null。" +
                        "请实现 JwtParamResolver 接口并注册为 Spring Bean (name='jwtParamResolver')",
                paramMeta.getParamName());
        return null;
    }
}
