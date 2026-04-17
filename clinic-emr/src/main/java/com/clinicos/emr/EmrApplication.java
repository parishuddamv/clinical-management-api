package com.clinicos.emr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.clinicos.emr", "com.clinicos.common"})
public class EmrApplication {
    public static void main(String[] args) {
        SpringApplication.run(EmrApplication.class, args);
    }
}

