package com.andersonvianadev.finance_control_api.controllers;

import com.andersonvianadev.finance_control_api.controllers.dtos.requests.UserRequestDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.requests.UserUpdateDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.UserResponseDTO;
import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.models.enums.UserRole;
import com.andersonvianadev.finance_control_api.domain.services.IUserService;
import com.andersonvianadev.finance_control_api.infra.exceptions.StandardException;
import com.andersonvianadev.finance_control_api.infra.repositories.CategoryRepository;
import com.andersonvianadev.finance_control_api.infra.repositories.UserRepository;
import org.junit.jupiter.api.*;
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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository repository;

    @Autowired
    private IUserService userService;

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void setup() {
        categoryRepository.deleteAll();
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

    @Test
    @DisplayName("Should return user when a valid ID is provided")
    void findById_WhenUserExists_ShouldReturnUser() throws Exception {
        User user = User.builder()
                .name("Anderson")
                .email("anderson@gmail.com")
                .password("Anderson@12")
                .build();

        user = repository.save(user);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/users/" + user.getId().toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        String content = result.getResponse().getContentAsString();

        UserResponseDTO response = objectMapper.readValue(content, UserResponseDTO.class);

        assertEquals(user.getId(), response.id());
        assertEquals(user.getName(), response.name());
        assertEquals(user.getEmail(), response.email());
    }

    @Test
    @DisplayName("Should throw NotFoundException when user does not exist")
    void findById_WhenUserDoesNotExist_ShouldThrowNotFoundException() throws Exception{
        UUID id = UUID.randomUUID();

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/users/" + id.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        String content = result.getResponse().getContentAsString();

        StandardException exception = objectMapper.readValue(content, StandardException.class);

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.status());
    }

    @Test
    @DisplayName("Should delete user successfully when user exists")
    void deleteById_WhenUserExists_ShouldDeleteUser() throws Exception {
        User user = User.builder()
                .name("Anderson")
                .email("anderson@gmail.com")
                .password("Anderson@12")
                .build();

        user = repository.save(user);

        mockMvc.perform(MockMvcRequestBuilders.delete("/users/" + user.getId().toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNoContent())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        assertTrue(repository.findById(user.getId()).isEmpty());
    }

    @Test
    @DisplayName("Should throw NotFoundException when user does not exist")
    void deleteById_WhenUserDoesNotExist_ShouldThrowNotFoundException() throws Exception {
        UUID id = UUID.randomUUID();

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.delete("/users/" + id.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        String content = result.getResponse().getContentAsString();

        StandardException exception = objectMapper.readValue(content, StandardException.class);

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.status());
    }

    @Test
    @DisplayName("Should update user successfully when user exists")
    void update_WhenUserExists_ShouldUpdateUser() throws Exception {
        User userSaved = User.builder()
                .name("Anderson")
                .email("anderson@gmail.com")
                .password("anderson@12")
                .build();

        userSaved = repository.save(userSaved);

        UserUpdateDTO update = new UserUpdateDTO("Anderson12", "anderson12@gmail.com");

        String updateJson = objectMapper.writeValueAsString(update);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.put("/users/" + userSaved.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        String content = result.getResponse().getContentAsString();

        UserResponseDTO response = objectMapper.readValue(content, UserResponseDTO.class);

        assertEquals(userSaved.getId(), response.id());
        assertEquals(update.name(), response.name());
        assertEquals(update.email(), response.email());
    }

    @Test
    @DisplayName("Should throw NotFoundException when user does not exist")
    void update_WhenUserDoesNotExist_ShouldThrowNotFoundException() throws Exception {
        UUID id = UUID.randomUUID();

        UserUpdateDTO update = new UserUpdateDTO("Anderson", "anderson@gmail.com");

        String updateJson = objectMapper.writeValueAsString(update);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.put("/users/" + id.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        String content = result.getResponse().getContentAsString();

        StandardException exception = objectMapper.readValue(content, StandardException.class);

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.status());
    }

    @Test
    @DisplayName("Should throw ResourceAlreadyExistsException when email already exists")
    void update_ShouldThrowResourceAlreadyExistsException_WhenEmailAlreadyExists()  throws Exception {
        User userSaved = User.builder()
                .name("Anderson")
                .email("anderson@gmail.com")
                .password("anderson@12")
                .build();

        userSaved = repository.save(userSaved);

        User userEmailExists = User.builder()
                .name("anderson12")
                .email("anderson12@gmail.com")
                .password("anderson@12")
                .build();

        userService.save(userEmailExists);

        UserUpdateDTO update = new UserUpdateDTO("Anderson12", "anderson12@gmail.com");

        String updateJson = objectMapper.writeValueAsString(update);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.put("/users/" + userSaved.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        String content = result.getResponse().getContentAsString();

        StandardException exception = objectMapper.readValue(content, StandardException.class);

        assertEquals(HttpStatus.CONFLICT.value(), exception.status());
    }
}
