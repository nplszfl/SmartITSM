package com.smartitsm.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.smartitsm.ai.config.DeepSeekProperties;

@SpringBootApplication
@EnableConfigurationProperties(DeepSeekProperties.class)
public class SmartItsmAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartItsmAiApplication.class, args);
    }
}