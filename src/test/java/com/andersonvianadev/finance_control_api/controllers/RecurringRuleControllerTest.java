package com.andersonvianadev.finance_control_api.controllers;

import com.andersonvianadev.finance_control_api.controllers.dtos.requests.RecurringRuleRequestDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.requests.RecurringRuleUpdateDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.PageResponseDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.RecurringRuleResponseDTO;
import com.andersonvianadev.finance_control_api.domain.models.Category;
import com.andersonvianadev.finance_control_api.domain.models.RecurringRule;
import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.models.enums.RecurringType;
import com.andersonvianadev.finance_control_api.domain.models.enums.TransactionPeriodType;
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
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@SpringBootTest
@AutoConfigureMockMvc
class RecurringRuleControllerTest {

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
    private RecurringRuleRepository recurringRuleRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private IncomeRepository incomeRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private InstallmentPlanRepository installmentPlanRepository;

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

    @Test
    @DisplayName("Should create monthly income rule successfully")
    void save_WhenMonthlyIncomeRuleIsValid_ShouldReturn201() throws Exception {
        User user = createUser();
        Category category = createCategory(user, "salary");
        UserPrincipal principal = new UserPrincipal(user);

        RecurringRuleRequestDTO request = new RecurringRuleRequestDTO(
                LocalDateTime.of(2026, 6, 5, 0, 0),
                new BigDecimal("5000.00"),
                "Monthly salary",
                category.getId(),
                TransactionPeriodType.MONTHLY,
                RecurringType.INCOME
        );

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/recurring-rule")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        RecurringRuleResponseDTO response = objectMapper.readValue(
                result.getResponse().getContentAsString(), RecurringRuleResponseDTO.class);

