package com.aidegear.core.endpoint;

import com.aidegear.common.model.AiActionMeta;
import com.aidegear.common.model.AiParamMeta;
import com.aidegear.core.registry.AiActionRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 能力详情端点 - 提供单个动作的参数详情供 AI 模型获取调用规范。
 * <p>
 * 仅暴露 AI 可见的参数（CONVERSATION 来源），JWT 和 SYSTEM 参数自动隐藏。
 * </p>
 *
 * @author wayne
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/aide-gear")
public class CapabilityDetailEndpoint {

    @Resource
    private AiActionRegistry registry;

    /**
     * 获取指定动作的详细信息（含 AI 可见参数）
     *
     * @param actionId 动作ID
     */
    @GetMapping("/capabilities/detail")
    public Map<String, Object> getActionDetail(@RequestParam String actionId) {
        AiActionMeta meta = registry.getAction(actionId);
        if (meta == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "未找到动作: " + actionId);
            return error;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("actionId", meta.getActionId());
        result.put("actionName", meta.getActionName());
        result.put("description", meta.getDescription());
        result.put("returnDesc", meta.getReturnDesc());
        result.put("abilityName", meta.getAbilityName());
        result.put("abilityDescription", meta.getAbilityDescription());

        // 仅返回 AI 可见参数（安全过滤）
        List<Map<String, Object>> params = meta.getAiVisibleParams().stream()
                .map(this::toParamInfo)
                .collect(Collectors.toList());
        result.put("parameters", params);

        return result;
    }

    private Map<String, Object> toParamInfo(AiParamMeta param) {
        Map<String, Object> info = new HashMap<>();
        info.put("name", param.getParamName());
        info.put("description", param.getDescription());
        info.put("type", param.getParamType().getSimpleName());
        info.put("required", param.isRequired());
        if (param.getExample() != null && !param.getExample().isEmpty()) {
            info.put("example", param.getExample());
        }
        return info;
    }
}
