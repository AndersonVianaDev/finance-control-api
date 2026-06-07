package com.andersonvianadev.finance_control_api.infra.ratelimit;

import com.andersonvianadev.finance_control_api.controllers.dtos.requests.LoginRequestDTO;
import com.andersonvianadev.finance_control_api.infra.exceptions.StandardException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.rate-limit.enabled=true",
        "app.rate-limit.global.requests-per-minute=5",
        "app.rate-limit.auth.requests-per-minute=2"
})
class RateLimitFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RateLimitService rateLimitService;

    @BeforeEach
    void setup() {
        rateLimitService.resetBuckets();
    }

    @Test
    @DisplayName("Should process requests successfully when global limit is not exceeded")
    void globalRateLimit_WhenRequestsWithinLimit_ShouldProcessSuccessfully() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(MockMvcRequestBuilders.get("/v3/api-docs"))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.header().exists(RateLimitFilter.HEADER_LIMIT))
                    .andExpect(MockMvcResultMatchers.header().exists(RateLimitFilter.HEADER_REMAINING))
                    .andExpect(MockMvcResultMatchers.header().exists(RateLimitFilter.HEADER_RESET));
        }
    }

    @Test
    @DisplayName("Should return 429 when global limit is exceeded")
    void globalRateLimit_WhenLimitExceeded_ShouldReturnTooManyRequests() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(MockMvcRequestBuilders.get("/v3/api-docs"))
                    .andExpect(MockMvcResultMatchers.status().isOk());
        }

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/v3/api-docs"))
                .andExpect(MockMvcResultMatchers.status().isTooManyRequests())
                .andExpect(MockMvcResultMatchers.header().string(RateLimitFilter.HEADER_LIMIT, "5"))
                .andExpect(MockMvcResultMatchers.header().string(RateLimitFilter.HEADER_REMAINING, "0"))
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                StandardException.class
        );

        assertNotNull(exception);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), exception.status());
        assertEquals("Too many requests. Please try again later.", exception.error());
    }

    @Test
    @DisplayName("Should return 429 when auth limit is exceeded")
    void authRateLimit_WhenLimitExceeded_ShouldReturnTooManyRequests() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("unknown@gmail.com", "WrongPass1!");

        String requestJson = objectMapper.writeValueAsString(request);

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(MockMvcRequestBuilders.post("/users/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                    .andExpect(MockMvcResultMatchers.header().string(RateLimitFilter.HEADER_LIMIT, "2"));
        }

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(MockMvcResultMatchers.status().isTooManyRequests())
                .andExpect(MockMvcResultMatchers.header().string(RateLimitFilter.HEADER_LIMIT, "2"))
                .andExpect(MockMvcResultMatchers.header().string(RateLimitFilter.HEADER_REMAINING, "0"))
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                StandardException.class
        );

        assertNotNull(exception);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), exception.status());
    }

    @Test
    @DisplayName("Should block auth routes before global limit is reached")
    void authRateLimit_ShouldBlockBeforeGlobalLimit() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("unknown@gmail.com", "WrongPass1!");

        String requestJson = objectMapper.writeValueAsString(request);

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(MockMvcRequestBuilders.post("/users/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(MockMvcResultMatchers.status().isUnauthorized());
        }

        mockMvc.perform(MockMvcRequestBuilders.post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(MockMvcResultMatchers.status().isTooManyRequests());

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/v3/api-docs"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        assertNotEquals(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                result.getResponse().getStatus()
        );
    }
}
