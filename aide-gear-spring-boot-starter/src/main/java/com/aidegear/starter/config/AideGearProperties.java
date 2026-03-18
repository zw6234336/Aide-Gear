package com.aidegear.starter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Aide-Gear 配置属性。
 *
 * @author wayne
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "aide-gear")
public class AideGearProperties {

    /**
     * 是否启用 Aide-Gear
     */
    private boolean enabled = true;

    /**
     * 能力暴露端点的基础路径
     */
    private String basePath = "/aide-gear";

    /**
     * JWT 配置
     */
    private Jwt jwt = new Jwt();

    /**
     * 扫描配置
     */
    private Scanner scanner = new Scanner();

    @Data
    public static class Jwt {
        /**
         * JWT Header 名称
         */
        private String headerName = "Authorization";

        /**
         * JWT Token 前缀
         */
        private String tokenPrefix = "Bearer ";

        /**
         * JWT 密钥（签名验证用）
         */
        private String secret;
    }

    @Data
    public static class Scanner {
        /**
         * 扫描包路径（逗号分隔，为空则扫描全部）
         */
        private String basePackages;
    }
}
