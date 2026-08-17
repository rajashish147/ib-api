package com.ibtrader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.ibtrader")
@EnableScheduling
public class IbTraderApplication {

    public static void main(String[] args) {
        SpringApplication.run(IbTraderApplication.class, args);
    }
}
