package com.aidegear.core.executor;

import com.aidegear.common.enums.ParamSource;
import com.aidegear.common.model.ActionResult;
import com.aidegear.common.model.AiActionMeta;
import com.aidegear.common.model.AiParamMeta;
import com.aidegear.core.registry.AiActionRegistry;
import com.aidegear.core.security.JwtParamResolver;
import com.aidegear.core.security.SystemParamResolver;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * AI 动作执行器 - AI 触发工具调用时的统一入口。
 * <p>
 * 核心安全机制：执行器会截获 AI 的调用请求，根据 {@link ParamSource} 定义，
 * 将 AI 提供的业务参数与系统注入的安全参数合并后，才调用目标方法。
 * </p>
 *
 * @author wayne
 * @since 1.0.0
 */
@Slf4j
@Component
public class ActionExecutor {

    @Resource
    private AiActionRegistry registry;

    @Resource(name = "jwtParamResolver")
    private JwtParamResolver jwtParamResolver;

    @Resource(name = "systemParamResolver")
    private SystemParamResolver systemParamResolver;

    /**
     * 执行 AI 动作
     *
     * @param actionId      动作ID
     * @param aiArguments   AI 提供的参数 (仅 CONVERSATION 来源的参数)
     * @return 执行结果
     */
    public ActionResult execute(String actionId, Map<String, Object> aiArguments) {
        long startTime = System.currentTimeMillis();

        AiActionMeta meta = registry.getAction(actionId);
        if (meta == null) {
            return ActionResult.fail(actionId, "未找到动作: " + actionId, 0);
        }

        try {
            // 1. 构建完整参数数组（合并 AI 参数 + 安全注入参数）
            Object[] args = buildMethodArgs(meta, aiArguments);

            // 2. 调用目标方法
            Method method = meta.getTargetMethod();
            method.setAccessible(true);
            Object result = method.invoke(meta.getTargetBean(), args);

            // 3. 序列化结果
            String data = (result != null) ? JSON.toJSONString(result) : "null";

            long costMs = System.currentTimeMillis() - startTime;
            log.info("[AideGear] 动作执行成功: {}, 耗时: {}ms", actionId, costMs);
            return ActionResult.success(actionId, data, costMs);

        } catch (Exception e) {
            long costMs = System.currentTimeMillis() - startTime;
            log.error("[AideGear] 动作执行失败: {}", actionId, e);
            return ActionResult.fail(actionId, e.getMessage(), costMs);
        }
    }

    /**
     * 构建方法调用参数。
     * <p>安全核心逻辑：JWT 和 SYSTEM 来源的参数由框架强制注入，即使 AI 尝试伪造也会被覆盖。</p>
     */
    private Object[] buildMethodArgs(AiActionMeta meta, Map<String, Object> aiArguments) {
        List<AiParamMeta> paramMetas = meta.getParamMetas();
        Object[] args = new Object[paramMetas.size()];

        for (int i = 0; i < paramMetas.size(); i++) {
            AiParamMeta param = paramMetas.get(i);

            switch (param.getSource()) {
                case CONVERSATION -> {
                    // AI 提供的参数
                    Object value = aiArguments != null ? aiArguments.get(param.getParamName()) : null;
                    args[i] = convertType(value, param.getParamType());
                }
                case JWT -> {
                    // 从 JWT 强制注入（安全隔离）
                    args[i] = jwtParamResolver.resolve(param);
                }
                case SYSTEM -> {
                    // 从系统上下文强制注入
                    args[i] = systemParamResolver.resolve(param);
                }
            }
        }

        return args;
    }

    /**
     * 简单类型转换（将 AI 传入的 Object 转为目标类型）
     */
    private Object convertType(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        if (targetType.isInstance(value)) {
            return value;
        }

        String strValue = String.valueOf(value);

        if (targetType == String.class) {
            return strValue;
        } else if (targetType == Long.class || targetType == long.class) {
            return Long.parseLong(strValue);
        } else if (targetType == Integer.class || targetType == int.class) {
            return Integer.parseInt(strValue);
        } else if (targetType == Double.class || targetType == double.class) {
            return Double.parseDouble(strValue);
        } else if (targetType == Boolean.class || targetType == boolean.class) {
            return Boolean.parseBoolean(strValue);
        }

        // 复杂类型使用 JSON 反序列化
        return JSON.parseObject(JSON.toJSONString(value), targetType);
    }
}
