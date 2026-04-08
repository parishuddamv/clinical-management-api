package com.clinicos.common.config;

import com.clinicos.common.listener.AuditListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Common application configuration for all modules
 */
@Configuration
public class CommonConfig {

    @Bean
    public AuditListener auditListener() {
        return new AuditListener();
    }

    @Bean
    public OncePerRequestFilter tenantContextClearFilter() {
        return new TenantContextClearFilter();
    }
}

