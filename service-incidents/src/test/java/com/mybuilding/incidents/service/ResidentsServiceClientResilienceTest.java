package com.mybuilding.incidents.service;

import com.mybuilding.incidents.feignClient.ApartmentDTO;
import com.mybuilding.incidents.feignClient.ResidentsFeignClient;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration;
import io.github.resilience4j.springboot3.retry.autoconfigure.RetryAutoConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = {
                AopAutoConfiguration.class,
                CircuitBreakerAutoConfiguration.class,
                RetryAutoConfiguration.class,
                ResidentsServiceClient.class
        },
        properties = {
                "spring.cloud.config.enabled=false",
                "eureka.client.enabled=false",
                "resilience4j.circuitbreaker.circuit-breaker-aspect-order=1",
                "resilience4j.circuitbreaker.instances.residentsService.sliding-window-size=5",
                "resilience4j.circuitbreaker.instances.residentsService.failure-rate-threshold=50",
                "resilience4j.circuitbreaker.instances.residentsService.ignore-exceptions[0]=feign.FeignException$NotFound",
                "resilience4j.retry.retry-aspect-order=2",
                "resilience4j.retry.instances.residentsService.max-attempts=3",
                "resilience4j.retry.instances.residentsService.wait-duration=10ms",
                "resilience4j.retry.instances.residentsService.ignore-exceptions[0]=feign.FeignException$NotFound"
        }
)
class ResidentsServiceClientResilienceTest {

    @Autowired
    private ResidentsServiceClient client;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @MockBean
    private ResidentsFeignClient feignClient;

    @BeforeEach
    void resetBreaker() {
        circuitBreakerRegistry.circuitBreaker("residentsService").reset();
    }

    @Test
    void shouldPropagate404WithoutRetryOrFallback() {
        Request req = Request.create(Request.HttpMethod.GET, "/api/apartments/9999",
                Collections.emptyMap(), null, StandardCharsets.UTF_8, new RequestTemplate());
        when(feignClient.getApartmentById(9999L))
                .thenThrow(new FeignException.NotFound("Not Found", req, null, Collections.emptyMap()));

        assertThrows(FeignException.NotFound.class, () -> client.getApartmentById(9999L));
        verify(feignClient, times(1)).getApartmentById(9999L);
    }

    @Test
    void shouldRetryTransientErrorAndSucceedOnThirdAttempt() {
        Request req = Request.create(Request.HttpMethod.GET, "/api/apartments/1",
                Collections.emptyMap(), null, StandardCharsets.UTF_8, new RequestTemplate());
        FeignException.ServiceUnavailable unavailable =
                new FeignException.ServiceUnavailable("Down", req, null, Collections.emptyMap());
        ApartmentDTO expected = new ApartmentDTO(1L, "A-101", 1, new BigDecimal("85.50"), 1L);

        when(feignClient.getApartmentById(1L))
                .thenThrow(unavailable)
                .thenThrow(unavailable)
                .thenReturn(expected);

        ApartmentDTO result = client.getApartmentById(1L);
        assertEquals("A-101", result.getNumero());
        verify(feignClient, times(3)).getApartmentById(1L);
    }

    @Test
    void shouldReturnFallbackAfterRetriesExhausted() {
        Request req = Request.create(Request.HttpMethod.GET, "/api/apartments/1",
                Collections.emptyMap(), null, StandardCharsets.UTF_8, new RequestTemplate());
        when(feignClient.getApartmentById(1L))
                .thenThrow(new FeignException.ServiceUnavailable("Down", req, null, Collections.emptyMap()));

        ApartmentDTO result = client.getApartmentById(1L);
        assertTrue(ResidentsServiceClient.isFallbackApartment(result));
        assertEquals(ResidentsServiceClient.UNAVAILABLE_APARTMENT, result.getNumero());
        verify(feignClient, times(3)).getApartmentById(1L);
    }
}
