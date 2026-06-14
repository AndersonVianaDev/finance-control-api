package com.andersonvianadev.finance_control_api.controllers;

import com.andersonvianadev.finance_control_api.controllers.dtos.requests.ExpenseRequestDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.ExpenseResponseDTO;
import com.andersonvianadev.finance_control_api.domain.models.Budget;
import com.andersonvianadev.finance_control_api.domain.models.Category;
import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.models.enums.BudgetType;
import com.andersonvianadev.finance_control_api.domain.models.enums.UserRole;
import com.andersonvianadev.finance_control_api.domain.services.IUserService;
import com.andersonvianadev.finance_control_api.infra.exceptions.StandardException;
import com.andersonvianadev.finance_control_api.infra.messaging.ISqsMessageSender;
import com.andersonvianadev.finance_control_api.infra.repositories.BudgetRepository;
import com.andersonvianadev.finance_control_api.infra.repositories.CategoryRepository;
import com.andersonvianadev.finance_control_api.infra.repositories.ExpenseRepository;
import com.andersonvianadev.finance_control_api.infra.repositories.IncomeRepository;
import com.andersonvianadev.finance_control_api.infra.repositories.InstallmentPlanRepository;
import com.andersonvianadev.finance_control_api.infra.repositories.RecurringRuleRepository;
import com.andersonvianadev.finance_control_api.infra.repositories.UserRepository;
import com.andersonvianadev.finance_control_api.infra.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@SpringBootTest
@AutoConfigureMockMvc
class ExpenseControllerTest {

    @TestConfiguration
    static class InfraTestConfig {

