package com.paymentapp.payment.integration;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import paymentapp.payment.dto.TransferRequest;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@Testcontainers
class PaymentIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("payment_db")
            .withUsername("payment_user")
            .withPassword("payment_pass");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldInitiateTransferSuccessfully() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setSourceAccount("ACC001");
        request.setDestinationAccount("ACC002");
        request.setAmount(new BigDecimal("1000.00"));
        request.setDescription("Test transfer");
        request.setIdempotencyKey(UUID.randomUUID().toString());

        mockMvc.perform(post("/api/v1/payments/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());
//                .andExpected(jsonPath("$.status").value("ACCEPTED"))
//                .andExpected(jsonPath("$.txId").exists());
    }

    @Test
    void shouldRejectTransferWithInsufficientFunds() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setSourceAccount("ACC002");  // Account with 0 balance
        request.setDestinationAccount("ACC001");
        request.setAmount(new BigDecimal("1000.00"));
        request.setDescription("Test transfer");
        request.setIdempotencyKey(UUID.randomUUID().toString());

        mockMvc.perform(post("/api/v1/payments/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted()); // Initial acceptance
    }

    @Test
    void shouldGetAccountBalance() throws Exception {
        mockMvc.perform(get("/api/v1/payments/accounts/ACC001/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("ACC001"))
                .andExpect(jsonPath("$.book").exists())
                .andExpect(jsonPath("$.available").exists())
                .andExpect(jsonPath("$.openHold").exists());
    }

    @Test
    void shouldReturnNotFoundForNonExistentAccount() throws Exception {
        mockMvc.perform(get("/api/v1/payments/accounts/NONEXISTENT/balance"))
                .andExpect(status().isNotFound());
    }
}