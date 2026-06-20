package com.andersonvianadev.finance_control_api.controllers;

import com.andersonvianadev.finance_control_api.controllers.dtos.requests.ExpenseRequestDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.requests.ExpenseUpdateDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.ExpenseResponseDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.PageResponseDTO;
import tools.jackson.databind.JavaType;
import com.andersonvianadev.finance_control_api.domain.models.Budget;
import com.andersonvianadev.finance_control_api.domain.models.Category;
import com.andersonvianadev.finance_control_api.domain.models.Expense;
import com.andersonvianadev.finance_control_api.domain.models.InstallmentPlan;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    // ── findAll ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return paginated expenses for authenticated user")
    void findAll_WhenUserHasExpenses_ShouldReturnPaginatedResults() throws Exception {
        User user = createUser("anderson@gmail.com");
        Category category = createCategory(user);
        UserPrincipal principal = new UserPrincipal(user);

        for (int i = 1; i <= 3; i++) {
            mockMvc.perform(MockMvcRequestBuilders.post("/expenses")
                            .with(user(principal))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ExpenseRequestDTO(
                                    LocalDateTime.of(2026, 7, i, 10, 0),
                                    new BigDecimal("100.00"),
                                    "Expense " + i,
                                    category.getId()
                            ))))
                    .andExpect(MockMvcResultMatchers.status().isCreated());
        }

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/expenses")
                        .with(user(principal))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        JavaType pageType = objectMapper.getTypeFactory()
                .constructParametricType(PageResponseDTO.class, ExpenseResponseDTO.class);
        PageResponseDTO<ExpenseResponseDTO> response = objectMapper.readValue(
                result.getResponse().getContentAsString(), pageType);

        assertEquals(3, response.totalElement());
        assertEquals(3, response.content().size());
        assertEquals(0, response.page());
        assertEquals(1, response.totalPages());
        assertTrue(response.last());
    }

    @Test
    @DisplayName("Should return empty page when user has no expenses")
    void findAll_WhenUserHasNoExpenses_ShouldReturnEmptyPage() throws Exception {
        User user = createUser("anderson@gmail.com");
        UserPrincipal principal = new UserPrincipal(user);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/expenses")
                        .with(user(principal)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        JavaType pageType = objectMapper.getTypeFactory()
                .constructParametricType(PageResponseDTO.class, ExpenseResponseDTO.class);
        PageResponseDTO<ExpenseResponseDTO> response = objectMapper.readValue(
                result.getResponse().getContentAsString(), pageType);

        assertEquals(0, response.totalElement());
        assertTrue(response.content().isEmpty());
        assertTrue(response.last());
    }

    @Test
    @DisplayName("Should respect page size and return correct page")
    void findAll_WhenPageSizeIsApplied_ShouldReturnCorrectPage() throws Exception {
        User user = createUser("anderson@gmail.com");
        Category category = createCategory(user);
        UserPrincipal principal = new UserPrincipal(user);

        for (int i = 1; i <= 3; i++) {
            mockMvc.perform(MockMvcRequestBuilders.post("/expenses")
                            .with(user(principal))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ExpenseRequestDTO(
                                    LocalDateTime.of(2026, 7, i, 10, 0),
                                    new BigDecimal("100.00"),
                                    "Expense " + i,
                                    category.getId()
                            ))))
                    .andExpect(MockMvcResultMatchers.status().isCreated());
        }

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/expenses")
                        .with(user(principal))
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        JavaType pageType = objectMapper.getTypeFactory()
                .constructParametricType(PageResponseDTO.class, ExpenseResponseDTO.class);
        PageResponseDTO<ExpenseResponseDTO> response = objectMapper.readValue(
                result.getResponse().getContentAsString(), pageType);

        assertEquals(3, response.totalElement());
        assertEquals(2, response.content().size());
        assertEquals(2, response.totalPages());
        assertFalse(response.last());
    }

    @Test
    @DisplayName("Should return only expenses of the authenticated user")
    void findAll_ShouldOnlyReturnExpensesOfAuthenticatedUser() throws Exception {
        User owner = createUser("anderson@gmail.com");
        User other = createUser("other@gmail.com");
        Category ownerCategory = createCategory(owner);
        Category otherCategory = createCategory(other);
        UserPrincipal ownerPrincipal = new UserPrincipal(owner);
        UserPrincipal otherPrincipal = new UserPrincipal(other);

        for (int i = 1; i <= 2; i++) {
            mockMvc.perform(MockMvcRequestBuilders.post("/expenses")
                            .with(user(ownerPrincipal))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ExpenseRequestDTO(
                                    LocalDateTime.of(2026, 7, i, 10, 0),
                                    new BigDecimal("100.00"),
                                    "Owner expense " + i,
                                    ownerCategory.getId()
                            ))))
                    .andExpect(MockMvcResultMatchers.status().isCreated());
        }

        mockMvc.perform(MockMvcRequestBuilders.post("/expenses")
                        .with(user(otherPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ExpenseRequestDTO(
                                LocalDateTime.of(2026, 7, 5, 10, 0),
                                new BigDecimal("200.00"),
                                "Other user expense",
                                otherCategory.getId()
                        ))))
                .andExpect(MockMvcResultMatchers.status().isCreated());

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/expenses")
                        .with(user(ownerPrincipal)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        JavaType pageType = objectMapper.getTypeFactory()
                .constructParametricType(PageResponseDTO.class, ExpenseResponseDTO.class);
        PageResponseDTO<ExpenseResponseDTO> response = objectMapper.readValue(
                result.getResponse().getContentAsString(), pageType);

        assertEquals(2, response.totalElement());
        assertEquals(2, response.content().size());
    }

    @Test
    @DisplayName("Should return 403 when findAll request is unauthenticated")
    void findAll_WhenUnauthenticated_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/expenses"))
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andDo(MockMvcResultHandlers.print());
    }

    // ── delete ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should delete expense and return 204 when expense exists and belongs to user")
    void delete_WhenExpenseExistsAndBelongsToUser_ShouldReturn204() throws Exception {
        User user = createUser("anderson@gmail.com");
        Category category = createCategory(user);
        UserPrincipal principal = new UserPrincipal(user);

        MvcResult created = mockMvc.perform(MockMvcRequestBuilders.post("/expenses")
                        .with(user(principal))
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

        mockMvc.perform(MockMvcRequestBuilders.delete("/expenses/{id}", expenseId)
                        .with(user(principal)))
                .andExpect(MockMvcResultMatchers.status().isNoContent())
                .andDo(MockMvcResultHandlers.print());

        assertEquals(0, expenseRepository.count());
    }

    @Test
    @DisplayName("Should return 404 when expense does not exist")
    void delete_WhenExpenseDoesNotExist_ShouldReturn404() throws Exception {
        User user = createUser("anderson@gmail.com");
        UserPrincipal principal = new UserPrincipal(user);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.delete("/expenses/{id}", UUID.randomUUID())
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
    void delete_WhenExpenseBelongsToAnotherUser_ShouldReturn404() throws Exception {
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

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.delete("/expenses/{id}", expenseId)
                        .with(user(otherPrincipal)))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(), StandardException.class);

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.status());
        assertEquals(1, expenseRepository.count());
    }

    @Test
    @DisplayName("Should return 422 when expense is linked to an installment plan")
    void delete_WhenExpenseIsLinkedToInstallmentPlan_ShouldReturn422() throws Exception {
        User user = createUser("anderson@gmail.com");
        Category category = createCategory(user);
        UserPrincipal principal = new UserPrincipal(user);

        InstallmentPlan plan = installmentPlanRepository.save(InstallmentPlan.builder()
                .owner(user)
                .category(category)
                .description("MacBook Pro 12x")
                .totalAmount(new BigDecimal("3600.00"))
                .installmentAmount(new BigDecimal("300.00"))
                .totalInstallments(12)
                .firstDueDate(java.time.LocalDate.of(2026, 7, 1))
                .build());

        Expense installmentExpense = expenseRepository.save(Expense.builder()
                .owner(user)
                .category(category)
                .installmentPlan(plan)
                .transactionDate(LocalDateTime.of(2026, 7, 1, 0, 0))
                .price(new BigDecimal("300.00"))
                .description("MacBook Pro 12x")
                .installmentNumber(1)
                .build());

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.delete("/expenses/{id}", installmentExpense.getId())
                        .with(user(principal)))
                .andExpect(MockMvcResultMatchers.status().isUnprocessableEntity())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(), StandardException.class);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), exception.status());
        assertEquals(1, expenseRepository.count());
    }

    @Test
    @DisplayName("Should return 403 when delete request is unauthenticated")
    void delete_WhenUnauthenticated_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/expenses/{id}", UUID.randomUUID()))
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andDo(MockMvcResultHandlers.print());
    }

    // ── update ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return 200 with updated fields when request is valid")
    void update_WhenRequestIsValid_ShouldReturn200WithUpdatedExpense() throws Exception {
        User user = createUser("anderson@gmail.com");
        Category category = createCategory(user);
        UserPrincipal principal = new UserPrincipal(user);

        MvcResult created = mockMvc.perform(MockMvcRequestBuilders.post("/expenses")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ExpenseRequestDTO(
                                LocalDateTime.of(2026, 7, 1, 10, 0),
                                new BigDecimal("100.00"),
                                "Old description",
                                category.getId()
                        ))))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn();

        UUID expenseId = objectMapper.readValue(
                created.getResponse().getContentAsString(), ExpenseResponseDTO.class).id();

        ExpenseUpdateDTO updateRequest = new ExpenseUpdateDTO(
                new BigDecimal("175.00"), "Updated description", null, null);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.put("/expenses/{id}", expenseId)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ExpenseResponseDTO response = objectMapper.readValue(
                result.getResponse().getContentAsString(), ExpenseResponseDTO.class);

        assertEquals(expenseId, response.id());
        assertEquals(0, new BigDecimal("175.00").compareTo(response.price()));
        assertEquals("Updated description", response.description());
        assertEquals(category.getId(), response.category().id());
    }

    @Test
    @DisplayName("Should return 200 with new category when category is changed")
    void update_WhenCategoryChanged_ShouldReturn200WithNewCategory() throws Exception {
        User user = createUser("anderson@gmail.com");
        Category categoryA = createCategory(user);
        Category categoryB = categoryRepository.save(Category.builder()
                .name("Tech").description("Electronics").icon("tech").owner(user).build());
        UserPrincipal principal = new UserPrincipal(user);

        MvcResult created = mockMvc.perform(MockMvcRequestBuilders.post("/expenses")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ExpenseRequestDTO(
                                LocalDateTime.of(2026, 7, 1, 10, 0),
                                new BigDecimal("100.00"),
                                "Some expense",
                                categoryA.getId()
                        ))))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn();

        UUID expenseId = objectMapper.readValue(
                created.getResponse().getContentAsString(), ExpenseResponseDTO.class).id();

        ExpenseUpdateDTO updateRequest = new ExpenseUpdateDTO(null, null, null, categoryB.getId());

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.put("/expenses/{id}", expenseId)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        ExpenseResponseDTO response = objectMapper.readValue(
                result.getResponse().getContentAsString(), ExpenseResponseDTO.class);

        assertEquals(categoryB.getId(), response.category().id());
    }

    @Test
    @DisplayName("Should return 404 when expense to update does not exist")
    void update_WhenExpenseDoesNotExist_ShouldReturn404() throws Exception {
        User user = createUser("anderson@gmail.com");
        UserPrincipal principal = new UserPrincipal(user);

        ExpenseUpdateDTO updateRequest = new ExpenseUpdateDTO(
                new BigDecimal("175.00"), null, null, null);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.put("/expenses/{id}", UUID.randomUUID())
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(), StandardException.class);

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.status());
    }

    @Test
    @DisplayName("Should return 404 when expense to update belongs to another user")
    void update_WhenExpenseBelongsToAnotherUser_ShouldReturn404() throws Exception {
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
                                new BigDecimal("100.00"),
                                "Some expense",
                                category.getId()
                        ))))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn();

        UUID expenseId = objectMapper.readValue(
                created.getResponse().getContentAsString(), ExpenseResponseDTO.class).id();

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.put("/expenses/{id}", expenseId)
                        .with(user(otherPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ExpenseUpdateDTO(null, "Hacked", null, null))))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(), StandardException.class);

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.status());
    }

    @Test
    @DisplayName("Should return 404 when new category does not exist")
    void update_WhenNewCategoryDoesNotExist_ShouldReturn404() throws Exception {
        User user = createUser("anderson@gmail.com");
        Category category = createCategory(user);
        UserPrincipal principal = new UserPrincipal(user);

        MvcResult created = mockMvc.perform(MockMvcRequestBuilders.post("/expenses")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ExpenseRequestDTO(
                                LocalDateTime.of(2026, 7, 1, 10, 0),
                                new BigDecimal("100.00"),
                                "Some expense",
                                category.getId()
                        ))))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn();

        UUID expenseId = objectMapper.readValue(
                created.getResponse().getContentAsString(), ExpenseResponseDTO.class).id();

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.put("/expenses/{id}", expenseId)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ExpenseUpdateDTO(null, null, null, UUID.randomUUID()))))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(), StandardException.class);

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.status());
    }

    @Test
    @DisplayName("Should return 409 when updated values conflict with another existing expense")
    void update_WhenResultIsDuplicate_ShouldReturn409() throws Exception {
        User user = createUser("anderson@gmail.com");
        Category category = createCategory(user);
        UserPrincipal principal = new UserPrincipal(user);

        mockMvc.perform(MockMvcRequestBuilders.post("/expenses")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ExpenseRequestDTO(
                                LocalDateTime.of(2026, 7, 1, 10, 0),
                                new BigDecimal("150.00"),
                                "Monthly gym membership",
                                category.getId()
                        ))))
                .andExpect(MockMvcResultMatchers.status().isCreated());

        MvcResult created2 = mockMvc.perform(MockMvcRequestBuilders.post("/expenses")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ExpenseRequestDTO(
                                LocalDateTime.of(2026, 7, 2, 10, 0),
                                new BigDecimal("200.00"),
                                "Different expense",
                                category.getId()
                        ))))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn();

        UUID expense2Id = objectMapper.readValue(
                created2.getResponse().getContentAsString(), ExpenseResponseDTO.class).id();

        ExpenseUpdateDTO updateRequest = new ExpenseUpdateDTO(
                new BigDecimal("150.00"),
                "Monthly gym membership",
                LocalDateTime.of(2026, 7, 1, 10, 0),
                null
        );

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.put("/expenses/{id}", expense2Id)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(), StandardException.class);

        assertEquals(HttpStatus.CONFLICT.value(), exception.status());
    }

    @Test
    @DisplayName("Should return 422 when expense is linked to an installment plan")
    void update_WhenExpenseIsLinkedToInstallmentPlan_ShouldReturn422() throws Exception {
        User user = createUser("anderson@gmail.com");
        Category category = createCategory(user);
        UserPrincipal principal = new UserPrincipal(user);

        InstallmentPlan plan = installmentPlanRepository.save(InstallmentPlan.builder()
                .owner(user).category(category)
                .description("MacBook Pro 12x")
                .totalAmount(new BigDecimal("3600.00"))
                .installmentAmount(new BigDecimal("300.00"))
                .totalInstallments(12)
                .firstDueDate(java.time.LocalDate.of(2026, 7, 1))
                .build());

        Expense installmentExpense = expenseRepository.save(Expense.builder()
                .owner(user).category(category).installmentPlan(plan)
                .transactionDate(LocalDateTime.of(2026, 7, 1, 0, 0))
                .price(new BigDecimal("300.00"))
                .description("MacBook Pro 12x")
                .installmentNumber(1)
                .build());

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.put("/expenses/{id}", installmentExpense.getId())
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ExpenseUpdateDTO(new BigDecimal("250.00"), null, null, null))))
                .andExpect(MockMvcResultMatchers.status().isUnprocessableEntity())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(), StandardException.class);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), exception.status());
    }

    @Test
    @DisplayName("Should return 422 when price increase exceeds budget limit")
    void update_WhenPriceIncreasedAndBudgetIsExceeded_ShouldReturn422() throws Exception {
        User user = createUser("anderson@gmail.com");
        Category category = createCategory(user);
        UserPrincipal principal = new UserPrincipal(user);

        budgetRepository.save(Budget.builder()
                .owner(user).category(category)
                .budgetType(BudgetType.MONTHLY)
                .limitAmount(new BigDecimal("100.00"))
                .build());

        MvcResult created = mockMvc.perform(MockMvcRequestBuilders.post("/expenses")
                        .with(user(principal))
                        .header("X-SKIP-BUDGET", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ExpenseRequestDTO(
                                LocalDateTime.now(),
                                new BigDecimal("80.00"),
                                "Initial expense",
                                category.getId()
                        ))))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn();

        UUID expenseId = objectMapper.readValue(
                created.getResponse().getContentAsString(), ExpenseResponseDTO.class).id();

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.put("/expenses/{id}", expenseId)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ExpenseUpdateDTO(new BigDecimal("200.00"), null, null, null))))
                .andExpect(MockMvcResultMatchers.status().isUnprocessableEntity())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(), StandardException.class);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), exception.status());
    }

    @Test
    @DisplayName("Should return 200 when X-SKIP-BUDGET is true even if update would exceed budget")
    void update_WhenSkipBudgetIsTrue_ShouldBypassBudgetAndReturn200() throws Exception {
        User user = createUser("anderson@gmail.com");
        Category category = createCategory(user);
        UserPrincipal principal = new UserPrincipal(user);

        budgetRepository.save(Budget.builder()
                .owner(user).category(category)
                .budgetType(BudgetType.MONTHLY)
                .limitAmount(new BigDecimal("100.00"))
                .build());

        MvcResult created = mockMvc.perform(MockMvcRequestBuilders.post("/expenses")
                        .with(user(principal))
                        .header("X-SKIP-BUDGET", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ExpenseRequestDTO(
                                LocalDateTime.now(),
                                new BigDecimal("80.00"),
                                "Initial expense",
                                category.getId()
                        ))))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn();

        UUID expenseId = objectMapper.readValue(
                created.getResponse().getContentAsString(), ExpenseResponseDTO.class).id();

        mockMvc.perform(MockMvcRequestBuilders.put("/expenses/{id}", expenseId)
                        .with(user(principal))
                        .header("X-SKIP-BUDGET", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ExpenseUpdateDTO(new BigDecimal("200.00"), null, null, null))))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print());
    }

    @Test
    @DisplayName("Should return 403 when update request is unauthenticated")
    void update_WhenUnauthenticated_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/expenses/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andDo(MockMvcResultHandlers.print());
    }

    @Test
    @DisplayName("Should return 400 when price is zero")
    void update_WhenPriceIsZero_ShouldReturn400() throws Exception {
        User user = createUser("anderson@gmail.com");
        UserPrincipal principal = new UserPrincipal(user);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.put("/expenses/{id}", UUID.randomUUID())
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ExpenseUpdateDTO(BigDecimal.ZERO, null, null, null))))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(), StandardException.class);

        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.status());
    }

    @Test
    @DisplayName("Should return 400 when price is negative")
    void update_WhenPriceIsNegative_ShouldReturn400() throws Exception {
        User user = createUser("anderson@gmail.com");
        UserPrincipal principal = new UserPrincipal(user);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.put("/expenses/{id}", UUID.randomUUID())
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ExpenseUpdateDTO(new BigDecimal("-50.00"), null, null, null))))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(), StandardException.class);

        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.status());
    }

    // ── findBetweenTransactionDate ───────────────────────────────────────────

    @Test
    @DisplayName("Should return only expenses whose transactionDate falls within the given date range")
    void findBetweenTransactionDate_WhenExpensesExistInRange_ShouldReturnOnlyMatchingExpenses() throws Exception {
        User user = createUser("anderson@gmail.com");
        Category category = createCategory(user);
        UserPrincipal principal = new UserPrincipal(user);

        for (int i = 1; i <= 2; i++) {
            mockMvc.perform(MockMvcRequestBuilders.post("/expenses")
                            .with(user(principal))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ExpenseRequestDTO(
                                    LocalDateTime.of(2026, 6, i * 10, 10, 0),
                                    new BigDecimal("100.00"),
                                    "Expense in range " + i,
                                    category.getId()
                            ))))
                    .andExpect(MockMvcResultMatchers.status().isCreated());
        }

        mockMvc.perform(MockMvcRequestBuilders.post("/expenses")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ExpenseRequestDTO(
                                LocalDateTime.of(2026, 8, 1, 10, 0),
                                new BigDecimal("200.00"),
                                "Expense out of range",
                                category.getId()
                        ))))
                .andExpect(MockMvcResultMatchers.status().isCreated());

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/expenses")
                        .param("start", "2026-06-01")
                        .param("finish", "2026-06-30")
                        .with(user(principal)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        JavaType pageType = objectMapper.getTypeFactory()
                .constructParametricType(PageResponseDTO.class, ExpenseResponseDTO.class);
        PageResponseDTO<ExpenseResponseDTO> response = objectMapper.readValue(
                result.getResponse().getContentAsString(), pageType);

        assertEquals(2, response.totalElement());
        assertEquals(2, response.content().size());
    }

    @Test
    @DisplayName("Should include expenses registered at any hour on the finish date")
    void findBetweenTransactionDate_WhenFinishDayIsFullyInclusive_ShouldIncludeExpensesAtAnyHour() throws Exception {
        User user = createUser("anderson@gmail.com");
        Category category = createCategory(user);
        UserPrincipal principal = new UserPrincipal(user);

        mockMvc.perform(MockMvcRequestBuilders.post("/expenses")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ExpenseRequestDTO(
                                LocalDateTime.of(2026, 6, 30, 22, 45),
                                new BigDecimal("100.00"),
                                "Late night expense on finish day",
                                category.getId()
                        ))))
                .andExpect(MockMvcResultMatchers.status().isCreated());

        mockMvc.perform(MockMvcRequestBuilders.post("/expenses")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ExpenseRequestDTO(
                                LocalDateTime.of(2026, 7, 1, 9, 0),
                                new BigDecimal("100.00"),
                                "Expense the day after finish",
                                category.getId()
                        ))))
                .andExpect(MockMvcResultMatchers.status().isCreated());

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/expenses")
                        .param("start", "2026-06-01")
                        .param("finish", "2026-06-30")
                        .with(user(principal)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        JavaType pageType = objectMapper.getTypeFactory()
                .constructParametricType(PageResponseDTO.class, ExpenseResponseDTO.class);
        PageResponseDTO<ExpenseResponseDTO> response = objectMapper.readValue(
                result.getResponse().getContentAsString(), pageType);

        assertEquals(1, response.totalElement());
        assertEquals("Late night expense on finish day", response.content().get(0).description());
    }

    @Test
    @DisplayName("Should return empty page when no expenses fall within the given date range")
    void findBetweenTransactionDate_WhenNoExpensesInRange_ShouldReturnEmptyPage() throws Exception {
        User user = createUser("anderson@gmail.com");
        Category category = createCategory(user);
        UserPrincipal principal = new UserPrincipal(user);

        mockMvc.perform(MockMvcRequestBuilders.post("/expenses")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ExpenseRequestDTO(
                                LocalDateTime.of(2026, 9, 1, 10, 0),
                                new BigDecimal("100.00"),
                                "Expense outside range",
                                category.getId()
                        ))))
                .andExpect(MockMvcResultMatchers.status().isCreated());

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/expenses")
                        .param("start", "2026-06-01")
                        .param("finish", "2026-06-30")
                        .with(user(principal)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        JavaType pageType = objectMapper.getTypeFactory()
                .constructParametricType(PageResponseDTO.class, ExpenseResponseDTO.class);
        PageResponseDTO<ExpenseResponseDTO> response = objectMapper.readValue(
                result.getResponse().getContentAsString(), pageType);

        assertEquals(0, response.totalElement());
        assertTrue(response.content().isEmpty());
    }

    @Test
    @DisplayName("Should not return expenses belonging to another user even if they fall within the range")
    void findBetweenTransactionDate_WhenExpensesBelongToAnotherUser_ShouldReturnEmptyPage() throws Exception {
        User owner = createUser("anderson@gmail.com");
        User other = createUser("other@gmail.com");
        Category ownerCategory = createCategory(owner);
        Category otherCategory = createCategory(other);
        UserPrincipal ownerPrincipal = new UserPrincipal(owner);
        UserPrincipal otherPrincipal = new UserPrincipal(other);

        mockMvc.perform(MockMvcRequestBuilders.post("/expenses")
                        .with(user(ownerPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ExpenseRequestDTO(
                                LocalDateTime.of(2026, 6, 15, 10, 0),
                                new BigDecimal("100.00"),
                                "Owner expense in range",
                                ownerCategory.getId()
                        ))))
                .andExpect(MockMvcResultMatchers.status().isCreated());

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/expenses")
                        .param("start", "2026-06-01")
                        .param("finish", "2026-06-30")
                        .with(user(otherPrincipal)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        JavaType pageType = objectMapper.getTypeFactory()
                .constructParametricType(PageResponseDTO.class, ExpenseResponseDTO.class);
        PageResponseDTO<ExpenseResponseDTO> response = objectMapper.readValue(
                result.getResponse().getContentAsString(), pageType);

        assertEquals(0, response.totalElement());
        assertTrue(response.content().isEmpty());
    }

    @Test
    @DisplayName("Should return 400 when description is empty string")
    void update_WhenDescriptionIsEmpty_ShouldReturn400() throws Exception {
        User user = createUser("anderson@gmail.com");
        UserPrincipal principal = new UserPrincipal(user);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.put("/expenses/{id}", UUID.randomUUID())
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ExpenseUpdateDTO(null, "", null, null))))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(), StandardException.class);

        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.status());
    }
}
