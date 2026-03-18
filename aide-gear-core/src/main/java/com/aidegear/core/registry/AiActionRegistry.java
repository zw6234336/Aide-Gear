package com.aidegear.core.registry;

import com.aidegear.common.model.AiActionMeta;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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

    /**
     * 获取指定 ability 分类下的所有动作
     *
     * @param abilityName 分类名称（@AiAbility 的 name）
     * @return 该分类下的动作列表，不存在则返回空列表
     */
    public List<AiActionMeta> getActionsByAbility(String abilityName) {
        return actionMap.values().stream()
                .filter(meta -> abilityName.equals(meta.getAbilityName()))
                .collect(Collectors.toList());
    }

    /**
     * 批量获取多个 ability 分类下的所有动作
     *
     * @param abilityNames 分类名称列表
     * @return 这些分类下的所有动作列表
     */
    public List<AiActionMeta> getActionsByAbilities(List<String> abilityNames) {
        if (abilityNames == null || abilityNames.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> nameSet = Set.copyOf(abilityNames);
        return actionMap.values().stream()
                .filter(meta -> nameSet.contains(meta.getAbilityName()))
                .collect(Collectors.toList());
    }

    /**
     * 获取所有已注册的 ability 分类名称
     *
     * @return 去重后的分类名称集合
     */
    public Set<String> getAbilityNames() {
        return actionMap.values().stream()
                .map(AiActionMeta::getAbilityName)
                .collect(Collectors.toSet());
    }

    /**
     * 获取所有分类的摘要信息（名称 → 描述），用于 LLM 分类识别提示词构建
     *
     * @return Map&lt;abilityName, abilityDescription&gt;，保持插入顺序
     */
    public Map<String, String> getAbilitySummaries() {
        Map<String, String> summaries = new LinkedHashMap<>();
        for (AiActionMeta meta : actionMap.values()) {
            summaries.putIfAbsent(meta.getAbilityName(),
                    meta.getAbilityDescription() != null ? meta.getAbilityDescription() : "");
        }
        return Collections.unmodifiableMap(summaries);
    }
}
