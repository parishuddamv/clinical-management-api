package com.clinicos.gateway.config;

import com.clinicos.gateway.handler.AuthHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * Reactive router configuration for authentication endpoints
 */
@Configuration
public class AuthRouterConfig {

    @Bean
    public RouterFunction<ServerResponse> authRoutes(AuthHandler authHandler) {
        return route(GET("/api/v1/auth/google"), authHandler::getGoogleConfig)
                .andRoute(GET("/api/v1/auth/google/config"), authHandler::getGoogleConfig)
                .andRoute(POST("/api/v1/auth/google"), authHandler::authenticateWithGoogle)
                .andRoute(POST("/api/v1/auth/register"), authHandler::registerUser)
                .andRoute(GET("/api/v1/auth/user-status/{email}"), authHandler::getUserStatus)
                .andRoute(GET("/api/v1/auth/check-approval/{email}"), authHandler::checkApproval)
                .andRoute(GET("/api/v1/auth/user/{email}"), authHandler::getUser)
                .andRoute(PUT("/api/v1/auth/admin/approve/{email}"), authHandler::approveUser)
                .andRoute(PUT("/api/v1/auth/admin/reject/{email}"), authHandler::rejectUser)
                .andRoute(PUT("/api/v1/auth/admin/suspend/{email}"), authHandler::suspendUser);
    }
}

