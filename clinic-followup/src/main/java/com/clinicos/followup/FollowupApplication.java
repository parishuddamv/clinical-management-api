package com.clinicos.followup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.clinicos.followup", "com.clinicos.common"})
public class FollowupApplication {

    public static void main(String[] args) {
        SpringApplication.run(FollowupApplication.class, args);
    }
}

