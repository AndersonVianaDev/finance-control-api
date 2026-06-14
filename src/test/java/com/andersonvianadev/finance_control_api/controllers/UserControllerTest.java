package com.andersonvianadev.finance_control_api.controllers;

import com.andersonvianadev.finance_control_api.controllers.dtos.requests.UserRequestDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.requests.UserUpdateDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.UserResponseDTO;
import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.models.enums.UserRole;
import com.andersonvianadev.finance_control_api.domain.services.IUserService;
import com.andersonvianadev.finance_control_api.infra.exceptions.StandardException;
import com.andersonvianadev.finance_control_api.infra.repositories.BudgetRepository;
import com.andersonvianadev.finance_control_api.infra.repositories.CategoryRepository;
import com.andersonvianadev.finance_control_api.infra.repositories.ExpenseRepository;
import com.andersonvianadev.finance_control_api.infra.repositories.IncomeRepository;
import com.andersonvianadev.finance_control_api.infra.repositories.InstallmentPlanRepository;
import com.andersonvianadev.finance_control_api.infra.repositories.RecurringRuleRepository;
import com.andersonvianadev.finance_control_api.infra.repositories.UserRepository;
import com.andersonvianadev.finance_control_api.infra.security.UserPrincipal;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@SpringBootTest
@AutoConfigureMockMvc
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

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private IncomeRepository incomeRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private InstallmentPlanRepository installmentPlanRepository;

    @Autowired
    private RecurringRuleRepository recurringRuleRepository;

    @BeforeEach
    void setup() {
        incomeRepository.deleteAll();
        expenseRepository.deleteAll();
        installmentPlanRepository.deleteAll();
        budgetRepository.deleteAll();
        recurringRuleRepository.deleteAll();
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
    @DisplayName("Should return authenticated user profile when user is logged in")
    void findById_WhenUserIsAuthenticated_ShouldReturnUserProfile() throws Exception {
        User userSaved = userService.save(
                User.builder()
                        .name("Anderson")
                        .email("anderson@gmail.com")
                        .password("Anderson@12")
                        .role(UserRole.ROLE_USER)
                        .build()
        );

        UserPrincipal userPrincipal = new UserPrincipal(userSaved);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/users")
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        String content = result.getResponse().getContentAsString();

        UserResponseDTO response = objectMapper.readValue(content, UserResponseDTO.class);

        assertEquals(userSaved.getId(), response.id());
        assertEquals(userSaved.getName(), response.name());
        assertEquals(userSaved.getEmail(), response.email());
    }

    @Test
    @DisplayName("Should delete user successfully when admin deletes an existing user")
    void deleteById_WhenAdminAndUserExists_ShouldDeleteUser() throws Exception {
        User admin = userService.save(
                User.builder()
                        .name("Admin")
                        .email("admin@gmail.com")
                        .password("Admin@1234")
                        .role(UserRole.ROLE_ADMIN)
                        .build()
        );

        User userToDelete = userService.save(
                User.builder()
                        .name("Anderson")
                        .email("anderson@gmail.com")
                        .password("Anderson@12")
                        .role(UserRole.ROLE_USER)
                        .build()
        );

        UserPrincipal adminPrincipal = new UserPrincipal(admin);

        mockMvc.perform(MockMvcRequestBuilders.delete("/users/" + userToDelete.getId())
                        .with(user(adminPrincipal))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNoContent())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        assertTrue(repository.findById(userToDelete.getId()).isEmpty());
    }

    @Test
    @DisplayName("Should throw NotFoundException when user does not exist")
    void deleteById_WhenUserDoesNotExist_ShouldThrowNotFoundException() throws Exception {
        User admin = userService.save(
                User.builder()
                        .name("Admin")
                        .email("admin@gmail.com")
                        .password("Admin@1234")
                        .role(UserRole.ROLE_ADMIN)
                        .build()
        );

        UserPrincipal adminPrincipal = new UserPrincipal(admin);
        UUID id = UUID.randomUUID();

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.delete("/users/" + id)
                        .with(user(adminPrincipal))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        String content = result.getResponse().getContentAsString();

        StandardException exception = objectMapper.readValue(content, StandardException.class);

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.status());
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when user is not admin")
    void deleteById_WhenUserIsNotAdmin_ShouldThrowAccessDeniedException() throws Exception {
        User userSaved = userService.save(
                User.builder()
                        .name("Anderson")
                        .email("anderson@gmail.com")
                        .password("Anderson@12")
                        .role(UserRole.ROLE_USER)
                        .build()
        );

        User userToDelete = userService.save(
                User.builder()
                        .name("Other")
                        .email("other@gmail.com")
                        .password("Other@1234")
                        .role(UserRole.ROLE_USER)
                        .build()
        );

        UserPrincipal userPrincipal = new UserPrincipal(userSaved);

        mockMvc.perform(MockMvcRequestBuilders.delete("/users/" + userToDelete.getId())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        assertTrue(repository.findById(userToDelete.getId()).isPresent());
    }

    @Test
    @DisplayName("Should update authenticated user successfully when user exists")
    void update_WhenUserExists_ShouldUpdateUser() throws Exception {
        User userSaved = userService.save(
                User.builder()
                        .name("Anderson")
                        .email("anderson@gmail.com")
                        .password("anderson@12")
                        .role(UserRole.ROLE_USER)
                        .build()
        );

        UserPrincipal userPrincipal = new UserPrincipal(userSaved);

        UserUpdateDTO update = new UserUpdateDTO("Anderson12", "anderson12@gmail.com");

        String updateJson = objectMapper.writeValueAsString(update);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.put("/users")
                        .with(user(userPrincipal))
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
    @DisplayName("Should throw ResourceAlreadyExistsException when email already exists")
    void update_ShouldThrowResourceAlreadyExistsException_WhenEmailAlreadyExists() throws Exception {
        User userSaved = userService.save(
                User.builder()
                        .name("Anderson")
                        .email("anderson@gmail.com")
                        .password("anderson@12")
                        .role(UserRole.ROLE_USER)
                        .build()
        );

        userService.save(
                User.builder()
                        .name("anderson12")
                        .email("anderson12@gmail.com")
                        .password("anderson@12")
                        .role(UserRole.ROLE_USER)
                        .build()
        );

        UserPrincipal userPrincipal = new UserPrincipal(userSaved);

        UserUpdateDTO update = new UserUpdateDTO("Anderson12", "anderson12@gmail.com");

        String updateJson = objectMapper.writeValueAsString(update);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.put("/users")
                        .with(user(userPrincipal))
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
