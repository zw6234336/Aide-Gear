package com.aidegear.core.scanner;

import com.aidegear.common.annotation.AiAbility;
import com.aidegear.common.annotation.AiAction;
import com.aidegear.common.annotation.AiParam;
import com.aidegear.common.enums.ParamSource;
import com.aidegear.common.model.AiActionMeta;
import com.aidegear.common.model.AiParamMeta;
import com.aidegear.core.registry.AiActionRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import jakarta.annotation.Resource;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 能力扫描器 - 基于 BeanPostProcessor 在 Spring 容器初始化时自动扫描。
 * <p>
 * 扫描所有标记了 {@code @AiAbility} 的 Bean，提取其中标记了 {@code @AiAction} 的方法，
 * 构建元数据并注册到 {@link AiActionRegistry}。
 * </p>
 *
 * @author wayne
 * @since 1.0.0
 */
@Slf4j
@Component
public class AiAbilityScanner implements BeanPostProcessor {

    @Resource
    private AiActionRegistry registry;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();

        // 处理 CGLIB 代理类，获取原始类
        if (beanClass.getName().contains("$$")) {
            beanClass = beanClass.getSuperclass();
        }

        AiAbility ability = AnnotationUtils.findAnnotation(beanClass, AiAbility.class);
        if (ability == null) {
            return bean;
        }

        log.info("[AideGear] 发现 AI 能力: {} ({})", ability.name(), beanClass.getSimpleName());

        // 扫描所有标记了 @AiAction 的方法
        Method[] methods = ReflectionUtils.getDeclaredMethods(beanClass);
        for (Method method : methods) {
            AiAction action = AnnotationUtils.findAnnotation(method, AiAction.class);
            if (action == null) {
                continue;
            }

            AiActionMeta meta = buildActionMeta(bean, beanName, ability, action, method);
            registry.register(meta);
        }

        return bean;
    }

    /**
     * 构建动作元数据
     */
    private AiActionMeta buildActionMeta(Object bean, String beanName,
                                          AiAbility ability, AiAction action, Method method) {
        // 构建参数元数据
        List<AiParamMeta> paramMetas = buildParamMetas(method);

        String actionId = ability.name() + "." + action.name();

        return AiActionMeta.builder()
                .actionId(actionId)
                .actionName(action.name())
                .description(action.desc())
                .returnDesc(action.returnDesc())
                .abilityName(ability.name())
                .abilityDescription(ability.description())
                .beanName(beanName)
                .targetBean(bean)
                .targetMethod(method)
                .paramMetas(paramMetas)
                .build();
    }

    /**
     * 构建方法的参数元数据列表
     */
    private List<AiParamMeta> buildParamMetas(Method method) {
        Parameter[] parameters = method.getParameters();
        List<AiParamMeta> paramMetas = new ArrayList<>();

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            AiParam aiParam = param.getAnnotation(AiParam.class);

            AiParamMeta meta;
            if (aiParam != null) {
                meta = AiParamMeta.builder()
                        .paramName(param.getName())
                        .description(aiParam.value())
                        .paramType(param.getType())
                        .source(aiParam.source())
                        .required(aiParam.required())
                        .jwtKey(aiParam.jwtKey())
                        .systemKey(aiParam.systemKey())
                        .example(aiParam.example())
                        .paramIndex(i)
                        .build();
            } else {
                // 未标注 @AiParam 的参数，默认按 CONVERSATION 处理
                meta = AiParamMeta.builder()
                        .paramName(param.getName())
                        .description(param.getName())
                        .paramType(param.getType())
                        .source(ParamSource.CONVERSATION)
                        .required(true)
                        .paramIndex(i)
                        .build();
            }

            paramMetas.add(meta);
        }

        return paramMetas;
    }
}
