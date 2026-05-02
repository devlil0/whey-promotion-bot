package com.devlil0.whey_promotion_bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class WheyPromotionBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(WheyPromotionBotApplication.class, args);
    }
}
