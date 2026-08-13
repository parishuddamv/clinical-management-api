package com.clinicos.common.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * HikariCP Connection Pool Configuration for optimal database performance.
 * Provides high-speed connection pooling for database operations.
 */
@Configuration
@EnableConfigurationProperties(HikariDataSourceConfig.HikariProperties.class)
public class HikariDataSourceConfig {

    /**
     * Configure HikariCP with performance-optimized settings
     */
    @Bean
    public DataSource dataSource(HikariProperties hikariProperties) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(hikariProperties.getUrl());
        config.setUsername(hikariProperties.getUsername());
        config.setPassword(hikariProperties.getPassword());

        // Connection Pool Settings - Optimized for High Performance
        // Maximum connections to maintain in the pool
        config.setMaximumPoolSize(hikariProperties.getMaximumPoolSize());
        // Minimum idle connections
        config.setMinimumIdle(hikariProperties.getMinimumIdle());
        // Connection timeout (30 seconds)
        config.setConnectionTimeout(30000);
        // Idle timeout (10 minutes)
        config.setIdleTimeout(600000);
        // Max lifetime (30 minutes)
        config.setMaxLifetime(1800000);
        // Validation query timeout
        config.setValidationTimeout(5000);
        // Connection test - HikariCP uses connection validation through health checks
        config.setLeakDetectionThreshold(60000);

        // Performance Settings
        // Thread pool size for statement execution
        config.setThreadFactory(Thread::new);
        // Auto-commit set to true for connection safety
        config.setAutoCommit(true);

        // PostgreSQL Specific Settings
        config.setDriverClassName("org.postgresql.Driver");
        // Enable prepared statement caching
        config.addDataSourceProperty("cachePreparedStatements", "true");
        config.addDataSourceProperty("preparedStatementCacheSize", "250");
        config.addDataSourceProperty("preparedStatementCacheSqlLimit", "2048");
        // Enable TCP keepalive
        config.addDataSourceProperty("tcpKeepAlives", "true");
        // Connection retry attempts
        config.addDataSourceProperty("connectTimeout", "30");

        return new HikariDataSource(config);
    }

    /**
     * HikariCP Configuration Properties
     * Binds to environment variables: DB_URL, DB_USERNAME, DB_PASSWORD, etc.
     */
    @ConfigurationProperties(prefix = "db")
    public static class HikariProperties {
        private String url;
        private String username;
        private String password;
        private int maximumPoolSize = 15;
        private int minimumIdle = 5;

        // Getters and Setters
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public int getMaximumPoolSize() { return maximumPoolSize; }
        public void setMaximumPoolSize(int maximumPoolSize) { this.maximumPoolSize = maximumPoolSize; }
        public int getMinimumIdle() { return minimumIdle; }
        public void setMinimumIdle(int minimumIdle) { this.minimumIdle = minimumIdle; }
    }
}

