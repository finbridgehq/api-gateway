package com.liquilabs.vankoo.gateway;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import reactor.core.publisher.Mono;

@SpringBootTest(properties = {
        "SPRING_PROFILES_ACTIVE=dev",
        "JWT_SECRET=01234567890123456789012345678901",
        "eureka.client.enabled=false",
        "eureka.client.register-with-eureka=false",
        "eureka.client.fetch-registry=false"
})
class RouteDefinitionTests {

    @Autowired
    private RouteLocator routeLocator;

    @Test
    void riskAndNotificationRoutesAreExposed() {
        List<Route> routes = routeLocator.getRoutes().collectList().block(Duration.ofSeconds(10));
        assertNotNull(routes);

        Route risk = find(routes, "risk-mype-assessments-protected");
        Route notification = find(routes, "notification-api-protected");

        assertTrue(matches(risk, "/risk/api/v1/mype/auctions/1"));
        assertFalse(matches(risk, "/risk/api/v1/risk-assessments/1"));
        assertFalse(matches(risk, "/notification/api/v1/device-registrations"));
        assertTrue(matches(notification, "/notification/api/v1/device-registrations"));
    }

    private static boolean matches(Route route, String path) {
        Boolean accepted = Mono.from(route.getPredicate().apply(exchange(path))).block(Duration.ofSeconds(5));
        return Boolean.TRUE.equals(accepted);
    }

    private static Route find(List<Route> routes, String id) {
        return routes.stream()
                .filter(route -> id.equals(route.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Falta la ruta " + id));
    }

    private static MockServerWebExchange exchange(String path) {
        return MockServerWebExchange.from(MockServerHttpRequest.get(path).build());
    }
}
