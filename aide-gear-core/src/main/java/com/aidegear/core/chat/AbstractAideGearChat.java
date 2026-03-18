package com.aidegear.core.chat;

import com.aidegear.common.model.ActionCallRequest;
import com.aidegear.common.model.ActionResult;
import com.aidegear.common.model.AiActionMeta;
import com.aidegear.common.model.AiParamMeta;
import com.aidegear.core.executor.ActionExecutor;
import com.aidegear.core.registry.AiActionRegistry;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI 原子能力抽象基类 —— 提示词驱动的业务方法编排。
 *
 * <h2>核心流程</h2>
 * <pre>
 *   用户问题
 *      ↓
 *   ① 分类识别（框架构建提示词 → 用户的 callLlm → 框架解析）
 *      ↓
 *   ② Action 选择 + 参数提取（框架构建提示词 → 用户的 callLlm → 框架解析 JSON）
 *      ↓
 *   ③ 执行业务方法（ActionExecutor，自动注入 JWT/SYSTEM 参数）
 *      ↓
 *   返回 List&lt;ActionResult&gt;（原始业务数据）
 * </pre>
 *
 * <h2>用户使用方式（继承）</h2>
 * <pre>
 * {@code
 * @Service
 * public class MyAiService extends AbstractAideGearChat {
 *
 *     @Override
 *     protected String callLlm(String prompt) {
 *         // 使用自己的 LLM 客户端
 *         return myLlmClient.chat(prompt);
 *     }
 * }
 * }
 * </pre>
 *
 * <h2>可组合的原子构建块</h2>
 * <ul>
 *   <li>{@link #identifyAbility(String)} — 识别相关业务分类</li>
 *   <li>{@link #selectAndExecuteActions(String, List)} — 选择 Action + 提参 + 执行</li>
 *   <li>{@link #chat(String, List)} — 指定分类直接执行</li>
 * </ul>
 *
 * @author aide-gear
 * @since 1.0.0
 */
@Slf4j
public abstract class AbstractAideGearChat {

    @Resource
    protected AiActionRegistry registry;

    @Resource
    protected ActionExecutor actionExecutor;

    // =========================================================
    // 抽象方法：用户必须实现
    // =========================================================

    /**
     * 调用大语言模型，返回文本响应。
     * <p>
     * 用户在此方法中封装自己的 LLM 客户端调用逻辑，框架在编排流程中统一调用此方法。
     * </p>
     *
     * @param prompt 框架构建好的完整提示词
     * @return LLM 返回的文本内容
     */
    protected abstract String callLlm(String prompt);

    // =========================================================
    // 可覆盖方法：提示词定制
    // =========================================================

    /**
     * 构建分类识别提示词。
     * <p>
     * 默认内置中文提示词模板，用户可覆盖此方法以自定义语言、风格或约束。
     * </p>
     *
     * @param question   用户问题
     * @param abilities  所有可用分类，key=分类名，value=分类描述
     * @return 提示词字符串
     */
    protected String buildAbilityIdentificationPrompt(String question, Map<String, String> abilities) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个意图分类助手。根据用户的问题，从以下业务分类中选出所有相关的分类。\n\n");
        sb.append("可用业务分类：\n");
        abilities.forEach((name, desc) -> {
            sb.append("- ").append(name);
            if (desc != null && !desc.isBlank()) {
                sb.append("：").append(desc);
            }
            sb.append("\n");
        });
        sb.append("\n用户问题：").append(question).append("\n\n");
        sb.append("请只返回相关的分类名称，用英文逗号分隔。");
        sb.append("如果没有匹配的分类，返回 NONE。不要返回任何解释或多余内容。");
        return sb.toString();
    }

    /**
     * 构建 Action 选择 + 参数提取提示词。
     * <p>
     * 默认内置中文提示词模板，用户可覆盖此方法以自定义格式或语言。
     * </p>
     *
     * @param question 用户问题
     * @param actions  候选 Action 列表（仅包含 AI 可见参数）
     * @return 提示词字符串
     */
    protected String buildActionSelectionPrompt(String question, List<AiActionMeta> actions) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个方法调用助手。根据用户的问题，从以下可用方法中选择需要调用的方法，并从问题中提取参数值。\n\n");
        sb.append("可用方法：\n");
        for (int i = 0; i < actions.size(); i++) {
            AiActionMeta meta = actions.get(i);
            sb.append(i + 1).append(". 方法ID: ").append(meta.getActionId()).append("\n");
            sb.append("   名称: ").append(meta.getActionName()).append("\n");
            if (meta.getDescription() != null && !meta.getDescription().isBlank()) {
                sb.append("   描述: ").append(meta.getDescription()).append("\n");
            }
            if (meta.getReturnDesc() != null && !meta.getReturnDesc().isBlank()) {
                sb.append("   返回: ").append(meta.getReturnDesc()).append("\n");
            }
            List<AiParamMeta> visibleParams = meta.getAiVisibleParams();
            if (!visibleParams.isEmpty()) {
                sb.append("   参数:\n");
                for (AiParamMeta param : visibleParams) {
                    sb.append("     - ").append(param.getParamName())
                            .append("(").append(param.getParamType().getSimpleName()).append(")");
                    if (param.getDescription() != null && !param.getDescription().isBlank()) {
                        sb.append(": ").append(param.getDescription());
                    }
                    if (param.getExample() != null && !param.getExample().isBlank()) {
                        sb.append(", 示例: ").append(param.getExample());
                    }
                    if (!param.isRequired()) {
                        sb.append(" [可选]");
                    }
                    sb.append("\n");
                }
            } else {
                sb.append("   参数: 无\n");
            }
            sb.append("\n");
        }
        sb.append("用户问题：").append(question).append("\n\n");
        sb.append("请以 JSON 数组格式返回，每个元素包含 actionId 和 arguments 字段。\n");
        sb.append("示例：[{\"actionId\": \"分类名.方法名\", \"arguments\": {\"参数名\": \"参数值\"}}]\n");
        sb.append("如果没有需要调用的方法，返回空数组 []。\n");
        sb.append("只返回 JSON，不要包含任何解释或 markdown 代码块标记。");
        return sb.toString();
    }

    /**
     * 解析 LLM 返回的分类名字符串，提取有效的分类名列表。
     * <p>
     * 默认实现：按逗号分隔，并校验每个名称是否在已注册分类中存在（过滤幻觉）。
     * 用户可覆盖此方法以适配自定义提示词的输出格式。
     * </p>
     *
     * @param llmResponse LLM 返回的原始文本
     * @param validNames  已注册的合法分类名集合
     * @return 解析出的有效分类名列表，可能为空（框架会兜底全部分类）
     */
    protected List<String> parseIdentifiedAbilities(String llmResponse, Set<String> validNames) {
        if (llmResponse == null || llmResponse.isBlank() || llmResponse.trim().equalsIgnoreCase("NONE")) {
            return Collections.emptyList();
        }
        return Arrays.stream(llmResponse.split("[,，]"))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .filter(validNames::contains)
                .collect(Collectors.toList());
    }

    /**
     * 解析 LLM 返回的 JSON 串，转换为 ActionCallRequest 列表。
     * <p>
     * 默认实现：解析 JSON 数组，校验 actionId 在 registry 中存在（防止幻觉）。
     * 用户可覆盖此方法以适配自定义提示词的输出格式。
     * </p>
     *
     * @param llmResponse LLM 返回的 JSON 字符串
     * @return 解析出的调用请求列表，格式异常时返回空列表
     */
    protected List<ActionCallRequest> parseActionCalls(String llmResponse) {
        if (llmResponse == null || llmResponse.isBlank()) {
            return Collections.emptyList();
        }
        try {
            // 兼容 LLM 可能返回 markdown 代码块包裹的 JSON
            String json = llmResponse.trim();
            if (json.startsWith("```")) {
                json = json.replaceAll("(?s)```[a-z]*\\n?(.*?)```", "$1").trim();
            }
            JSONArray array = JSON.parseArray(json);
            if (array == null || array.isEmpty()) {
                return Collections.emptyList();
            }
            List<ActionCallRequest> requests = new ArrayList<>();
            for (int i = 0; i < array.size(); i++) {
                JSONObject item = array.getJSONObject(i);
                String actionId = item.getString("actionId");
                if (actionId == null || actionId.isBlank()) {
                    log.warn("[AideGear] 解析 ActionCall 时 actionId 为空，跳过第 {} 项", i);
                    continue;
                }
                // 校验 actionId 合法性
                if (!registry.contains(actionId)) {
                    log.warn("[AideGear] LLM 返回了不存在的 actionId: {}，已过滤", actionId);
                    continue;
                }
                Map<String, Object> arguments = Collections.emptyMap();
                JSONObject args = item.getJSONObject("arguments");
                if (args != null) {
                    arguments = args.toJavaObject(Map.class);
                }
                requests.add(ActionCallRequest.builder()
                        .actionId(actionId)
                        .arguments(arguments)
                        .build());
            }
            return requests;
        } catch (Exception e) {
            log.warn("[AideGear] 解析 LLM 返回的 ActionCall JSON 失败，原始内容: {}", llmResponse, e);
            return Collections.emptyList();
        }
    }

    // =========================================================
    // 原子能力方法：完整流程
    // =========================================================

    /**
     * 全量分类对话：自动识别分类 → 选择 Action + 提参 → 执行 → 返回原始业务数据。
     *
     * @param question 用户自然语言问题
     * @return 业务方法执行结果列表（原始 JSON 数据）
     */
    public List<ActionResult> chat(String question) {
        log.info("[AideGear] 开始处理问题（全量分类）: {}", question);
        List<String> abilities = identifyAbility(question);
        log.info("[AideGear] 识别到相关分类: {}", abilities);
        return selectAndExecuteActions(question, abilities);
    }

    /**
     * 指定分类对话：跳过分类识别 → 选择 Action + 提参 → 执行 → 返回原始业务数据。
     *
     * @param question     用户自然语言问题
     * @param abilityNames 指定的分类名称列表
     * @return 业务方法执行结果列表（原始 JSON 数据）
     */
    public List<ActionResult> chat(String question, List<String> abilityNames) {
        log.info("[AideGear] 开始处理问题（指定分类 {}）: {}", abilityNames, question);
        return selectAndExecuteActions(question, abilityNames);
    }

    // =========================================================
    // 可组合的构建块
    // =========================================================

    /**
     * 从所有已注册分类中，识别与用户问题相关的分类。
     * <p>
     * 如果只有 1 个分类，直接返回，跳过 LLM 调用。
     * 识别失败（LLM 返回 NONE 或解析结果为空）时，兜底返回全部分类。
     * </p>
     *
     * @param question 用户问题
     * @return 相关分类名称列表（不为空）
     */
    public List<String> identifyAbility(String question) {
        Map<String, String> summaries = registry.getAbilitySummaries();
        return identifyAbilityFromSummaries(question, summaries);
    }

    /**
     * 从指定候选分类中，识别与用户问题相关的分类。
     *
     * @param question   用户问题
     * @param candidates 候选分类名称列表
     * @return 相关分类名称列表（不为空）
     */
    public List<String> identifyAbility(String question, List<String> candidates) {
        Map<String, String> allSummaries = registry.getAbilitySummaries();
        // 只保留候选范围内的分类
        Map<String, String> filteredSummaries = allSummaries.entrySet().stream()
                .filter(e -> candidates.contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return identifyAbilityFromSummaries(question, filteredSummaries);
    }

    /**
     * 在指定分类范围内，选择 Action + 提取参数 + 执行业务方法。
     *
     * @param question     用户问题
     * @param abilityNames 目标分类名称列表
     * @return 执行结果列表
     */
    public List<ActionResult> selectAndExecuteActions(String question, List<String> abilityNames) {
        // 1. 获取这些分类下的所有 Action
        List<AiActionMeta> actions = registry.getActionsByAbilities(abilityNames);
        if (actions.isEmpty()) {
            log.warn("[AideGear] 分类 {} 下未找到任何 Action", abilityNames);
            return Collections.emptyList();
        }

        // 2. 构建 Action 选择 + 参数提取提示词
        String prompt = buildActionSelectionPrompt(question, actions);
        log.debug("[AideGear] Action 选择提示词:\n{}", prompt);

        // 3. 调用 LLM
        String llmResponse = callLlm(prompt);
        log.debug("[AideGear] LLM 返回的 ActionCall JSON: {}", llmResponse);

        // 4. 解析 LLM 响应
        List<ActionCallRequest> callRequests = parseActionCalls(llmResponse);
        if (callRequests.isEmpty()) {
            log.info("[AideGear] LLM 未选择任何 Action 调用");
            return Collections.emptyList();
        }

        // 5. 逐一执行 Action
        List<ActionResult> results = new ArrayList<>();
        for (ActionCallRequest req : callRequests) {
            log.info("[AideGear] 执行 Action: {}, 参数: {}", req.getActionId(), req.getArguments());
            ActionResult result = actionExecutor.execute(req.getActionId(), req.getArguments());
            results.add(result);
            if (!result.isSuccess()) {
                log.warn("[AideGear] Action 执行失败: {}, 原因: {}", req.getActionId(), result.getErrorMessage());
            }
        }

        return results;
    }

    // =========================================================
    // 内部辅助方法
    // =========================================================

    private List<String> identifyAbilityFromSummaries(String question, Map<String, String> summaries) {
        List<String> allNames = new ArrayList<>(summaries.keySet());

        // 单分类时跳过 LLM 调用
        if (allNames.size() == 1) {
            log.debug("[AideGear] 只有 1 个分类，直接使用: {}", allNames.get(0));
            return allNames;
        }

        // 无分类时返回空列表
        if (allNames.isEmpty()) {
            log.warn("[AideGear] 未注册任何 @AiAbility 分类");
            return Collections.emptyList();
        }

        // 构建提示词并调用 LLM
        String prompt = buildAbilityIdentificationPrompt(question, summaries);
        log.debug("[AideGear] 分类识别提示词:\n{}", prompt);

        String llmResponse = callLlm(prompt);
        log.debug("[AideGear] LLM 识别结果: {}", llmResponse);

        // 解析并校验
        List<String> identified = parseIdentifiedAbilities(llmResponse, summaries.keySet());

        // 识别失败时兜底全部分类
        if (identified.isEmpty()) {
            log.warn("[AideGear] 分类识别失败（LLM 返回 '{}'），兜底使用全部分类", llmResponse);
            return allNames;
        }

        return identified;
    }
}
