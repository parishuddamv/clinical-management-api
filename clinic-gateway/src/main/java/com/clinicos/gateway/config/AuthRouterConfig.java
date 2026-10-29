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
                .andRoute(POST("/api/v1/auth/login"), authHandler::loginWithPassword)
                .andRoute(POST("/api/v1/auth/forgot-password"), authHandler::forgotPassword)
                .andRoute(POST("/api/v1/auth/reset-password"), authHandler::resetPassword)
                .andRoute(GET("/api/v1/auth/user-status/{email}"), authHandler::getUserStatus)
                .andRoute(GET("/api/v1/auth/check-approval/{email}"), authHandler::checkApproval)
                .andRoute(GET("/api/v1/auth/user/{email}"), authHandler::getUser)
                .andRoute(PUT("/api/v1/auth/admin/approve/{email}"), authHandler::approveUser)
                .andRoute(PUT("/api/v1/auth/admin/reject/{email}"), authHandler::rejectUser)
                .andRoute(PUT("/api/v1/auth/admin/suspend/{email}"), authHandler::suspendUser)
                .andRoute(GET("/api/v1/auth/admin/registrations"), authHandler::registrations)
                .andRoute(GET("/api/v1/admin/users/pending"), authHandler::pendingUsers)
                .andRoute(GET("/api/v1/admin/users/all"), authHandler::registrations)
                .andRoute(GET("/api/v1/admin/demo-bookings/pending"), authHandler::pendingDemoBookings)
                .andRoute(GET("/api/v1/admin/demo-bookings/confirmed"), authHandler::confirmedDemoBookings)
                .andRoute(GET("/api/v1/admin/demo-bookings/date/{date}"), authHandler::demoBookingsByDate)
                .andRoute(GET("/api/v1/admin/demo-bookings/{id}"), authHandler::getDemoBooking)
                .andRoute(POST("/api/v1/admin/demo-bookings/{id}/confirm"), authHandler::confirmDemoBooking)
                .andRoute(POST("/api/v1/admin/demo-bookings/{id}/cancel"), authHandler::cancelDemoBooking)
                .andRoute(GET("/api/v1/auth/admin/registrations/{email}/history"), authHandler::history)
                .andRoute(PUT("/api/v1/auth/admin/reactivate/{email}"), authHandler::reactivateUser)
                .andRoute(PUT("/api/v1/auth/admin/review/{email}"), authHandler::submitForReview)
                // Existing handlers perform blocking JPA operations; never execute them on Netty's event loop.
                .filter((request, next) -> reactor.core.publisher.Mono.defer(() -> next.handle(request))
                        .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic()));
    }
}

