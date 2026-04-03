package com.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.dto.request.FinancialRecordRequest;
import com.finance.dto.request.LoginRequest;
import com.finance.dto.request.RegisterRequest;
import com.finance.enums.RecordType;
import com.finance.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("FinancialRecord Controller Integration Tests")
class FinancialRecordControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String adminToken;
    private String analystToken;
    private String viewerToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken   = registerAndLogin("rec_admin",   "rec_admin@test.com",   "password123", Role.ADMIN);
        analystToken = registerAndLogin("rec_analyst", "rec_analyst@test.com", "password123", Role.ANALYST);
        viewerToken  = registerAndLogin("rec_viewer",  "rec_viewer@test.com",  "password123", Role.VIEWER);
    }

    // â”€â”€â”€ Create â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @DisplayName("POST /api/records - ADMIN can create a record")
    void createRecord_asAdmin_returns201() throws Exception {
        mockMvc.perform(post("/api/records")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildSampleRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.amount").value(1500.00))
                .andExpect(jsonPath("$.data.type").value("INCOME"));
    }

    @Test
    @DisplayName("POST /api/records - ANALYST is forbidden from creating records")
    void createRecord_asAnalyst_returns403() throws Exception {
        mockMvc.perform(post("/api/records")
                        .header("Authorization", "Bearer " + analystToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildSampleRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/records - VIEWER is forbidden from creating records")
    void createRecord_asViewer_returns403() throws Exception {
        mockMvc.perform(post("/api/records")
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildSampleRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/records - returns 401 when no token is provided")
    void createRecord_noToken_returns401() throws Exception {
        mockMvc.perform(post("/api/records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildSampleRequest())))
                .andExpect(status().isUnauthorized());
    }

    // â”€â”€â”€ Read â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @DisplayName("GET /api/records - ANALYST can list records")
    void getRecords_asAnalyst_returns200() throws Exception {
        mockMvc.perform(get("/api/records")
                        .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("GET /api/records - VIEWER is forbidden from viewing records")
    void getRecords_asViewer_returns403() throws Exception {
        mockMvc.perform(get("/api/records")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isForbidden());
    }

    // â”€â”€â”€ Validation â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @DisplayName("POST /api/records - 400 Bad Request when amount is zero")
    void createRecord_zeroAmount_returns400() throws Exception {
        FinancialRecordRequest req = buildSampleRequest();
        req.setAmount(BigDecimal.ZERO);

        mockMvc.perform(post("/api/records")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.amount").exists());
    }

    @Test
    @DisplayName("POST /api/records - 400 Bad Request when date is in the future")
    void createRecord_futureDate_returns400() throws Exception {
        FinancialRecordRequest req = buildSampleRequest();
        req.setDate(LocalDate.now().plusDays(10));

        mockMvc.perform(post("/api/records")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.date").exists());
    }

    // â”€â”€â”€ Delete (soft delete) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @DisplayName("DELETE /api/records/{id} - ADMIN can soft-delete a record")
    void deleteRecord_asAdmin_returns200() throws Exception {
        // Create first
        MvcResult createResult = mockMvc.perform(post("/api/records")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildSampleRequest())))
                .andExpect(status().isCreated())
                .andReturn();

        String body = createResult.getResponse().getContentAsString();
        Long id = objectMapper.readTree(body).path("data").path("id").asLong();

        // Then delete
        mockMvc.perform(delete("/api/records/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verify it's gone from the listing
        mockMvc.perform(get("/api/records/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    // â”€â”€â”€ Helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private FinancialRecordRequest buildSampleRequest() {
        FinancialRecordRequest req = new FinancialRecordRequest();
        req.setAmount(new BigDecimal("1500.00"));
        req.setType(RecordType.INCOME);
        req.setCategory("Salary");
        req.setDate(LocalDate.now());
        req.setNotes("Test record");
        return req;
    }

    private String registerAndLogin(String username, String email,
                                    String password, Role role) throws Exception {
        RegisterRequest reg = new RegisterRequest();
        reg.setUsername(username);
        reg.setEmail(email);
        reg.setPassword(password);
        reg.setRole(role);

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andReturn();

        String token = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
        System.out.println("DEBUG TOKEN: " + token);
        return token;
    }
}
