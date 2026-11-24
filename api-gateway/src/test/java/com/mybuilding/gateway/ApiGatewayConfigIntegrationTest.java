package com.mybuilding.gateway;

import com.mybuilding.gateway.filter.RateLimitProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
@TestPropertySource(locations = {
        "file:../config-repo/api-gateway.properties"
})
class ApiGatewayConfigIntegrationTest {

    @Autowired
    private RouteDefinitionLocator routeDefinitionLocator;

    @Autowired
    private RateLimitProperties rateLimitProperties;

    @Test
    void shouldLoadGatewayRoutesAndRateLimiterFromConfigRepo() {
        List<RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions().collectList().block();
        assertNotNull(routes);
        assertTrue(routes.stream().anyMatch(r -> "route-residents".equals(r.getId())));
        assertTrue(routes.stream().anyMatch(r -> "route-incidents".equals(r.getId())));
        assertTrue(routes.stream().anyMatch(r -> "route-finances".equals(r.getId())));
        assertTrue(routes.stream().anyMatch(r -> "route-notifications".equals(r.getId())));

        assertTrue(rateLimitProperties.isEnabled());
        assertEquals(20, rateLimitProperties.getCapacity());
        assertEquals(15, rateLimitProperties.getRefillSeconds());
    }
}
