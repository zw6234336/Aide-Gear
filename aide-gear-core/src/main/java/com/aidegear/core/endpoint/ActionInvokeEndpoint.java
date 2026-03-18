package com.aidegear.core.endpoint;

import com.aidegear.common.model.ActionResult;
import com.aidegear.core.executor.ActionExecutor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/**
 * AI 动作调用端点 - 接收 AI 的工具调用请求并执行。
 *
 * @author wayne
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/aide-gear")
public class ActionInvokeEndpoint {

    @Resource
    private ActionExecutor actionExecutor;

    /**
     * 执行 AI 动作
     */
    @PostMapping("/action/invoke")
    public ActionResult invoke(@RequestBody @Valid InvokeRequest request) {
        log.info("[AideGear] 收到动作调用请求: {}", request.getActionId());
        return actionExecutor.execute(request.getActionId(), request.getArguments());
    }

    @Data
    public static class InvokeRequest {

        @NotBlank(message = "actionId 不能为空")
        private String actionId;

        /**
         * AI 提供的参数 (仅 CONVERSATION 来源的参数)
         */
        private Map<String, Object> arguments;
    }
}
