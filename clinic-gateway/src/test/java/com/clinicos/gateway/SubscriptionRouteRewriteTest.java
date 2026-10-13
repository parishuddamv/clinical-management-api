package com.clinicos.gateway;

import com.clinicos.gateway.config.GatewayConfig;
import com.clinicos.gateway.config.JwtGatewayFilterFactory;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.security.reactive.ReactiveManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@SpringBootTest(
        classes = SubscriptionRouteRewriteTest.Config.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.main.web-application-type=reactive",
                "spring.cloud.gateway.enabled=true"
        }
)
@AutoConfigureWebTestClient
class SubscriptionRouteRewriteTest {

    private static HttpServer downstream;
    private static int downstreamPort;
    private static final AtomicReference<String> lastPath = new AtomicReference<>();

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            FlywayAutoConfiguration.class,
            ReactiveManagementWebSecurityAutoConfiguration.class,
            ReactiveSecurityAutoConfiguration.class,
            ReactiveUserDetailsServiceAutoConfiguration.class
    })
    @Import({GatewayConfig.class, TestBeans.class})
    static class Config {
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        JwtGatewayFilterFactory jwtGatewayFilterFactory() {
            return new JwtGatewayFilterFactory(mock(com.clinicos.common.security.JwtTokenProvider.class),
                    mock(com.clinicos.common.security.RegistrationAccess.class)) {
                @Override
                public org.springframework.cloud.gateway.filter.GatewayFilter apply(Config config) {
                    return (exchange, chain) -> chain.filter(exchange);
                }
            };
        }
    }

    @BeforeAll
    static void startDownstream() throws IOException {
        downstream = HttpServer.create(new InetSocketAddress(0), 0);
        downstream.createContext("/", SubscriptionRouteRewriteTest::handle);
        downstream.start();
        downstreamPort = downstream.getAddress().getPort();
    }

    @AfterAll
    static void stopDownstream() {
        if (downstream != null) {
            downstream.stop(0);
        }
    }

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("service.subscription.url", () -> "http://localhost:" + downstreamPort);
    }

    @Autowired
    WebTestClient http;

    @Test
    void rewritesSubscriptionMePathForDownstreamService() {
        http.get()
                .uri("/api/v1/subscriptions/me")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.path").isEqualTo("/subscriptions/me");

        assertThat(lastPath.get()).isEqualTo("/subscriptions/me");
    }

    @Test
    void preservesQueryStringWhileRewritingSubscriptionPath() {
        http.get()
                .uri("/api/v1/subscriptions/plans?billing=annual")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.path").isEqualTo("/subscriptions/plans")
                .jsonPath("$.query").isEqualTo("billing=annual");

        assertThat(lastPath.get()).isEqualTo("/subscriptions/plans");
    }

    private static void handle(HttpExchange exchange) throws IOException {
        lastPath.set(exchange.getRequestURI().getPath());
        String query = exchange.getRequestURI().getRawQuery();
        String body = "{\"path\":\"" + exchange.getRequestURI().getPath()
                + "\",\"query\":" + (query == null ? "null" : "\"" + UriUtils.decode(query, StandardCharsets.UTF_8) + "\"") + "}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}

