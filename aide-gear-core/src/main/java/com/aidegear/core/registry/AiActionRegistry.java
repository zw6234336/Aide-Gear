package com.aidegear.core.registry;

import com.aidegear.common.model.AiActionMeta;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 能力注册中心 - 存储所有扫描到的 AI 动作元数据。
 * <p>
 * 线程安全，支持运行时动态注册和查询。
 * </p>
 *
 * @author wayne
 * @since 1.0.0
 */
@Slf4j
@Component
public class AiActionRegistry {

    /**
     * 动作注册表 key=actionId, value=AiActionMeta
     */
    private final Map<String, AiActionMeta> actionMap = new ConcurrentHashMap<>();

    /**
     * 注册一个 AI 动作
     *
     * @param meta 动作元数据
     */
    public void register(AiActionMeta meta) {
        String actionId = meta.getActionId();
        if (actionMap.containsKey(actionId)) {
            log.warn("[AideGear] 动作ID重复, 将覆盖: {}", actionId);
        }
        actionMap.put(actionId, meta);
        log.info("[AideGear] 注册动作: {} -> {}.{}",
                actionId, meta.getBeanName(), meta.getTargetMethod().getName());
    }

    /**
     * 根据 actionId 获取动作元数据
     */
    public AiActionMeta getAction(String actionId) {
        return actionMap.get(actionId);
    }

    /**
     * 获取所有已注册的动作
     */
    public Collection<AiActionMeta> getAllActions() {
        return Collections.unmodifiableCollection(actionMap.values());
    }

    /**
     * 获取已注册动作数量
     */
    public int size() {
        return actionMap.size();
    }

    /**
     * 判断是否存在指定 actionId 的动作
     */
    public boolean contains(String actionId) {
        return actionMap.containsKey(actionId);
    }
}