        assertNotNull(response.id());
        assertEquals(request.description(), response.description());
        assertEquals(0, request.price().compareTo(response.price()));
        assertEquals(TransactionPeriodType.MONTHLY, response.transactionPeriodType());
        assertEquals(RecurringType.INCOME, response.recurringType());
        assertTrue(response.isActive());
    }

    @Test
    @DisplayName("Should create weekly expense rule successfully")
    void save_WhenWeeklyExpenseRuleIsValid_ShouldReturn201() throws Exception {
        User user = createUser();
        Category category = createCategory(user, "gym");
        UserPrincipal principal = new UserPrincipal(user);

        RecurringRuleRequestDTO request = new RecurringRuleRequestDTO(
                LocalDateTime.of(2026, 6, 8, 0, 0), // Monday
                new BigDecimal("150.00"),
                "Weekly gym membership",
                category.getId(),
                TransactionPeriodType.WEEKLY,
                RecurringType.EXPENSE
        );

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/recurring-rule")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        RecurringRuleResponseDTO response = objectMapper.readValue(
                result.getResponse().getContentAsString(), RecurringRuleResponseDTO.class);

        assertNotNull(response.id());
        assertEquals(TransactionPeriodType.WEEKLY, response.transactionPeriodType());
        assertEquals(RecurringType.EXPENSE, response.recurringType());
    }

    @Test
    @DisplayName("Should return 409 when a duplicate recurring rule is submitted")
    void save_WhenRuleAlreadyExists_ShouldReturn409() throws Exception {
        User user = createUser();
        Category category = createCategory(user, "salary");
        UserPrincipal principal = new UserPrincipal(user);

        RecurringRuleRequestDTO request = new RecurringRuleRequestDTO(
                LocalDateTime.of(2026, 6, 5, 0, 0),
                new BigDecimal("5000.00"),
                "Monthly salary",
                category.getId(),
                TransactionPeriodType.MONTHLY,
                RecurringType.INCOME
        );

        String body = objectMapper.writeValueAsString(request);

        mockMvc.perform(MockMvcRequestBuilders.post("/recurring-rule")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(MockMvcResultMatchers.status().isCreated());

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/recurring-rule")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(), StandardException.class);

        assertEquals(HttpStatus.CONFLICT.value(), exception.status());
    }

    @Test
    @DisplayName("Should return 404 when category does not exist")
    void save_WhenCategoryNotFound_ShouldReturn404() throws Exception {
        User user = createUser();
        UserPrincipal principal = new UserPrincipal(user);

        RecurringRuleRequestDTO request = new RecurringRuleRequestDTO(
                LocalDateTime.of(2026, 6, 5, 0, 0),
                new BigDecimal("5000.00"),
                "Monthly salary",
                UUID.randomUUID(),
                TransactionPeriodType.MONTHLY,
                RecurringType.INCOME
        );

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/recurring-rule")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(), StandardException.class);

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.status());
    }

    @Test
    @DisplayName("Should return 400 when required fields are missing")
    void save_WhenRequiredFieldsAreMissing_ShouldReturn400() throws Exception {
        User user = createUser();
        UserPrincipal principal = new UserPrincipal(user);

        String bodyMissingPrice = """
                {
                    "transactionDate": "2026-06-05T00:00:00",
                    "description": "Monthly salary",
                    "categoryId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                    "transactionPeriodType": "MONTHLY",
                    "recurringType": "INCOME"
                }
                """;

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/recurring-rule")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyMissingPrice))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(), StandardException.class);

        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.status());
    }

    @Test
    @DisplayName("Should return 400 when price is negative")
    void save_WhenPriceIsNegative_ShouldReturn400() throws Exception {
        User user = createUser();
        Category category = createCategory(user, "salary");
        UserPrincipal principal = new UserPrincipal(user);

        RecurringRuleRequestDTO request = new RecurringRuleRequestDTO(
                LocalDateTime.of(2026, 6, 5, 0, 0),
                new BigDecimal("-100.00"),
                "Monthly salary",
                category.getId(),
                TransactionPeriodType.MONTHLY,
                RecurringType.INCOME
        );

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/recurring-rule")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(), StandardException.class);

        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.status());
    }

    @Test
    @DisplayName("Should return 403 when request is unauthenticated")
    void save_WhenUnauthenticated_ShouldReturn403() throws Exception {
        RecurringRuleRequestDTO request = new RecurringRuleRequestDTO(
                LocalDateTime.of(2026, 6, 5, 0, 0),
                new BigDecimal("5000.00"),
                "Monthly salary",
                UUID.randomUUID(),
                TransactionPeriodType.MONTHLY,
                RecurringType.INCOME
        );

        mockMvc.perform(MockMvcRequestBuilders.post("/recurring-rule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andDo(MockMvcResultHandlers.print());
    }

    // ── findAll ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return only rules owned by the authenticated user")
    void findAll_WhenRulesExist_ShouldReturnOwnedRulesOnly() throws Exception {
        User user = createUser();
        Category category = createCategory(user, "salary");
        UserPrincipal principal = new UserPrincipal(user);
        createRecurringRule(user, category, TransactionPeriodType.MONTHLY, RecurringType.INCOME);
        createRecurringRule(user, category, TransactionPeriodType.WEEKLY, RecurringType.EXPENSE);

        User otherUser = userService.save(User.builder()
                .name("Other").email("other@gmail.com").password("Arthur@1406").role(UserRole.ROLE_USER).build());
        Category otherCategory = createCategory(otherUser, "other");
        createRecurringRule(otherUser, otherCategory, TransactionPeriodType.MONTHLY, RecurringType.INCOME);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/recurring-rule")
                        .with(user(principal)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        PageResponseDTO response = objectMapper.readValue(
                result.getResponse().getContentAsString(), PageResponseDTO.class);

        assertEquals(2, response.totalElement());
    }

    @Test
    @DisplayName("Should return empty page when user has no rules")
    void findAll_WhenNoRules_ShouldReturnEmptyPage() throws Exception {
        User user = createUser();
        UserPrincipal principal = new UserPrincipal(user);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/recurring-rule")
                        .with(user(principal)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        PageResponseDTO response = objectMapper.readValue(
                result.getResponse().getContentAsString(), PageResponseDTO.class);

        assertEquals(0, response.totalElement());
    }

    // ── delete ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should delete rule and return 204 when it exists and belongs to the user")
    void delete_WhenRuleExistsAndBelongsToUser_ShouldReturn204() throws Exception {
        User user = createUser();
        Category category = createCategory(user, "salary");
        UserPrincipal principal = new UserPrincipal(user);
        RecurringRule rule = createRecurringRule(user, category, TransactionPeriodType.MONTHLY, RecurringType.INCOME);

        mockMvc.perform(MockMvcRequestBuilders.delete("/recurring-rule/{id}", rule.getId())
                        .with(user(principal)))
                .andExpect(MockMvcResultMatchers.status().isNoContent())
                .andDo(MockMvcResultHandlers.print());
    }

    @Test
    @DisplayName("Should return 404 when rule to delete does not exist")
    void delete_WhenRuleNotFound_ShouldReturn404() throws Exception {
        User user = createUser();
        UserPrincipal principal = new UserPrincipal(user);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.delete("/recurring-rule/{id}", UUID.randomUUID())
                        .with(user(principal)))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(), StandardException.class);
        assertEquals(HttpStatus.NOT_FOUND.value(), exception.status());
    }

    @Test
    @DisplayName("Should return 404 when rule to delete belongs to another user")
    void delete_WhenRuleBelongsToAnotherUser_ShouldReturn404() throws Exception {
        User owner = createUser();
        Category category = createCategory(owner, "salary");
        RecurringRule rule = createRecurringRule(owner, category, TransactionPeriodType.MONTHLY, RecurringType.INCOME);

        User otherUser = userService.save(User.builder()
                .name("Other").email("other@gmail.com").password("Arthur@1406").role(UserRole.ROLE_USER).build());
        UserPrincipal otherPrincipal = new UserPrincipal(otherUser);

        mockMvc.perform(MockMvcRequestBuilders.delete("/recurring-rule/{id}", rule.getId())
                        .with(user(otherPrincipal)))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andDo(MockMvcResultHandlers.print());
    }

    // ── toggle ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should deactivate rule and return 200 when rule is currently active")
    void toggle_WhenRuleIsActive_ShouldDeactivateAndReturn200() throws Exception {
        User user = createUser();
        Category category = createCategory(user, "salary");
        UserPrincipal principal = new UserPrincipal(user);
        RecurringRule rule = createRecurringRule(user, category, TransactionPeriodType.MONTHLY, RecurringType.INCOME, true);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.patch("/recurring-rule/{id}/toggle", rule.getId())
                        .with(user(principal)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        RecurringRuleResponseDTO response = objectMapper.readValue(
                result.getResponse().getContentAsString(), RecurringRuleResponseDTO.class);
        assertFalse(response.isActive());
    }

    @Test
    @DisplayName("Should activate rule and return 200 when rule is currently inactive")
    void toggle_WhenRuleIsInactive_ShouldActivateAndReturn200() throws Exception {
        User user = createUser();
        Category category = createCategory(user, "salary");
        UserPrincipal principal = new UserPrincipal(user);
        RecurringRule rule = createRecurringRule(user, category, TransactionPeriodType.MONTHLY, RecurringType.INCOME, false);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.patch("/recurring-rule/{id}/toggle", rule.getId())
                        .with(user(principal)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        RecurringRuleResponseDTO response = objectMapper.readValue(
                result.getResponse().getContentAsString(), RecurringRuleResponseDTO.class);
        assertTrue(response.isActive());
    }

    @Test
    @DisplayName("Should return 404 when rule to toggle does not exist")
    void toggle_WhenRuleNotFound_ShouldReturn404() throws Exception {
        User user = createUser();
        UserPrincipal principal = new UserPrincipal(user);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.patch("/recurring-rule/{id}/toggle", UUID.randomUUID())
                        .with(user(principal)))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(), StandardException.class);
        assertEquals(HttpStatus.NOT_FOUND.value(), exception.status());
    }

    // ── update ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should update rule and return 200 with updated fields")
    void update_WhenValid_ShouldReturn200WithUpdatedFields() throws Exception {
        User user = createUser();
        Category category = createCategory(user, "salary");
        UserPrincipal principal = new UserPrincipal(user);
        RecurringRule rule = createRecurringRule(user, category, TransactionPeriodType.MONTHLY, RecurringType.INCOME);

        RecurringRuleUpdateDTO request = new RecurringRuleUpdateDTO(
                LocalDateTime.of(2026, 7, 1, 0, 0),
                new BigDecimal("6000.00"),
                "Updated salary",
                category.getId(),
                TransactionPeriodType.MONTHLY,
                RecurringType.INCOME
        );

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.put("/recurring-rule/{id}", rule.getId())
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        RecurringRuleResponseDTO response = objectMapper.readValue(
                result.getResponse().getContentAsString(), RecurringRuleResponseDTO.class);

        assertEquals("Updated salary", response.description());
        assertEquals(0, new BigDecimal("6000.00").compareTo(response.price()));
        assertEquals(rule.getId(), response.id());
        assertTrue(response.isActive());
    }

    @Test
    @DisplayName("Should return 409 when update creates a duplicate rule")
    void update_WhenDuplicateExists_ShouldReturn409() throws Exception {
        User user = createUser();
        Category category = createCategory(user, "salary");
        UserPrincipal principal = new UserPrincipal(user);

        createRecurringRule(user, category, TransactionPeriodType.MONTHLY, RecurringType.INCOME);
        RecurringRule rule2 = createRecurringRule(user, category, TransactionPeriodType.WEEKLY, RecurringType.EXPENSE);

        RecurringRuleUpdateDTO request = new RecurringRuleUpdateDTO(
                LocalDateTime.of(2026, 6, 5, 0, 0),
                new BigDecimal("5000.00"),
                "Test recurring rule",
                category.getId(),
                TransactionPeriodType.MONTHLY,
                RecurringType.INCOME
        );

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.put("/recurring-rule/{id}", rule2.getId())
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(), StandardException.class);
        assertEquals(HttpStatus.CONFLICT.value(), exception.status());
    }

    @Test
    @DisplayName("Should return 404 when rule to update does not exist")
    void update_WhenRuleNotFound_ShouldReturn404() throws Exception {
        User user = createUser();
        Category category = createCategory(user, "salary");
        UserPrincipal principal = new UserPrincipal(user);

        RecurringRuleUpdateDTO request = new RecurringRuleUpdateDTO(
                LocalDateTime.of(2026, 7, 1, 0, 0),
                new BigDecimal("6000.00"),
                "Updated salary",
                category.getId(),
                TransactionPeriodType.MONTHLY,
                RecurringType.INCOME
        );

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.put("/recurring-rule/{id}", UUID.randomUUID())
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(), StandardException.class);
        assertEquals(HttpStatus.NOT_FOUND.value(), exception.status());
    }

    @Test
    @DisplayName("Should return 404 when rule to update belongs to another user")
    void update_WhenRuleBelongsToAnotherUser_ShouldReturn404() throws Exception {
        User owner = createUser();
        Category category = createCategory(owner, "salary");
        RecurringRule rule = createRecurringRule(owner, category, TransactionPeriodType.MONTHLY, RecurringType.INCOME);

        User otherUser = userService.save(User.builder()
                .name("Other").email("other@gmail.com").password("Arthur@1406").role(UserRole.ROLE_USER).build());
        Category otherCategory = createCategory(otherUser, "other");
        UserPrincipal otherPrincipal = new UserPrincipal(otherUser);

        RecurringRuleUpdateDTO request = new RecurringRuleUpdateDTO(
                LocalDateTime.of(2026, 7, 1, 0, 0),
                new BigDecimal("6000.00"),
                "Updated salary",
                otherCategory.getId(),
                TransactionPeriodType.MONTHLY,
                RecurringType.INCOME
        );

        mockMvc.perform(MockMvcRequestBuilders.put("/recurring-rule/{id}", rule.getId())
                        .with(user(otherPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andDo(MockMvcResultHandlers.print());
    }

    @Test
    @DisplayName("Should return 400 when required field is missing on update")
    void update_WhenRequiredFieldMissing_ShouldReturn400() throws Exception {
        User user = createUser();
        Category category = createCategory(user, "salary");
        UserPrincipal principal = new UserPrincipal(user);
        RecurringRule rule = createRecurringRule(user, category, TransactionPeriodType.MONTHLY, RecurringType.INCOME);

        String bodyMissingPrice = """
                {
                    "transactionDate": "2026-07-01T00:00:00",
                    "description": "Updated salary",
                    "categoryId": "%s",
                    "transactionPeriodType": "MONTHLY",
                    "recurringType": "INCOME"
                }
                """.formatted(category.getId());

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.put("/recurring-rule/{id}", rule.getId())
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyMissingPrice))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(), StandardException.class);
        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.status());
    }

    // ── findById ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return recurring rule when it exists and belongs to the user")
    void findById_WhenRuleExistsAndBelongsToUser_ShouldReturn200() throws Exception {
        User user = createUser();
        Category category = createCategory(user, "salary");
        UserPrincipal principal = new UserPrincipal(user);
        RecurringRule rule = createRecurringRule(user, category, TransactionPeriodType.MONTHLY, RecurringType.INCOME);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/recurring-rule/{id}", rule.getId())
                        .with(user(principal)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        RecurringRuleResponseDTO response = objectMapper.readValue(
                result.getResponse().getContentAsString(), RecurringRuleResponseDTO.class);

        assertEquals(rule.getId(), response.id());
        assertEquals(rule.getDescription(), response.description());
        assertEquals(0, rule.getPrice().compareTo(response.price()));
        assertEquals(TransactionPeriodType.MONTHLY, response.transactionPeriodType());
        assertEquals(RecurringType.INCOME, response.recurringType());
        assertTrue(response.isActive());
    }

    @Test
    @DisplayName("Should return 404 when rule does not exist")
    void findById_WhenRuleNotFound_ShouldReturn404() throws Exception {
        User user = createUser();
        UserPrincipal principal = new UserPrincipal(user);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/recurring-rule/{id}", UUID.randomUUID())
                        .with(user(principal)))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(), StandardException.class);

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.status());
    }

    @Test
    @DisplayName("Should return 404 when rule belongs to another user")
    void findById_WhenRuleBelongsToAnotherUser_ShouldReturn404() throws Exception {
        User owner = createUser();
        Category category = createCategory(owner, "salary");
        RecurringRule rule = createRecurringRule(owner, category, TransactionPeriodType.MONTHLY, RecurringType.INCOME);

        User otherUser = userService.save(User.builder()
                .name("Other")
                .email("other@gmail.com")
                .password("Arthur@1406")
                .role(UserRole.ROLE_USER)
                .build());
        UserPrincipal otherPrincipal = new UserPrincipal(otherUser);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/recurring-rule/{id}", rule.getId())
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
    void findById_WhenUnauthenticated_ShouldReturn403() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/recurring-rule/{id}", UUID.randomUUID()))
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andDo(MockMvcResultHandlers.print());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private User createUser() {
        return userService.save(User.builder()
                .name("Anderson")
                .email("anderson@gmail.com")
                .password("Arthur@1406")
                .role(UserRole.ROLE_USER)
                .build());
    }

    private Category createCategory(User owner, String name) {
        return categoryRepository.save(Category.builder()
                .name(name)
                .description("Test category")
                .icon("icon")
                .owner(owner)
                .build());
    }

    private RecurringRule createRecurringRule(User owner, Category category,
                                              TransactionPeriodType period, RecurringType type) {
        return createRecurringRule(owner, category, period, type, true);
    }

    private RecurringRule createRecurringRule(User owner, Category category,
                                              TransactionPeriodType period, RecurringType type, boolean isActive) {
        return recurringRuleRepository.save(RecurringRule.builder()
                .owner(owner)
                .category(category)
                .transactionDate(LocalDateTime.of(2026, 6, 5, 0, 0))
                .price(new BigDecimal("5000.00"))
                .description("Test recurring rule")
                .transactionPeriodType(period)
                .recurringType(type)
                .isActive(isActive)
                .build());
    }
}
