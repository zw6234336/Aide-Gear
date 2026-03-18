package com.aidegear.core.security;

import com.aidegear.common.model.AiParamMeta;

/**
 * JWT 参数解析器接口 - 从 Authorization Header 中解析用户身份信息。
 * <p>
 * 使用方需实现此接口，提供 JWT 解析逻辑。
 * 框架会在动作执行时自动调用此解析器，将结果注入到标记为 {@code ParamSource.JWT} 的参数中。
 * </p>
 *
 * <pre>{@code
 * @Component("jwtParamResolver")
 * public class MyJwtParamResolver implements JwtParamResolver {
 *     @Override
 *     public Object resolve(AiParamMeta paramMeta) {
 *         HttpServletRequest request = getRequest();
 *         String token = request.getHeader("Authorization");
 *         // 解析 JWT 并提取 claim
 *         Claims claims = Jwts.parser().parseClaimsJws(token).getBody();
 *         return claims.get(paramMeta.getJwtKey());
 *     }
 * }
 * }</pre>
 *
 * @author wayne
 * @since 1.0.0
 */
public interface JwtParamResolver {

    /**
     * 从当前请求的 JWT 中解析参数值
     *
     * @param paramMeta 参数元数据（包含 jwtKey 等信息）
     * @return 解析出的参数值
     */
    Object resolve(AiParamMeta paramMeta);
}
