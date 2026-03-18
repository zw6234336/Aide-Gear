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

import static org.junit.jupiter.api.Assertions.*;

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
                java.util.Map.of("riskCode", "RISK001"));

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
                java.util.Map.of("riskCodeA", "RISK001", "riskCodeB", "RISK002"));

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
                java.util.Map.of("type", "寿险"));

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
                java.util.Map.of());

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

    /**
     * 测试：OpenAI 对话 - 查询产品详情
     * <p>
     * 注意：此测试需要配置 OPENAI_API_KEY 环境变量。
     * 未配置时会返回提示信息而非抛出异常。
     * </p>
     */
    @Test
    void testChatWithProductQuery() {
        String answer = chatService.chat("帮我查一下产品编码为 RISK001 的产品详情");
        assertNotNull(answer, "应返回回答");
        log.info("AI 回答（产品查询）: {}", answer);
    }

    /**
     * 测试：OpenAI 对话 - 产品对比
     */
    @Test
    void testChatWithProductComparison() {
        String answer = chatService.chat("帮我对比一下 RISK001 和 RISK002 两个产品");
        assertNotNull(answer, "应返回回答");
        log.info("AI 回答（产品对比）: {}", answer);
    }

    /**
     * 测试：OpenAI 对话 - 不需要工具调用的普通问题
     */
    @Test
    void testChatWithGeneralQuestion() {
        String answer = chatService.chat("你好，你是谁？");
        assertNotNull(answer, "应返回回答");
        log.info("AI 回答（普通问题）: {}", answer);
    }

    /**
     * 测试：OpenAI 对话 - 查询个人保单
     */
    @Test
    void testChatWithPolicyQuery() {
        String answer = chatService.chat("查一下我的寿险保单");
        assertNotNull(answer, "应返回回答");
        log.info("AI 回答（保单查询）: {}", answer);
    }
}
