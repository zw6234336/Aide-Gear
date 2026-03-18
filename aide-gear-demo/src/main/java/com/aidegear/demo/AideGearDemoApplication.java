package com.aidegear.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Aide-Gear 演示应用主入口。
 *
 * @author wayne
 * @since 1.0.0
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.aidegear.demo.client.feign")
public class AideGearDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(AideGearDemoApplication.class, args);
    }
}
