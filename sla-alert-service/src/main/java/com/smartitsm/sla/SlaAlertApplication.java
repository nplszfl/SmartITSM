package com.smartitsm.sla;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {"com.smartitsm.sla", "com.smartitsm.common"})
public class SlaAlertApplication {

    public static void main(String[] args) {
        SpringApplication.run(SlaAlertApplication.class, args);
    }
}