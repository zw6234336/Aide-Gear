package com.aidegear.starter.config;

import com.aidegear.core.security.DefaultJwtParamResolver;
import com.aidegear.core.security.DefaultSystemParamResolver;
import com.aidegear.core.security.JwtParamResolver;
import com.aidegear.core.security.SystemParamResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * Aide-Gear 自动配置类。
 * <p>
 * 引入 aide-gear-spring-boot-starter 后自动生效，注册核心组件。
 * </p>
 *
 * @author wayne
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(AideGearProperties.class)
@ComponentScan(basePackages = {
        "com.aidegear.core.registry",
        "com.aidegear.core.scanner",
        "com.aidegear.core.executor",
        "com.aidegear.core.endpoint"
})
public class AideGearAutoConfiguration {

    /**
     * 默认 JWT 参数解析器（当用户未提供自定义实现时）
     */
    @Bean("jwtParamResolver")
    @ConditionalOnMissingBean(JwtParamResolver.class)
    public JwtParamResolver defaultJwtParamResolver() {
        log.info("[AideGear] 使用默认 JWT 参数解析器");
        return new DefaultJwtParamResolver();
    }

    /**
     * 默认系统参数解析器（当用户未提供自定义实现时）
     */
    @Bean("systemParamResolver")
    @ConditionalOnMissingBean(SystemParamResolver.class)
    public SystemParamResolver defaultSystemParamResolver() {
        log.info("[AideGear] 使用默认 System 参数解析器");
        return new DefaultSystemParamResolver();
    }
}
