package com.aidegear.demo;

import com.aidegear.common.model.ActionResult;
import com.aidegear.common.model.AiActionMeta;
import com.aidegear.core.executor.ActionExecutor;
import com.aidegear.core.registry.AiActionRegistry;
import com.aidegear.demo.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * ChatService 集成测试。
 * <p>
 * 测试 AI 能力注册、扫描、执行的完整链路。
 * OpenAI 相关测试需要配置 OPENAI_API_KEY 环境变量。
 * </p>
 *
 * @author wayne
 * @since 1.0.0
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("dev")
class ChatServiceTest {

    @Resource
    private AiActionRegistry registry;

    @Resource
    private ActionExecutor actionExecutor;

    @Resource
    private ChatService chatService;

    /**
     * 测试：AI 能力是否正确扫描注册
     */
    @Test
    void testAbilitiesRegistered() {
        assertTrue(registry.size() > 0, "应至少注册一个 AI 能力");
        log.info("已注册的 AI 能力数量: {}", registry.size());

        // 验证具体能力是否注册
        assertNotNull(registry.getAction("个人中心服务.查询我的保单"), "应注册 '查询我的保单' 能力");
        assertNotNull(registry.getAction("个人中心服务.查询个人信息"), "应注册 '查询个人信息' 能力");
        assertNotNull(registry.getAction("产品查询服务.查询产品详情"), "应注册 '查询产品详情' 能力");
        assertNotNull(registry.getAction("产品查询服务.产品对比"), "应注册 '产品对比' 能力");
    }

    /**
     * 测试：AI 可见参数过滤
     */
    @Test
    void testAiVisibleParams() {
        AiActionMeta meta = registry.getAction("个人中心服务.查询我的保单");
        assertNotNull(meta);

        // 全部参数应为 2 个（userId + type）
        assertEquals(2, meta.getParamMetas().size(), "应有 2 个参数");

        // AI 可见参数应为 1 个（仅 type，userId 为 JWT 参数不可见）
        assertEquals(1, meta.getAiVisibleParams().size(), "AI 可见参数应为 1 个");
        assertEquals("type", meta.getAiVisibleParams().get(0).getParamName(), "可见参数应为 type");
    }

    /**
     * 测试：直接调用 ActionExecutor 执行产品查询
     */
    @Test
    void testDirectActionExecution() {
        ActionResult result = actionExecutor.execute("产品查询服务.查询产品详情",
                Map.of("riskCode", "RISK001"));

        assertTrue(result.isSuccess(), "执行应成功");
        assertNotNull(result.getData(), "应返回数据");
        log.info("产品查询结果: {}", result.getData());
    }

    /**
     * 测试：直接调用 ActionExecutor 执行产品对比
     */
    @Test
    void testProductComparison() {
        ActionResult result = actionExecutor.execute("产品查询服务.产品对比",
                Map.of("riskCodeA", "RISK001", "riskCodeB", "RISK002"));

        assertTrue(result.isSuccess(), "执行应成功");
        assertNotNull(result.getData(), "应返回数据");
        log.info("产品对比结果: {}", result.getData());
    }

    /**
     * 测试：JWT 参数隔离 - 查询保单（userId 自动注入）
     */
    @Test
    void testJwtParamInjection() {
        ActionResult result = actionExecutor.execute("个人中心服务.查询我的保单",
                Map.of("type", "寿险"));

        assertTrue(result.isSuccess(), "执行应成功");
        assertNotNull(result.getData(), "应返回数据");
        log.info("保单查询结果: {}", result.getData());
    }

    /**
     * 测试：调用不存在的 actionId
     */
    @Test
    void testActionNotFound() {
        ActionResult result = actionExecutor.execute("不存在的服务.不存在的能力",
                Map.of());

        assertFalse(result.isSuccess(), "应执行失败");
        assertNotNull(result.getErrorMessage(), "应有错误信息");
        log.info("预期的错误: {}", result.getErrorMessage());
    }

    /**
     * 测试：ChatService 初始化状态
     */
    @Test
    void testChatServiceInitialized() {
        assertNotNull(chatService, "ChatService 应被正确注入");
    }

    // ─── 新增：AiActionRegistry 过滤方法测试 ───────────────────────────────

    /**
     * 测试：按能力分组获取 Action 列表
     */
    @Test
    void testGetActionsByAbility() {
        List<AiActionMeta> actions = registry.getActionsByAbility("产品查询服务");
        assertNotNull(actions, "应返回非空列表");
        assertFalse(actions.isEmpty(), "产品查询服务应有至少一个 Action");
        actions.forEach(a -> assertEquals("产品查询服务", a.getAbilityName()));
        log.info("产品查询服务的 Action 数量: {}", actions.size());
    }

    /**
     * 测试：获取所有已注册的 abilityName 集合
     */
    @Test
    void testGetAbilityNames() {
        Set<String> names = registry.getAbilityNames();
        assertNotNull(names);
        assertFalse(names.isEmpty());
        assertTrue(names.contains("产品查询服务"));
        assertTrue(names.contains("个人中心服务"));
        log.info("已注册 Ability 名称: {}", names);
    }

    /**
     * 测试：获取 ability 摘要 Map（用于构建 LLM 提示词）
     */
    @Test
    void testGetAbilitySummaries() {
        Map<String, String> summaries = registry.getAbilitySummaries();
        assertNotNull(summaries);
        assertFalse(summaries.isEmpty());
        assertTrue(summaries.containsKey("产品查询服务"));
        log.info("Ability 摘要: {}", summaries);
    }

    /**
     * 测试：按多个 abilityName 批量查询 Action
     */
    @Test
    void testGetActionsByAbilities() {
        List<AiActionMeta> actions = registry.getActionsByAbilities(
                List.of("产品查询服务", "个人中心服务"));
        assertNotNull(actions);
        assertTrue(actions.size() >= 2, "应包含两个分组的 Action");
        log.info("两个分组共返回 {} 个 Action", actions.size());
    }

    // ─── LLM 对话测试（需要配置 OpenAI API Key）──────────────────────────────

    /**
     * 测试：通过 AbstractAideGearChat 自动编排对话 - 产品查询
     * <p>需要配置 aide-gear.openai.api-key，否则跳过。</p>
     */
    @Test
    void testChatWithProductQuery() {
        assumeTrue(chatService.isLlmReady(), "跳过：未配置 OpenAI API Key");

        List<ActionResult> results = chatService.chat("帮我查一下产品编码为 RISK001 的产品详情");
        assertNotNull(results, "应返回结果列表");
        assertFalse(results.isEmpty(), "应有执行结果");
        log.info("产品查询 results: {}", results);
    }

    /**
     * 测试：通过 AbstractAideGearChat 自动编排对话 - 产品对比
     */
    @Test
    void testChatWithProductComparison() {
        assumeTrue(chatService.isLlmReady(), "跳过：未配置 OpenAI API Key");

        List<ActionResult> results = chatService.chat("帮我对比一下 RISK001 和 RISK002 两个产品");
        assertNotNull(results, "应返回结果列表");
        assertFalse(results.isEmpty(), "应有执行结果");
        log.info("产品对比 results: {}", results);
    }

    /**
     * 测试：通过 AbstractAideGearChat 自动编排对话 - 查询个人保单
     */
    @Test
    void testChatWithPolicyQuery() {
        assumeTrue(chatService.isLlmReady(), "跳过：未配置 OpenAI API Key");

        List<ActionResult> results = chatService.chat("查一下我的寿险保单");
        assertNotNull(results, "应返回结果列表");
        log.info("保单查询 results: {}", results);
    }
}
