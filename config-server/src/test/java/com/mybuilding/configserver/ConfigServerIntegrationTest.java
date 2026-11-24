package com.mybuilding.configserver;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConfigServerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @SuppressWarnings("rawtypes")
    void shouldServeExternalizedConfigurationsForAllServicesAndGatewayProfiles() {
        List<String> targets = List.of(
                "/service-residents/default",
                "/service-incidents/default",
                "/service-finances/default",
                "/service-notifications/default",
                "/api-gateway/default",
                "/api-gateway/statique"
        );

        for (String path : targets) {
            ResponseEntity<Map> response = restTemplate.getForEntity(path, Map.class);
            assertEquals(HttpStatus.OK, response.getStatusCode(), "Echec Config Server sur " + path);
            assertNotNull(response.getBody());
            List<?> sources = (List<?>) response.getBody().get("propertySources");
            assertNotNull(sources, "propertySources nul pour " + path);
            assertFalse(sources.isEmpty(), "Aucune config chargée depuis config-repo pour " + path);
        }
    }
}
