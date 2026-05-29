package com.smartitsm.classification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.smartitsm.classification", "com.smartitsm.common"})
public class SmartTicketClassificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartTicketClassificationApplication.class, args);
    }
}