        @Bean
        @Primary
        public ISqsMessageSender sqsMessageSender() {
            return new ISqsMessageSender() {
                @Override
                public <T> void send(String queueUrl, T body) {}
            };
        }
    }

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
    private ExpenseRepository expenseRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private IncomeRepository incomeRepository;

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
        userRepository.deleteAll();
    }

    private User createUser(String email) {
        return userService.save(User.builder()
                .name("Anderson")
                .email(email)
                .password("Arthur@1406")
                .role(UserRole.ROLE_USER)
                .build());
    }

    private Category createCategory(User owner) {
        return categoryRepository.save(Category.builder()
                .name("Food")
                .description("Food and drinks")
                .icon("food")
                .owner(owner)
                .build());
    }

    @Test
    @DisplayName("Should register expense and return 201 when request is valid")
    void save_WhenRequestIsValid_ShouldReturnCreated() throws Exception {
        User user = createUser("anderson@gmail.com");
        Category category = createCategory(user);
        UserPrincipal principal = new UserPrincipal(user);

        ExpenseRequestDTO request = new ExpenseRequestDTO(
                LocalDateTime.of(2026, 7, 1, 10, 0),
                new BigDecimal("150.00"),
                "Monthly gym membership",
                category.getId()
        );

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/expenses")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ExpenseResponseDTO response = objectMapper.readValue(
                result.getResponse().getContentAsString(), ExpenseResponseDTO.class);

        assertNotNull(response.id());
        assertEquals(LocalDateTime.of(2026, 7, 1, 10, 0), response.transactionDate());
        assertEquals(0, new BigDecimal("150.00").compareTo(response.price()));
        assertEquals("Monthly gym membership", response.description());
        assertEquals(category.getId(), response.category().id());
        assertNull(response.installmentNumber());
        assertNull(response.totalInstallments());
        assertEquals(1, expenseRepository.count());
    }

    @Test
    @DisplayName("Should return 404 when category does not exist")
    void save_WhenCategoryDoesNotExist_ShouldReturnNotFound() throws Exception {
        User user = createUser("anderson@gmail.com");
        UserPrincipal principal = new UserPrincipal(user);

        ExpenseRequestDTO request = new ExpenseRequestDTO(
                LocalDateTime.of(2026, 7, 1, 10, 0),
                new BigDecimal("150.00"),
                "Monthly gym membership",
                UUID.randomUUID()
        );

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/expenses")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(), StandardException.class);

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.status());
        assertEquals(0, expenseRepository.count());
    }

    @Test
    @DisplayName("Should return 404 when category belongs to another user")
    void save_WhenCategoryBelongsToAnotherUser_ShouldReturnNotFound() throws Exception {
        User owner = createUser("anderson@gmail.com");
        User other = createUser("other@gmail.com");
        Category otherCategory = createCategory(other);
        UserPrincipal principal = new UserPrincipal(owner);

        ExpenseRequestDTO request = new ExpenseRequestDTO(
                LocalDateTime.of(2026, 7, 1, 10, 0),
                new BigDecimal("150.00"),
                "Monthly gym membership",
                otherCategory.getId()
        );

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/expenses")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(), StandardException.class);

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.status());
        assertEquals(0, expenseRepository.count());
    }

    @Test
    @DisplayName("Should return 409 when expense is a duplicate")
    void save_WhenDuplicateExpense_ShouldReturnConflict() throws Exception {
        User user = createUser("anderson@gmail.com");
        Category category = createCategory(user);
        UserPrincipal principal = new UserPrincipal(user);

        ExpenseRequestDTO request = new ExpenseRequestDTO(
                LocalDateTime.of(2026, 7, 1, 10, 0),
                new BigDecimal("150.00"),
                "Monthly gym membership",
                category.getId()
        );

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(MockMvcRequestBuilders.post("/expenses")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(MockMvcResultMatchers.status().isCreated());

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/expenses")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(), StandardException.class);

        assertEquals(HttpStatus.CONFLICT.value(), exception.status());
        assertEquals(1, expenseRepository.count());
    }

    @Test
    @DisplayName("Should return 422 when budget limit is exceeded")
    void save_WhenBudgetIsExceeded_ShouldReturnUnprocessableEntity() throws Exception {
        User user = createUser("anderson@gmail.com");
        Category category = createCategory(user);
        UserPrincipal principal = new UserPrincipal(user);

        budgetRepository.save(Budget.builder()
                .owner(user)
                .category(category)
                .budgetType(BudgetType.MONTHLY)
                .limitAmount(new BigDecimal("100.00"))
                .build());

        ExpenseRequestDTO request = new ExpenseRequestDTO(
                LocalDateTime.now(),
                new BigDecimal("200.00"),
                "Expensive item",
                category.getId()
        );

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/expenses")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isUnprocessableEntity())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(), StandardException.class);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), exception.status());
        assertEquals(0, expenseRepository.count());
    }

    @Test
    @DisplayName("Should bypass budget limit and return 201 when X-SKIP-BUDGET is true")
    void save_WhenSkipBudgetIsTrue_ShouldBypassBudgetLimit() throws Exception {
        User user = createUser("anderson@gmail.com");
        Category category = createCategory(user);
        UserPrincipal principal = new UserPrincipal(user);

        budgetRepository.save(Budget.builder()
                .owner(user)
                .category(category)
                .budgetType(BudgetType.MONTHLY)
                .limitAmount(new BigDecimal("100.00"))
                .build());

        ExpenseRequestDTO request = new ExpenseRequestDTO(
                LocalDateTime.now(),
                new BigDecimal("200.00"),
                "Expensive item",
                category.getId()
        );

        mockMvc.perform(MockMvcRequestBuilders.post("/expenses")
                        .with(user(principal))
                        .header("X-SKIP-BUDGET", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andDo(MockMvcResultHandlers.print());

        assertEquals(1, expenseRepository.count());
    }

    // ── findById ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return 200 with expense when ID exists and belongs to user")
    void findById_WhenExpenseExistsAndBelongsToUser_ShouldReturnOk() throws Exception {
        User user = createUser("anderson@gmail.com");
        Category category = createCategory(user);
        UserPrincipal principal = new UserPrincipal(user);

        ExpenseRequestDTO request = new ExpenseRequestDTO(
                LocalDateTime.of(2026, 7, 1, 10, 0),
                new BigDecimal("150.00"),
                "Monthly gym membership",
                category.getId()
        );

        MvcResult created = mockMvc.perform(MockMvcRequestBuilders.post("/expenses")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn();

        UUID expenseId = objectMapper.readValue(
                created.getResponse().getContentAsString(), ExpenseResponseDTO.class).id();

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/expenses/{id}", expenseId)
                        .with(user(principal)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ExpenseResponseDTO response = objectMapper.readValue(
                result.getResponse().getContentAsString(), ExpenseResponseDTO.class);

        assertEquals(expenseId, response.id());
        assertEquals(LocalDateTime.of(2026, 7, 1, 10, 0), response.transactionDate());
        assertEquals(0, new BigDecimal("150.00").compareTo(response.price()));
        assertEquals("Monthly gym membership", response.description());
        assertEquals(category.getId(), response.category().id());
    }

    @Test
    @DisplayName("Should return 404 when expense ID does not exist")
    void findById_WhenExpenseDoesNotExist_ShouldReturnNotFound() throws Exception {
        User user = createUser("anderson@gmail.com");
        UserPrincipal principal = new UserPrincipal(user);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/expenses/{id}", UUID.randomUUID())
                        .with(user(principal)))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(), StandardException.class);

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.status());
    }

    @Test
    @DisplayName("Should return 404 when expense belongs to another user")
    void findById_WhenExpenseBelongsToAnotherUser_ShouldReturnNotFound() throws Exception {
        User owner = createUser("anderson@gmail.com");
        User other = createUser("other@gmail.com");
        Category category = createCategory(owner);
        UserPrincipal ownerPrincipal = new UserPrincipal(owner);
        UserPrincipal otherPrincipal = new UserPrincipal(other);

        MvcResult created = mockMvc.perform(MockMvcRequestBuilders.post("/expenses")
                        .with(user(ownerPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ExpenseRequestDTO(
                                LocalDateTime.of(2026, 7, 1, 10, 0),
                                new BigDecimal("150.00"),
                                "Monthly gym membership",
                                category.getId()
                        ))))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn();

        UUID expenseId = objectMapper.readValue(
                created.getResponse().getContentAsString(), ExpenseResponseDTO.class).id();

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/expenses/{id}", expenseId)
                        .with(user(otherPrincipal)))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(), StandardException.class);

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.status());
    }

    @Test
    @DisplayName("Should return 403 when request is unauthenticated")
    void findById_WhenUnauthenticated_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/expenses/{id}", UUID.randomUUID()))
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andDo(MockMvcResultHandlers.print());
    }
}
