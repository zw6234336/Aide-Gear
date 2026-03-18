package com.aidegear.demo.config;

import com.aidegear.common.model.AiParamMeta;
import com.aidegear.core.security.JwtParamResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 示例：自定义 JWT 参数解析器实现。
 * <p>
 * 实际项目中应替换为真实的 JWT 解析逻辑（如 io.jsonwebtoken 库）。
 * 此处为演示目的，从 Header 中简单提取 userId。
 * </p>
 *
 * @author wayne
 * @since 1.0.0
 */
@Slf4j
@Component("jwtParamResolver")
public class DemoJwtParamResolver implements JwtParamResolver {

    @Override
    public Object resolve(AiParamMeta paramMeta) {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            log.warn("[Demo] 无法获取当前请求上下文, 参数: {}", paramMeta.getParamName());
            return null;
        }

        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            log.warn("[Demo] 请求中无 Authorization Header");
            return getDefaultValue(paramMeta);
        }

        // TODO: 实际项目中应使用 JWT 库解析 Token
        // Claims claims = Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
        // return claims.get(paramMeta.getJwtKey());

        // 演示：简单从 Header 中提取 (实际项目请替换为真实 JWT 解析)
        String jwtKey = paramMeta.getJwtKey();
        String headerValue = request.getHeader("X-" + jwtKey);
        if (headerValue != null) {
            return convertToParamType(headerValue, paramMeta.getParamType());
        }

        return getDefaultValue(paramMeta);
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    private Object getDefaultValue(AiParamMeta paramMeta) {
        // 演示用默认值
        if (paramMeta.getParamType() == Long.class || paramMeta.getParamType() == long.class) {
            return 10001L; // 默认演示用户ID
        }
        return null;
    }

    private Object convertToParamType(String value, Class<?> targetType) {
        if (targetType == Long.class || targetType == long.class) {
            return Long.parseLong(value);
        } else if (targetType == Integer.class || targetType == int.class) {
            return Integer.parseInt(value);
        } else if (targetType == String.class) {
            return value;
        }
        return value;
    }
}
