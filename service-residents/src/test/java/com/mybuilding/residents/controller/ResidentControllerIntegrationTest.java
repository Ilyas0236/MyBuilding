package com.mybuilding.residents.controller;

import com.mybuilding.residents.service.ResidentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
@AutoConfigureMockMvc
@TestPropertySource(
        locations = "file:../config-repo/service-residents.properties",
        properties = {
                "spring.datasource.url=jdbc:h2:mem:residentstest;DB_CLOSE_DELAY=-1"
        }
)
class ResidentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldLoadDataInitializerAndHandleValidationErrorsAndIdempotency() throws Exception {
        // V1: DataInitializer charge 2 résidents initiaux
        mockMvc.perform(get("/api/residents/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Ahmed Alami"));

        // V6 ControllerAdvice: 400 JSON uniforme sur validation DTO
        mockMvc.perform(post("/api/residents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"\",\"email\":\"invalid\",\"telephone\":\"\",\"role\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.errors.email").exists());

        // V6 ControllerAdvice: 400 sur Enum invalide
        mockMvc.perform(post("/api/residents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"Test\",\"email\":\"enum.err@test.ma\",\"telephone\":\"0600000000\",\"role\":\"INCONNU\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        // V6 Idempotence: 1er appel 201, 2ème appel même clé 409
        String validBody = "{\"nom\":\"Nadia Test\",\"email\":\"nadia.test@mybuilding.ma\",\"telephone\":\"0611223344\",\"role\":\"PROPRIETAIRE\"}";
        mockMvc.perform(post("/api/residents")
                        .header("Idempotency-Key", "INT-RES-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.solde").value(0));

        mockMvc.perform(post("/api/residents")
                        .header("Idempotency-Key", "INT-RES-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}
