package com.andersonvianadev.finance_control_api.controllers;

import com.andersonvianadev.finance_control_api.controllers.dtos.requests.UserRequestDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.UserResponseDTO;
import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.infra.exceptions.StandardException;
import com.andersonvianadev.finance_control_api.infra.repositories.UserRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository repository;

    @BeforeEach
    void setup() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("Should save user successfully when email does not exist")
    void save_ShouldReturnSavedUser_WhenEmailDoesNotExists() throws Exception {
        UserRequestDTO request = new UserRequestDTO("Anderson", "anderson@gmail.com", "Arthur@1406");

        String requestJson = objectMapper.writeValueAsString(request);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        String content = result.getResponse().getContentAsString();

        UserResponseDTO response = objectMapper.readValue(content, UserResponseDTO.class);

        assertNotNull(response.id());
        assertEquals(request.name(), response.name());
        assertEquals(request.email(), response.email());
    }

    @Test
    @DisplayName("Should throw ResourceAlreadyExistsException when email already exists")
    void save_ShouldThrowResourceAlreadyExistsException_WhenEmailAlreadyExists() throws Exception {
        User userSaved = User.builder()
                .name("Anderson")
                .email("anderson@gmail.com")
                .password("anderson")
                .build();

        repository.save(userSaved);

        UserRequestDTO request = new UserRequestDTO("anderson", "anderson@gmail.com", "Arthur@1406");

        String requestJson = objectMapper.writeValueAsString(request);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        String content = result.getResponse().getContentAsString();

        StandardException exception = objectMapper.readValue(content, StandardException.class);

        assertEquals(HttpStatus.CONFLICT.value(), exception.status());
    }

    @Test
    @DisplayName("Should throw MethodArgumentNotValidException when field invalid")
    void save_ShouldThrowMethodArgumentNotValidException_WhenFieldInvalid() throws Exception {
        UserRequestDTO request = new UserRequestDTO("Anderson", "anderson@gmail.com", "aaa");

        String requestJson = objectMapper.writeValueAsString(request);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        String content = result.getResponse().getContentAsString();

        StandardException exception = objectMapper.readValue(content, StandardException.class);

        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.status());
    }
}
