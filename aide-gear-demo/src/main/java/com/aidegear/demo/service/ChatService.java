package com.aidegear.demo.service;

import com.aidegear.common.enums.ParamSource;
import com.aidegear.common.model.ActionResult;
import com.aidegear.common.model.AiActionMeta;
import com.aidegear.common.model.AiParamMeta;
import com.aidegear.core.executor.ActionExecutor;
import com.aidegear.core.registry.AiActionRegistry;
import com.alibaba.fastjson2.JSON;
import dev.langchain4j.agent.tool.JsonSchemaProperty;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 对话服务 - 通过 OpenAI 大模型理解用户自然语言问题，
 * 自动识别并调用已注册的 AI 能力，返回最终回答。
 *
 * @author wayne
 * @since 1.0.0
 */
@Slf4j
@Service
public class ChatService {

    @Resource
    private AiActionRegistry registry;

    @Resource
    private ActionExecutor actionExecutor;

    @Value("${aide-gear.openai.api-key:}")
    private String apiKey;

    @Value("${aide-gear.openai.model-name:gpt-4o-mini}")
    private String modelName;

    @Value("${aide-gear.openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    private ChatLanguageModel chatModel;

    @PostConstruct
    public void init() {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("[AideGear] OpenAI API Key 未配置，ChatService 将不可用。请设置 aide-gear.openai.api-key");
            return;
        }
        this.chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl(baseUrl)
                .build();
        log.info("[AideGear] ChatService 初始化完成，模型: {}", modelName);
    }

    /**
     * 处理用户自然语言提问，通过大模型识别意图并调用对应能力
     *
     * @param userQuestion 用户提问
     * @return AI 最终回答
     */
    public String chat(String userQuestion) {
        if (chatModel == null) {
            return "ChatService 未初始化，请配置 aide-gear.openai.api-key";
        }

        // 1. 将已注册的 AI 能力转换为 LangChain4j ToolSpecification
        List<ToolSpecification> toolSpecs = buildToolSpecifications();

        // 2. 构建消息列表
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(
                "你是一个智能助手，可以通过调用工具来帮助用户解决问题。" +
                "请根据用户的问题判断是否需要调用工具，如果需要则调用对应工具，" +
                "然后根据工具返回的结果用自然语言回答用户。如果不需要调用工具，直接回答即可。"
        ));
        messages.add(UserMessage.from(userQuestion));

        log.info("[AideGear] 用户提问: {}", userQuestion);
        log.debug("[AideGear] 可用工具数量: {}", toolSpecs.size());

        // 3. 发送给大模型（携带工具定义）
        Response<AiMessage> response = chatModel.generate(messages, toolSpecs);
        AiMessage aiMessage = response.content();

        // 4. 检查是否需要调用工具
        if (aiMessage.hasToolExecutionRequests()) {
            return handleToolCalls(messages, aiMessage, toolSpecs);
        }

