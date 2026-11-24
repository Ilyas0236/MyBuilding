package com.mybuilding.finances.controller;

import com.mybuilding.finances.feignClient.ApartmentDTO;
import com.mybuilding.finances.feignClient.ResidentDTO;
import com.mybuilding.finances.feignClient.ResidentsFeignClient;
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

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
        locations = "file:../config-repo/service-finances.properties",
        properties = {
                "spring.datasource.url=jdbc:h2:mem:financesctrl;DB_CLOSE_DELAY=-1",
                "resilience4j.retry.instances.residentsService.wait-duration=10ms"
        }
)
class ChargeAndPaymentControllerIntegrationTest {

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
    void shouldValidateChargeCreationPaymentSoldeUpdateAndEnrichedDto() throws Exception {
        Request req404 = Request.create(Request.HttpMethod.GET, "/api/apartments/9999",
                Collections.emptyMap(), null, StandardCharsets.UTF_8, new RequestTemplate());
        when(residentsFeignClient.getApartmentById(9999L))
                .thenThrow(new FeignException.NotFound("Not Found", req404, null, Collections.emptyMap()));

        // V4 (test 6.7): Charge pour appt inexistant -> 404
        mockMvc.perform(post("/api/charges")
                        .header("Idempotency-Key", "INT-CHG-404")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"apartmentId\":9999,\"periode\":\"2026-12\",\"montant\":1600.00}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        // V4 Consultation enrichie Charge 1
        when(residentsFeignClient.getApartmentById(1L))
                .thenReturn(new ApartmentDTO(1L, "A-101", 1, new BigDecimal("85.50"), 1L));
        when(residentsFeignClient.getResidentById(1L))
                .thenReturn(new ResidentDTO(1L, "Ahmed Alami", "ahmed@test.ma", "0600000000", "PROPRIETAIRE", BigDecimal.ZERO));

        mockMvc.perform(get("/api/charges/1/detail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.apartmentNumero").value("A-101"))
                .andExpect(jsonPath("$.residentNom").value("Ahmed Alami"));

        // V4 & V6 Paiement valide avec Idempotency-Key -> 201 + PATCH solde résident
        mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", "INT-PAY-201")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"chargeId\":1,\"date\":\"2026-11-24\",\"montant\":50.00,\"moyen\":\"VIREMENT\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.chargeId").value(1));

        verify(residentsFeignClient).updateResidentSolde(eq(1L), anyMap());

        // Rejeu même clé -> 409
        mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", "INT-PAY-201")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"chargeId\":1,\"date\":\"2026-11-24\",\"montant\":50.00,\"moyen\":\"VIREMENT\"}"))
                .andExpect(status().isConflict());
    }
}
