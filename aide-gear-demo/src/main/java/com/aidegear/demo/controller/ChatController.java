package com.aidegear.demo.controller;

import com.aidegear.common.model.ActionResult;
import com.aidegear.demo.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 对话控制器 - 提供自然语言对话接口，通过 OpenAI 大模型智能调用已注册的 AI 能力。
 * <p>
 * 用户通过自然语言提问，大模型自动识别意图并调用对应的 AiAction，
 * 将执行结果转化为自然语言回复。
 * </p>
 *
 * @author wayne
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/demo")
public class ChatController {

    @Resource
    private ChatService chatService;

    /**
     * GET 方式对话（简单测试用）
     *
     * @param question 用户问题
     * @return AI 回答
     */
    @GetMapping("/chat")
    public Map<String, Object> chatGet(@RequestParam String question) {
        return doChat(question);
    }

    /**
     * POST 方式对话
     *
     * @param request 包含 question 字段的请求体
     * @return AI 回答
     */
    @PostMapping("/chat")
    public Map<String, Object> chatPost(@RequestBody ChatRequest request) {
        return doChat(request.getQuestion());
    }

    private Map<String, Object> doChat(String question) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("question", question);

        try {
            List<ActionResult> results = chatService.chat(question);
            result.put("results", results);
            result.put("success", true);
        } catch (Exception e) {
            log.error("[AideGear] 对话处理失败", e);
            result.put("results", List.of());
            result.put("success", false);
            result.put("errorMessage", e.getMessage());
        }

        return result;
    }

    @lombok.Data
    public static class ChatRequest {
        private String question;
    }
}