        // 5. 无需调用工具，直接返回回答
        return aiMessage.text();
    }

    /**
     * 处理大模型返回的工具调用请求
     */
    private String handleToolCalls(List<ChatMessage> messages, AiMessage aiMessage,
                                   List<ToolSpecification> toolSpecs) {
        messages.add(aiMessage);

        List<ToolExecutionRequest> toolRequests = aiMessage.toolExecutionRequests();
        for (ToolExecutionRequest toolRequest : toolRequests) {
            String toolName = toolRequest.name();
            String argsJson = toolRequest.arguments();

            log.info("[AideGear] 大模型请求调用工具: {}, 参数: {}", toolName, argsJson);

            // 将工具名映射回 actionId（工具名使用了安全格式化后的名称）
            String actionId = toolNameToActionId(toolName);

            // 解析参数
            Map<String, Object> arguments = new HashMap<>();
            if (argsJson != null && !argsJson.isEmpty()) {
                try {
                    arguments = JSON.parseObject(argsJson, Map.class);
                } catch (Exception e) {
                    log.warn("[AideGear] 解析工具参数失败: {}", argsJson, e);
                }
            }

            // 调用 ActionExecutor 执行
            ActionResult result = actionExecutor.execute(actionId, arguments);
            String resultStr = result.isSuccess() ? result.getData() : "调用失败: " + result.getErrorMessage();

            log.info("[AideGear] 工具执行结果: {}", resultStr);

            // 构建工具执行结果消息
            messages.add(ToolExecutionResultMessage.from(toolRequest, resultStr));
        }

        // 将工具结果发送回大模型，获取最终回答
        Response<AiMessage> finalResponse = chatModel.generate(messages, toolSpecs);
        AiMessage finalMessage = finalResponse.content();

        // 如果大模型再次请求工具调用（多轮工具调用），递归处理
        if (finalMessage.hasToolExecutionRequests()) {
            return handleToolCalls(messages, finalMessage, toolSpecs);
        }

        return finalMessage.text();
    }

    /**
     * 将注册的 AI 能力转换为 LangChain4j ToolSpecification 列表
     */
    private List<ToolSpecification> buildToolSpecifications() {
        List<ToolSpecification> specs = new ArrayList<>();

        for (AiActionMeta meta : registry.getAllActions()) {
            ToolSpecification.Builder builder = ToolSpecification.builder()
                    .name(actionIdToToolName(meta.getActionId()))
                    .description(buildToolDescription(meta));

            // 只添加 AI 可见的参数（CONVERSATION 来源）
            List<AiParamMeta> visibleParams = meta.getAiVisibleParams();
            for (AiParamMeta param : visibleParams) {
                List<JsonSchemaProperty> properties = new ArrayList<>();
                properties.add(getJsonSchemaType(param.getParamType()));
                properties.add(JsonSchemaProperty.description(param.getDescription()));
                builder.addParameter(param.getParamName(), properties.toArray(new JsonSchemaProperty[0]));
            }

            specs.add(builder.build());
        }

        return specs;
    }

    /**
     * 构建工具描述（包含能力分组信息和返回值描述）
     */
    private String buildToolDescription(AiActionMeta meta) {
        StringBuilder desc = new StringBuilder();
        desc.append("[").append(meta.getAbilityName()).append("] ");
        if (meta.getDescription() != null && !meta.getDescription().isEmpty()) {
            desc.append(meta.getDescription());
        } else {
            desc.append(meta.getActionName());
        }
        if (meta.getReturnDesc() != null && !meta.getReturnDesc().isEmpty()) {
            desc.append("。").append(meta.getReturnDesc());
        }
        return desc.toString();
    }

    /**
     * actionId 转工具名（OpenAI function name 不支持中文和特殊字符）
     * 使用下划线替换点号，中文转拼音或使用编码
     */
    private String actionIdToToolName(String actionId) {
        // 简单处理：将点号替换为双下划线，保留中文（OpenAI 支持 UTF-8 function name）
        // 但更安全的做法是使用索引映射
        return actionId.replace(".", "__");
    }

    /**
     * 工具名转回 actionId
     */
    private String toolNameToActionId(String toolName) {
        return toolName.replace("__", ".");
    }

    /**
     * Java 类型映射到 JsonSchemaProperty 类型
     */
    private JsonSchemaProperty getJsonSchemaType(Class<?> type) {
        if (type == String.class) {
            return JsonSchemaProperty.STRING;
        } else if (type == Integer.class || type == int.class) {
            return JsonSchemaProperty.INTEGER;
        } else if (type == Long.class || type == long.class) {
            return JsonSchemaProperty.INTEGER;
        } else if (type == Double.class || type == double.class || type == Float.class || type == float.class) {
            return JsonSchemaProperty.NUMBER;
        } else if (type == Boolean.class || type == boolean.class) {
            return JsonSchemaProperty.BOOLEAN;
        }
        return JsonSchemaProperty.STRING;
    }
}
