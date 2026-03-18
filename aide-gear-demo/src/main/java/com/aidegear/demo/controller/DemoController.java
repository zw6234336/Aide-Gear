package com.aidegear.demo.controller;

import com.aidegear.common.model.ActionResult;
import com.aidegear.common.model.AiActionMeta;
import com.aidegear.common.model.AiParamMeta;
import com.aidegear.core.executor.ActionExecutor;
import com.aidegear.core.registry.AiActionRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 演示控制器 - 展示完整的 AI 发现 → 查询列表 → 查看详情 → 调用执行 链路。
 * <p>
 * 提供一个 /demo/full-chain 端点，串联整个流程并返回每一步的结果。
 * </p>
 *
 * @author wayne
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/demo")
public class DemoController {

    @Resource
    private AiActionRegistry registry;

    @Resource
    private ActionExecutor actionExecutor;

    /**
     * 完整链路演示：发现 → 列表 → 详情 → 调用
     * <p>
     * 模拟 AI 的完整调用过程：
     * 1. 发现服务中注册了哪些 AI 能力
     * 2. 查看能力列表（安全过滤后）
     * 3. 获取某个能力的参数详情
     * 4. 使用参数调用该能力
     * </p>
     */
    @GetMapping("/full-chain")
    public Map<String, Object> fullChainDemo() {
        Map<String, Object> result = new LinkedHashMap<>();

        // ===== 第一步：发现 - 检查注册了多少个 AI 能力 =====
        Map<String, Object> step1 = new LinkedHashMap<>();
        int totalActions = registry.size();
        step1.put("description", "发现 AI 能力数量");
        step1.put("totalActions", totalActions);
        step1.put("message", String.format("系统中共注册了 %d 个 AI 可调用的技能", totalActions));
        result.put("step1_发现能力", step1);

        // ===== 第二步：查询能力列表（安全过滤后） =====
        Map<String, Object> step2 = new LinkedHashMap<>();
        List<Map<String, Object>> actionList = registry.getAllActions().stream()
                .map(meta -> {
                    Map<String, Object> info = new LinkedHashMap<>();
                    info.put("actionId", meta.getActionId());
                    info.put("actionName", meta.getActionName());
                    info.put("description", meta.getDescription());
                    info.put("abilityName", meta.getAbilityName());
                    return info;
                })
                .collect(Collectors.toList());
        step2.put("description", "获取所有可用能力列表（仅暴露安全信息，隐藏 JWT/SYSTEM 参数）");
        step2.put("actions", actionList);
        result.put("step2_查询列表", step2);

        // ===== 第三步：查看能力详情 - 选择"查询产品详情"获取参数 =====
        String targetActionId = "产品查询服务.查询产品详情";
        AiActionMeta targetMeta = registry.getAction(targetActionId);

        Map<String, Object> step3 = new LinkedHashMap<>();
        if (targetMeta != null) {
            step3.put("description", String.format("获取 [%s] 的参数详情", targetActionId));
            step3.put("actionId", targetMeta.getActionId());
            step3.put("actionName", targetMeta.getActionName());
            step3.put("actionDesc", targetMeta.getDescription());
            step3.put("returnDesc", targetMeta.getReturnDesc());

            List<Map<String, Object>> visibleParams = targetMeta.getAiVisibleParams().stream()
                    .map(p -> {
                        Map<String, Object> paramInfo = new LinkedHashMap<>();
                        paramInfo.put("name", p.getParamName());
                        paramInfo.put("description", p.getDescription());
                        paramInfo.put("type", p.getParamType().getSimpleName());
                        paramInfo.put("required", p.isRequired());
                        if (p.getExample() != null && !p.getExample().isEmpty()) {
                            paramInfo.put("example", p.getExample());
                        }
                        return paramInfo;
                    })
                    .collect(Collectors.toList());
            step3.put("aiVisibleParameters", visibleParams);
            step3.put("hiddenParamCount",
                    targetMeta.getParamMetas().size() - targetMeta.getAiVisibleParams().size());
        } else {
            step3.put("error", "未找到目标动作: " + targetActionId);
        }
        result.put("step3_查看详情", step3);

        // ===== 第四步：调用执行 - AI 填充参数并调用 =====
        Map<String, Object> step4 = new LinkedHashMap<>();
        step4.put("description", String.format("调用 [%s]，模拟 AI 填充参数", targetActionId));

        Map<String, Object> aiArguments = new HashMap<>();
        aiArguments.put("riskCode", "RISK001");
        step4.put("aiProvidedArguments", aiArguments);

        ActionResult invokeResult = actionExecutor.execute(targetActionId, aiArguments);
        step4.put("executeResult", invokeResult);
        result.put("step4_调用执行", step4);

        // ===== 第五步：调用含 JWT 参数的能力（演示安全隔离） =====
        String jwtActionId = "个人中心服务.查询我的保单";
        Map<String, Object> step5 = new LinkedHashMap<>();
        step5.put("description", String.format("调用 [%s]（演示 JWT 参数自动注入，AI 无需也无法提供 userId）", jwtActionId));

        Map<String, Object> aiArgs2 = new HashMap<>();
        aiArgs2.put("type", "寿险");
        step5.put("aiProvidedArguments", aiArgs2);
        step5.put("note", "userId 由框架从 JWT 自动注入，AI 仅需提供 type 参数");

        ActionResult jwtResult = actionExecutor.execute(jwtActionId, aiArgs2);
        step5.put("executeResult", jwtResult);
        result.put("step5_安全隔离调用", step5);

        return result;
    }

    /**
     * 单独的能力发现端点 - 模拟 AI 首次连接时的能力发现
     */
    @GetMapping("/discover")
    public Map<String, Object> discover() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("serviceName", "aide-gear-demo");
        result.put("version", "1.0.0");
        result.put("totalAbilities", registry.getAllActions().stream()
                .map(AiActionMeta::getAbilityName)
                .distinct()
                .count());
        result.put("totalActions", registry.size());

        // 按能力分组
        Map<String, List<Map<String, Object>>> grouped = registry.getAllActions().stream()
                .collect(Collectors.groupingBy(
                        AiActionMeta::getAbilityName,
                        LinkedHashMap::new,
                        Collectors.mapping(meta -> {
                            Map<String, Object> info = new LinkedHashMap<>();
                            info.put("actionId", meta.getActionId());
                            info.put("name", meta.getActionName());
                            info.put("description", meta.getDescription());
                            return info;
                        }, Collectors.toList())
                ));
        result.put("abilities", grouped);

        return result;
    }

    /**
     * 单独调用指定动作
     */
    @GetMapping("/invoke")
    public ActionResult invoke(
            @RequestParam String actionId,
            @RequestParam(required = false) String args
    ) {
        Map<String, Object> arguments = new HashMap<>();
        if (args != null && !args.isEmpty()) {
            // 简单解析 key=value,key2=value2 格式
            for (String pair : args.split(",")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    arguments.put(kv[0].trim(), kv[1].trim());
                }
            }
        }
        return actionExecutor.execute(actionId, arguments);
    }
}
