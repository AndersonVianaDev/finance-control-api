package com.andersonvianadev.finance_control_api.controllers;

import com.andersonvianadev.finance_control_api.controllers.dtos.requests.BudgetRequestDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.requests.BudgetUpdateDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.BudgetResponseDTO;
import com.andersonvianadev.finance_control_api.domain.models.Budget;
import com.andersonvianadev.finance_control_api.domain.models.Category;
import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.models.enums.BudgetType;
import com.andersonvianadev.finance_control_api.domain.models.enums.UserRole;
import com.andersonvianadev.finance_control_api.domain.services.IUserService;
import com.andersonvianadev.finance_control_api.infra.exceptions.StandardException;
import com.andersonvianadev.finance_control_api.infra.repositories.BudgetRepository;
import com.andersonvianadev.finance_control_api.infra.repositories.CategoryRepository;
import com.andersonvianadev.finance_control_api.infra.repositories.UserRepository;
import com.andersonvianadev.finance_control_api.infra.security.UserPrincipal;
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

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@SpringBootTest
@AutoConfigureMockMvc
class BudgetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IUserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @BeforeEach
    void setup() {
        budgetRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should save budget successfully when budget is valid")
    void save_WhenBudgetIsValid_ShouldReturnSavedBudget() throws Exception {
        User userSaved = userService.save(
                User.builder()
                        .name("Anderson")
                        .email("anderson@gmail.com")
                        .password("Arthur@1406")
                        .role(UserRole.ROLE_USER)
                        .build()
        );

        Category categorySaved = categoryRepository.save(
                Category.builder()
                        .name("Food")
                        .description("Food description")
                        .icon("food")
                        .owner(userSaved)
                        .build()
        );

        UserPrincipal userPrincipal = new UserPrincipal(userSaved);

        BudgetRequestDTO request = new BudgetRequestDTO(
                categorySaved.getId(),
                BudgetType.MONTHLY,
                new BigDecimal("1500.00")
        );

        String requestJson = objectMapper.writeValueAsString(request);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/budgets")
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        String content = result.getResponse().getContentAsString();

        BudgetResponseDTO response = objectMapper.readValue(content, BudgetResponseDTO.class);

        assertNotNull(response);
        assertEquals(userSaved.getId(), response.user().id());
        assertEquals(categorySaved.getId(), response.category().id());
        assertEquals(request.budgetType(), response.budgetType());
        assertEquals(0, request.limitAmount().compareTo(response.limitAmount()));
        assertTrue(response.active());
        assertEquals(1, budgetRepository.count());
    }

    @Test
    @DisplayName("Should throw NotFoundException when category does not exist")
    void save_WhenCategoryDoesNotExist_ShouldThrowNotFoundException() throws Exception {
        User userSaved = userService.save(
                User.builder()
                        .name("Anderson")
                        .email("anderson@gmail.com")
                        .password("Arthur@1406")
                        .role(UserRole.ROLE_USER)
                        .build()
        );

        UserPrincipal userPrincipal = new UserPrincipal(userSaved);

        BudgetRequestDTO request = new BudgetRequestDTO(
                UUID.randomUUID(),
                BudgetType.MONTHLY,
                new BigDecimal("1500.00")
        );

        String requestJson = objectMapper.writeValueAsString(request);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/budgets")
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        String content = result.getResponse().getContentAsString();

        StandardException exception = objectMapper.readValue(content, StandardException.class);

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.status());
    }

    @Test
    @DisplayName("Should throw ResourceAlreadyExistsException when budget already exists for category")
    void save_WhenBudgetAlreadyExistsForCategory_ShouldThrowResourceAlreadyExistsException() throws Exception {
        User userSaved = userService.save(
                User.builder()
                        .name("Anderson")
                        .email("anderson@gmail.com")
                        .password("Arthur@1406")
                        .role(UserRole.ROLE_USER)
                        .build()
        );

        Category categorySaved = categoryRepository.save(
                Category.builder()
                        .name("Food")
                        .description("Food description")
                        .icon("food")
                        .owner(userSaved)
                        .build()
        );

        budgetRepository.save(
                Budget.builder()
                        .owner(userSaved)
                        .category(categorySaved)
                        .budgetType(BudgetType.MONTHLY)
                        .limitAmount(new BigDecimal("1000.00"))
                        .build()
        );

        UserPrincipal userPrincipal = new UserPrincipal(userSaved);

        BudgetRequestDTO request = new BudgetRequestDTO(
                categorySaved.getId(),
                BudgetType.WEEKLY,
                new BigDecimal("500.00")
        );

        String requestJson = objectMapper.writeValueAsString(request);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/budgets")
                        .with(user(userPrincipal))
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
        User userSaved = userService.save(
                User.builder()
                        .name("Anderson")
                        .email("anderson@gmail.com")
                        .password("Arthur@1406")
                        .role(UserRole.ROLE_USER)
                        .build()
        );

        Category categorySaved = categoryRepository.save(
                Category.builder()
                        .name("Food")
                        .description("Food description")
                        .icon("food")
                        .owner(userSaved)
                        .build()
        );

        UserPrincipal userPrincipal = new UserPrincipal(userSaved);

        BudgetRequestDTO request = new BudgetRequestDTO(
                categorySaved.getId(),
                BudgetType.MONTHLY,
                new BigDecimal("0")
        );

        String requestJson = objectMapper.writeValueAsString(request);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/budgets")
                        .with(user(userPrincipal))
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
    @DisplayName("Should return budget when budget exists")
    void findById_WhenBudgetExists_ShouldReturnBudget() throws Exception {
        User userSaved = userService.save(
                User.builder()
                        .name("Anderson")
                        .email("anderson@gmail.com")
                        .password("Arthur@1406")
                        .role(UserRole.ROLE_USER)
                        .build()
        );

        Category categorySaved = categoryRepository.save(
                Category.builder()
                        .name("Food")
                        .description("Food description")
                        .icon("food")
                        .owner(userSaved)
                        .build()
        );

        Budget budgetSaved = budgetRepository.save(
                Budget.builder()
                        .owner(userSaved)
                        .category(categorySaved)
                        .budgetType(BudgetType.MONTHLY)
                        .limitAmount(new BigDecimal("1500.00"))
                        .build()
        );

        UserPrincipal userPrincipal = new UserPrincipal(userSaved);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/budgets/" + budgetSaved.getId())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        String content = result.getResponse().getContentAsString();

        BudgetResponseDTO response = objectMapper.readValue(content, BudgetResponseDTO.class);

        assertNotNull(response);
        assertEquals(userSaved.getId(), response.user().id());
        assertEquals(categorySaved.getId(), response.category().id());
        assertEquals(budgetSaved.getBudgetType(), response.budgetType());
        assertEquals(0, budgetSaved.getLimitAmount().compareTo(response.limitAmount()));
    }

    @Test
    @DisplayName("Should throw NotFoundException when budget does not exist")
    void findById_WhenBudgetDoesNotExist_ShouldThrowNotFoundException() throws Exception {
        User userSaved = userService.save(
                User.builder()
                        .name("Anderson")
                        .email("anderson@gmail.com")
                        .password("Arthur@1406")
                        .role(UserRole.ROLE_USER)
                        .build()
        );

        UserPrincipal userPrincipal = new UserPrincipal(userSaved);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/budgets/" + UUID.randomUUID())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        String content = result.getResponse().getContentAsString();

        StandardException exception = objectMapper.readValue(content, StandardException.class);

        assertNotNull(exception);
        assertEquals(HttpStatus.NOT_FOUND.value(), exception.status());
    }

    @Test
    @DisplayName("Should throw NotFoundException when budget belongs to another user")
    void findById_WhenBudgetBelongsToAnotherUser_ShouldThrowNotFoundException() throws Exception {
        User owner = userService.save(
                User.builder()
                        .name("Anderson")
                        .email("anderson@gmail.com")
                        .password("Arthur@1406")
                        .role(UserRole.ROLE_USER)
                        .build()
        );

        User anotherUser = userService.save(
                User.builder()
                        .name("Other")
                        .email("other@gmail.com")
                        .password("Other@1234")
                        .role(UserRole.ROLE_USER)
                        .build()
        );

        Category categorySaved = categoryRepository.save(
                Category.builder()
                        .name("Food")
                        .description("Food description")
                        .icon("food")
                        .owner(owner)
                        .build()
        );

        Budget budgetSaved = budgetRepository.save(
                Budget.builder()
                        .owner(owner)
                        .category(categorySaved)
                        .budgetType(BudgetType.MONTHLY)
                        .limitAmount(new BigDecimal("1500.00"))
                        .build()
        );

        UserPrincipal anotherUserPrincipal = new UserPrincipal(anotherUser);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/budgets/" + budgetSaved.getId())
                        .with(user(anotherUserPrincipal))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        String content = result.getResponse().getContentAsString();

        StandardException exception = objectMapper.readValue(content, StandardException.class);

        assertNotNull(exception);
        assertEquals(HttpStatus.NOT_FOUND.value(), exception.status());
    }

    @Test
    @DisplayName("Should update budget successfully when budget exists")
    void update_WhenBudgetExists_ShouldUpdateBudget() throws Exception {
        User userSaved = userService.save(
                User.builder()
                        .name("Anderson")
                        .email("anderson@gmail.com")
                        .password("Arthur@1406")
                        .role(UserRole.ROLE_USER)
                        .build()
        );

        Category categorySaved = categoryRepository.save(
                Category.builder()
                        .name("Food")
                        .description("Food description")
                        .icon("food")
                        .owner(userSaved)
                        .build()
        );

        Budget budgetSaved = budgetRepository.save(
                Budget.builder()
                        .owner(userSaved)
                        .category(categorySaved)
                        .budgetType(BudgetType.MONTHLY)
                        .limitAmount(new BigDecimal("1500.00"))
                        .build()
        );

        UserPrincipal userPrincipal = new UserPrincipal(userSaved);

        BudgetUpdateDTO update = new BudgetUpdateDTO(
                new BigDecimal("500.00"),
                BudgetType.WEEKLY,
                false
        );

        String updateJson = objectMapper.writeValueAsString(update);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.put("/budgets/" + budgetSaved.getId())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        String content = result.getResponse().getContentAsString();

        BudgetResponseDTO response = objectMapper.readValue(content, BudgetResponseDTO.class);

        assertEquals(update.budgetType(), response.budgetType());
        assertEquals(0, update.limitAmount().compareTo(response.limitAmount()));
        assertEquals(update.active(), response.active());
    }

    @Test
    @DisplayName("Should throw NotFoundException when budget does not exist")
    void update_WhenBudgetDoesNotExist_ShouldThrowNotFoundException() throws Exception {
        User userSaved = userService.save(
                User.builder()
                        .name("Anderson")
                        .email("anderson@gmail.com")
                        .password("Arthur@1406")
                        .role(UserRole.ROLE_USER)
                        .build()
        );

        UserPrincipal userPrincipal = new UserPrincipal(userSaved);

        BudgetUpdateDTO update = new BudgetUpdateDTO(
                new BigDecimal("500.00"),
                BudgetType.WEEKLY,
                false
        );

        String updateJson = objectMapper.writeValueAsString(update);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.put("/budgets/" + UUID.randomUUID())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        String content = result.getResponse().getContentAsString();

        StandardException exception = objectMapper.readValue(content, StandardException.class);

        assertNotNull(exception);
        assertEquals(HttpStatus.NOT_FOUND.value(), exception.status());
    }

    @Test
    @DisplayName("Should throw MethodArgumentNotValidException when field invalid")
    void update_ShouldThrowMethodArgumentNotValidException_WhenFieldInvalid() throws Exception {
        User userSaved = userService.save(
                User.builder()
                        .name("Anderson")
                        .email("anderson@gmail.com")
                        .password("Arthur@1406")
                        .role(UserRole.ROLE_USER)
                        .build()
        );

        Category categorySaved = categoryRepository.save(
                Category.builder()
                        .name("Food")
                        .description("Food description")
                        .icon("food")
                        .owner(userSaved)
                        .build()
        );

        Budget budgetSaved = budgetRepository.save(
                Budget.builder()
                        .owner(userSaved)
                        .category(categorySaved)
                        .budgetType(BudgetType.MONTHLY)
                        .limitAmount(new BigDecimal("1500.00"))
                        .build()
        );

        UserPrincipal userPrincipal = new UserPrincipal(userSaved);

        String updateJson = """
                {
                    "limitAmount": 500.00,
                    "budgetType": "WEEKLY"
                }
                """;

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.put("/budgets/" + budgetSaved.getId())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        String content = result.getResponse().getContentAsString();

        StandardException exception = objectMapper.readValue(content, StandardException.class);

        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.status());
    }
}
