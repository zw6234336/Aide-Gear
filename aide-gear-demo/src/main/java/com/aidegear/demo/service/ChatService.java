package com.aidegear.demo.service;

import com.aidegear.core.chat.AbstractAideGearChat;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

/**
 * AI 对话服务 —— 继承 {@link AbstractAideGearChat}，接入具体的大模型。
 *
 * <p>框架负责：分类识别提示词构建 → Action 选择+参数提取提示词构建 → 业务方法执行。
 * 本类只需提供实际的 LLM 调用实现（{@link #callLlm}）。
 *
 * @author wayne
 * @since 1.0.0
 */
@Slf4j
@Service
public class ChatService extends AbstractAideGearChat {

    @Value("${aide-gear.openai.api-key:}")
    private String apiKey;

    @Value("${aide-gear.openai.model-name:gpt-4o-mini}")
    private String modelName;

    @Value("${aide-gear.openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    private ChatLanguageModel chatModel;

    @PostConstruct
    public void init() {
        if (apiKey == null || apiKey.isBlank()) {
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
     * LLM 是否已就绪（API Key 已配置且初始化成功）。
     */
    public boolean isLlmReady() {
        return chatModel != null;
    }

    /**
     * 封装 LLM 调用 —— 框架在编排流程中（分类识别、Action 选择）统一调用此方法。
     *
     * @param prompt 框架构建好的完整提示词
     * @return LLM 返回的文本内容
     */
    @Override
    protected String callLlm(String prompt) {
        if (chatModel == null) {
            throw new IllegalStateException("ChatService 未初始化，请配置 aide-gear.openai.api-key");
        }
        return chatModel.generate(UserMessage.from(prompt)).content().text();
    }
}
