package com.clinicos.common.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@EnableConfigurationProperties(HikariDataSourceConfig.HikariProperties.class)
public class HikariDataSourceConfig {

    @Bean
    public DataSource dataSource(HikariProperties hikariProperties) {

        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(hikariProperties.getUrl());
        config.setUsername(hikariProperties.getUsername());
        config.setPassword(hikariProperties.getPassword());

        /*
         * IMPORTANT:
         * Cloud SQL currently has max_connections = 25.
         *
         * Keep each service small so that multiple ClinicOS
         * microservices can share the database safely.
         */
        config.setMaximumPoolSize(hikariProperties.getMaximumPoolSize());
        config.setMinimumIdle(hikariProperties.getMinimumIdle());

        // Connection timeout
        config.setConnectionTimeout(20000);

        // Remove idle connections after 5 minutes
        config.setIdleTimeout(300000);

        // Recycle connections after 10 minutes
        config.setMaxLifetime(600000);

        // Validation timeout
        config.setValidationTimeout(5000);

        // Disable leak detection in production unless troubleshooting
        config.setLeakDetectionThreshold(0);

        config.setAutoCommit(true);

        // PostgreSQL
        config.setDriverClassName("org.postgresql.Driver");

        // Prepared statement caching
        config.addDataSourceProperty(
                "cachePreparedStatements", "true");

        config.addDataSourceProperty(
                "preparedStatementCacheSize", "250");

        config.addDataSourceProperty(
                "preparedStatementCacheSqlLimit", "2048");

        // TCP keepalive
        config.addDataSourceProperty(
                "tcpKeepAlives", "true");

        // PostgreSQL connection timeout
        config.addDataSourceProperty(
                "connectTimeout", "20");

        return new HikariDataSource(config);
    }

    @ConfigurationProperties(prefix = "db")
    public static class HikariProperties {

        private String url;
        private String username;
        private String password;

        /*
         * IMPORTANT:
         * Defaults are intentionally small because Cloud SQL
         * currently allows only 25 connections.
         */
        private int maximumPoolSize = 3;
        private int minimumIdle = 1;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public int getMaximumPoolSize() {
            return maximumPoolSize;
        }

        public void setMaximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
        }

        public int getMinimumIdle() {
            return minimumIdle;
        }

        public void setMinimumIdle(int minimumIdle) {
            this.minimumIdle = minimumIdle;
        }
    }
}