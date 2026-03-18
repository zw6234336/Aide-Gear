package com.aidegear.demo.service;

import com.aidegear.common.annotation.AiAbility;
import com.aidegear.common.annotation.AiAction;
import com.aidegear.common.annotation.AiParam;
import com.aidegear.common.enums.ParamSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 示例：用户服务 - 展示 @AiAbility + @AiAction + @AiParam 的使用方式。
 * <p>
 * 重点演示参数来源隔离机制：userId 从 JWT 注入（AI 不可见），
 * 其他参数由 AI 从对话中提取。
 * </p>
 *
 * @author wayne
 * @since 1.0.0
 */
@Slf4j
@Service
@AiAbility(name = "个人中心服务", description = "提供用户保单查询、个人信息管理等能力")
public class UserService {

    /**
     * 查询我的保单列表
     * <p>
     * userId 由框架从 JWT 中强制注入，AI 无法感知和伪造此参数
     * </p>
     */
    @AiAction(name = "查询我的保单", desc = "获取当前登录用户的所有保单列表",
              returnDesc = "返回保单列表，包含保单号、产品名称、保费等信息")
    public List<Map<String, Object>> getMyPolicies(
            @AiParam(value = "用户ID", source = ParamSource.JWT, jwtKey = "userId") Long userId,
            @AiParam(value = "保单类型", required = false, example = "寿险") String type
    ) {
        log.info("查询用户[{}]的保单, 类型: {}", userId, type);

        // 模拟数据返回
        Map<String, Object> policy = new HashMap<>();
        policy.put("policyNo", "P2025031800001");
        policy.put("productName", "某某终身寿险");
        policy.put("premium", 5000);
        policy.put("status", "有效");
        policy.put("type", type != null ? type : "全部");

        return List.of(policy);
    }

    /**
     * 查询个人信息
     */
    @AiAction(name = "查询个人信息", desc = "获取当前登录用户的基本信息")
    public Map<String, Object> getMyProfile(
            @AiParam(value = "用户ID", source = ParamSource.JWT, jwtKey = "userId") Long userId
    ) {
        log.info("查询用户[{}]的个人信息", userId);

        Map<String, Object> profile = new HashMap<>();
        profile.put("userId", userId);
        profile.put("name", "张三");
        profile.put("phone", "138****8888");
        profile.put("level", "VIP");

        return profile;
    }
}
