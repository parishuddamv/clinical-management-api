package com.clinicos.billing.subscription.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.subscription")
public class SubscriptionProperties {

    private int trialDays = 30;
    private int graceDays = 7;
}

