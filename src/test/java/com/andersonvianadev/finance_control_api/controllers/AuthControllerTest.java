package com.andersonvianadev.finance_control_api.controllers;

import com.andersonvianadev.finance_control_api.controllers.dtos.requests.LoginRequestDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.LoginResponseDTO;
import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.services.IUserService;
import com.andersonvianadev.finance_control_api.infra.exceptions.StandardException;
import com.andersonvianadev.finance_control_api.infra.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository repository;

    @Autowired
    private IUserService userService;

    @BeforeEach
    void setup() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("Should return JWT token when credentials are valid")
    void login_ShouldReturnToken_WhenCredentialsAreValid() throws Exception {
        User user = User.builder()
                .name("Anderson")
                .email("anderson@gmail.com")
                .password("Arthur@1406")
                .build();

        userService.save(user);

        LoginRequestDTO request = new LoginRequestDTO("anderson@gmail.com", "Arthur@1406");

        String requestJson = objectMapper.writeValueAsString(request);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        String content = result.getResponse().getContentAsString();

        LoginResponseDTO response = objectMapper.readValue(content, LoginResponseDTO.class);

        assertNotNull(response.token());
    }

    @Test
    @DisplayName("Should return unauthorized when password is incorrect")
    void login_ShouldReturnUnauthorized_WhenPasswordIsIncorrect() throws Exception {
        User user = User.builder()
                .name("Anderson")
                .email("anderson@gmail.com")
                .password("Arthur@1406")
                .build();

        userService.save(user);

        LoginRequestDTO request = new LoginRequestDTO("anderson@gmail.com", "Arthur@14066");

        String requestJson = objectMapper.writeValueAsString(request);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        String content = result.getResponse().getContentAsString();

        StandardException exception = objectMapper.readValue(content, StandardException.class);

        assertNotNull(exception);
    }

    @Test
    @DisplayName("Should return bad request when email format is invalid")
    void login_ShouldReturnBadRequest_WhenEmailFormatIsInvalid() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("anderson.com", "Arthur@14066");

        String requestJson = objectMapper.writeValueAsString(request);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        String content = result.getResponse().getContentAsString();

        StandardException exception = objectMapper.readValue(content, StandardException.class);

        assertNotNull(exception);
    }
}
