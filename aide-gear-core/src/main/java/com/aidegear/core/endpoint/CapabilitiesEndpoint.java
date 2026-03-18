package com.aidegear.core.endpoint;

import com.aidegear.common.model.AiActionMeta;
import com.aidegear.common.model.AiParamMeta;
import com.aidegear.core.registry.AiActionRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 能力暴露端点 - 提供 capabilities/list 接口供 AI 模型发现可用技能。
 * <p>
 * <b>安全过滤</b>：自动隐藏所有标记为 JWT 或 SYSTEM 来源的参数，
 * 确保 AI 看到的 API 定义是纯净且安全的。
 * </p>
 *
 * @author wayne
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/aide-gear")
public class CapabilitiesEndpoint {

    @Resource
    private AiActionRegistry registry;

    /**
     * 列出所有可用的 AI 能力（安全过滤后）
     */
    @GetMapping("/capabilities/list")
    public Map<String, Object> listCapabilities() {
        Map<String, Object> result = new HashMap<>();
        result.put("version", "1.0.0");
        result.put("totalActions", registry.size());

        List<Map<String, Object>> actions = registry.getAllActions().stream()
                .map(this::toSafeActionInfo)
                .collect(Collectors.toList());

        result.put("actions", actions);
        return result;
    }

    /**
     * 将动作元数据转为安全的对外暴露格式（过滤非 AI 可见参数）
     */
    private Map<String, Object> toSafeActionInfo(AiActionMeta meta) {
        Map<String, Object> info = new HashMap<>();
        info.put("actionId", meta.getActionId());
        info.put("actionName", meta.getActionName());
        info.put("description", meta.getDescription());
        info.put("abilityName", meta.getAbilityName());
        info.put("returnDesc", meta.getReturnDesc());

        // 仅暴露 AI 可见的参数（CONVERSATION 来源）
        List<Map<String, Object>> params = meta.getAiVisibleParams().stream()
                .map(this::toParamInfo)
                .collect(Collectors.toList());

        info.put("parameters", params);
        return info;
    }

    /**
     * 将参数元数据转为对外暴露格式
     */
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
