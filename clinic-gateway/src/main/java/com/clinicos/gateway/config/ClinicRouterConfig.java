package com.clinicos.gateway.config;

import com.clinicos.gateway.handler.AuditHandler;
import com.clinicos.gateway.handler.BookingHandler;
import com.clinicos.gateway.handler.ClinicHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.PATCH;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.PUT;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/** Clinic-level endpoints served by the gateway itself (it owns the clinic tables). */
@Configuration
public class ClinicRouterConfig {

    @Bean
    public RouterFunction<ServerResponse> clinicRoutes(ClinicHandler clinic, AuditHandler audit, BookingHandler booking) {
        return route(GET("/api/v1/clinic/profile"), clinic::getProfile)
                .andRoute(PUT("/api/v1/clinic/profile"), clinic::saveProfile)
                .andRoute(GET("/api/v1/public/clinics/{slug}"), clinic::publicClinic)
                .andRoute(GET("/api/v1/public/clinics/{slug}/doctors"), booking::publicDoctors)
                .andRoute(GET("/api/v1/public/clinics/{slug}/slots"), booking::publicSlots)
                .andRoute(POST("/api/v1/public/clinics/{slug}/booking-requests"), booking::publicRequest)
                .andRoute(GET("/api/v1/clinic/booking-requests"), booking::list)
                .andRoute(GET("/api/v1/clinic/booking-requests/count"), booking::countNew)
                .andRoute(PATCH("/api/v1/clinic/booking-requests/{id}"), booking::update)
                .andRoute(GET("/api/v1/clinic/audit-log"), audit::search)
                .andRoute(POST("/api/v1/clinic/audit-log/events"), audit::reportClientEvent)
                // JPA calls block: keep them off Netty's event loop
                .filter((request, next) -> Mono.defer(() -> next.handle(request)).subscribeOn(Schedulers.boundedElastic()));
    }
}
