package com.mybuilding.incidents.controller;

import com.mybuilding.incidents.feignClient.ApartmentDTO;
import com.mybuilding.incidents.feignClient.ResidentDTO;
import com.mybuilding.incidents.feignClient.ResidentsFeignClient;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
@AutoConfigureMockMvc
@TestPropertySource(
        locations = "file:../config-repo/service-incidents.properties",
        properties = {
                "spring.datasource.url=jdbc:h2:mem:incidentsinteg;DB_CLOSE_DELAY=-1",
                "resilience4j.retry.instances.residentsService.wait-duration=10ms"
        }
)
class TicketControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @MockBean
    private ResidentsFeignClient residentsFeignClient;

    @BeforeEach
    void resetCircuitBreaker() {
        circuitBreakerRegistry.circuitBreaker("residentsService").reset();
    }

    @Test
    void shouldValidateConfigRepoAndHandleV4AndV6Flows() throws Exception {
        Request req404 = Request.create(Request.HttpMethod.GET, "/api/apartments/9999",
                Collections.emptyMap(), null, StandardCharsets.UTF_8, new RequestTemplate());
        when(residentsFeignClient.getApartmentById(9999L))
                .thenThrow(new FeignException.NotFound("Not Found", req404, null, Collections.emptyMap()));

        // V4 (test 6.2): appartement inexistant -> 404 NOT FOUND même avec Resilience4j actif
        mockMvc.perform(post("/api/tickets")
                        .header("Idempotency-Key", "INT-TKT-404")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"apartmentId\":9999,\"description\":\"Test 404\",\"priorite\":\"HAUTE\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        // La clé d'idempotence ne doit PAS être brûlée après un 404, donc si appt 1 existe ensuite :
        when(residentsFeignClient.getApartmentById(1L))
                .thenReturn(new ApartmentDTO(1L, "A-101", 1, new BigDecimal("85.50"), 1L));
        when(residentsFeignClient.getResidentById(1L))
                .thenReturn(new ResidentDTO(1L, "Ahmed Alami", "ahmed@test.ma", "0600000000", "PROPRIETAIRE", BigDecimal.ZERO));

        mockMvc.perform(post("/api/tickets")
                        .header("Idempotency-Key", "INT-TKT-201")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"apartmentId\":1,\"description\":\"Fuite cuisine\",\"priorite\":\"HAUTE\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.apartmentId").value(1));

        // Rejeu même clé -> 409 Conflict
        mockMvc.perform(post("/api/tickets")
                        .header("Idempotency-Key", "INT-TKT-201")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"apartmentId\":1,\"description\":\"Fuite cuisine\",\"priorite\":\"HAUTE\"}"))
                .andExpect(status().isConflict());

        // V4 Consultation enrichie (DTO Composé)
        mockMvc.perform(get("/api/tickets/1/detail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.description").isNotEmpty())
                .andExpect(jsonPath("$.apartmentNumero").value("A-101"))
                .andExpect(jsonPath("$.residentNom").value("Ahmed Alami"));

        // V6 Fallback en lecture quand service-residents tombe en panne
        Request req503 = Request.create(Request.HttpMethod.GET, "/api/apartments/1",
                Collections.emptyMap(), null, StandardCharsets.UTF_8, new RequestTemplate());
        when(residentsFeignClient.getApartmentById(1L))
                .thenThrow(new FeignException.ServiceUnavailable("Down", req503, null, Collections.emptyMap()));

        mockMvc.perform(get("/api/tickets/1/detail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.apartmentNumero").value("INDISPONIBLE (CircuitBreaker)"));
    }
}
