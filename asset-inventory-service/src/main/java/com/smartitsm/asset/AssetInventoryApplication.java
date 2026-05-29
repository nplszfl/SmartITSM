package com.smartitsm.asset;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {"com.smartitsm.asset", "com.smartitsm.common"})
public class AssetInventoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssetInventoryApplication.class, args);
    }
}