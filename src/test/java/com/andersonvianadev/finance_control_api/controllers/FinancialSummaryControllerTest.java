package com.andersonvianadev.finance_control_api.controllers;

import com.andersonvianadev.finance_control_api.domain.models.Category;
import com.andersonvianadev.finance_control_api.domain.models.Expense;
import com.andersonvianadev.finance_control_api.domain.models.Income;
import com.andersonvianadev.finance_control_api.domain.models.RecurringRule;
import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.models.dtos.SummaryResponseDTO;
import com.andersonvianadev.finance_control_api.domain.models.enums.RecurringType;
import com.andersonvianadev.finance_control_api.domain.models.enums.TransactionPeriodType;
import com.andersonvianadev.finance_control_api.domain.models.enums.UserRole;
import com.andersonvianadev.finance_control_api.domain.services.IUserService;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@SpringBootTest
@AutoConfigureMockMvc
class FinancialSummaryControllerTest {

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
    private IncomeRepository incomeRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

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
        userRepository.deleteAll();
    }

    // ── summarize (auth) ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return 403 when request is unauthenticated")
    void summarize_WhenUnauthenticated_ShouldReturn403() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/financial-summary"))
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andDo(MockMvcResultHandlers.print());
    }

    // ── summarize (empty state) ───────────────────────────────────────────────

    @Test
    @DisplayName("Should return 200 with all zeros when user has no transactions")
    void summarize_WhenNoTransactionsExist_ShouldReturn200WithAllZeros() throws Exception {
        User user = createUser();
        UserPrincipal principal = new UserPrincipal(user);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/financial-summary")
                        .param("start", "2025-01-01")
                        .param("finish", "2025-12-31")
                        .with(user(principal)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        SummaryResponseDTO response = objectMapper.readValue(
                result.getResponse().getContentAsString(), SummaryResponseDTO.class);

        assertEquals(0, BigDecimal.ZERO.compareTo(response.realizedIncome()));
        assertEquals(0, BigDecimal.ZERO.compareTo(response.realizedExpense()));
        assertEquals(0, BigDecimal.ZERO.compareTo(response.scheduledExpense()));
        assertEquals(0, BigDecimal.ZERO.compareTo(response.projectedExpense()));
        assertEquals(0, BigDecimal.ZERO.compareTo(response.totalBalance()));
        assertEquals(0L, response.totalCount());
    }

    @Test
    @DisplayName("Should return 200 when start and finish are omitted and default to current month")
    void summarize_WhenDefaultDates_ShouldReturn200() throws Exception {
        User user = createUser();
        UserPrincipal principal = new UserPrincipal(user);

        mockMvc.perform(MockMvcRequestBuilders.get("/financial-summary")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print());
    }

    // ── summarize (realized bucket) ───────────────────────────────────────────

    @Test
    @DisplayName("Should populate realized bucket when past income and expense exist within the range")
    void summarize_WhenPastTransactionsExist_ShouldPopulateRealizedBucket() throws Exception {
        User user = createUser();
        Category category = createCategory(user, "salary");
        UserPrincipal principal = new UserPrincipal(user);

        createIncome(user, category, LocalDateTime.of(2025, 6, 5, 0, 0), new BigDecimal("5000.00"), "June salary");
        createExpense(user, category, LocalDateTime.of(2025, 6, 10, 0, 0), new BigDecimal("1500.00"), "June rent");

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/financial-summary")
                        .param("start", "2025-06-01")
                        .param("finish", "2025-06-30")
                        .with(user(principal)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        SummaryResponseDTO response = objectMapper.readValue(
                result.getResponse().getContentAsString(), SummaryResponseDTO.class);

        assertEquals(0, new BigDecimal("5000.00").compareTo(response.realizedIncome()));
        assertEquals(0, new BigDecimal("1500.00").compareTo(response.realizedExpense()));
        assertEquals(0, new BigDecimal("3500.00").compareTo(response.realizedBalance()));
        assertEquals(2L, response.realizedCount());
        assertEquals(0, BigDecimal.ZERO.compareTo(response.scheduledIncome()));
        assertEquals(0, BigDecimal.ZERO.compareTo(response.scheduledExpense()));
        assertEquals(0, BigDecimal.ZERO.compareTo(response.projectedIncome()));
        assertEquals(0, BigDecimal.ZERO.compareTo(response.projectedExpense()));
    }

    // ── summarize (scheduled bucket) ─────────────────────────────────────────

    @Test
    @DisplayName("Should show manually registered future expense in the scheduled bucket")
    void summarize_WhenManualFutureExpenseRegistered_ShouldAppearInScheduledBucket() throws Exception {
        User user = createUser();
        Category category = createCategory(user, "bills");
        UserPrincipal principal = new UserPrincipal(user);

        LocalDateTime futureDate = LocalDate.now().plusDays(10).atStartOfDay();
        createExpense(user, category, futureDate, new BigDecimal("800.00"), "Future rent");

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/financial-summary")
                        .param("start", "2020-01-01")
                        .param("finish", "2030-12-31")
                        .with(user(principal)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        SummaryResponseDTO response = objectMapper.readValue(
                result.getResponse().getContentAsString(), SummaryResponseDTO.class);

        assertEquals(0, new BigDecimal("800.00").compareTo(response.scheduledExpense()));
        assertEquals(1L, response.scheduledCount());
        assertEquals(0, BigDecimal.ZERO.compareTo(response.realizedExpense()));
    }

    @Test
    @DisplayName("Should show manually registered future income in the scheduled bucket")
    void summarize_WhenManualFutureIncomeRegistered_ShouldAppearInScheduledBucket() throws Exception {
        User user = createUser();
        Category category = createCategory(user, "salary");
        UserPrincipal principal = new UserPrincipal(user);

        LocalDateTime futureDate = LocalDate.now().plusDays(5).atStartOfDay();
        createIncome(user, category, futureDate, new BigDecimal("3000.00"), "Future bonus");

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/financial-summary")
                        .param("start", "2020-01-01")
                        .param("finish", "2030-12-31")
                        .with(user(principal)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        SummaryResponseDTO response = objectMapper.readValue(
                result.getResponse().getContentAsString(), SummaryResponseDTO.class);

        assertEquals(0, new BigDecimal("3000.00").compareTo(response.scheduledIncome()));
        assertEquals(1L, response.scheduledCount());
        assertEquals(0, BigDecimal.ZERO.compareTo(response.realizedIncome()));
    }

    // ── summarize (projected bucket) ─────────────────────────────────────────

    @Test
    @DisplayName("Should show recurring rule as projected expense when no matching expense has been posted")
    void summarize_WhenRecurringExpenseRuleExistsAndNotPosted_ShouldAppearInProjectedBucket() throws Exception {
        User user = createUser();
        Category category = createCategory(user, "bills");
        UserPrincipal principal = new UserPrincipal(user);

        LocalDateTime ruleDate = LocalDate.now().plusDays(15).atStartOfDay();
        createRecurringRule(user, category, ruleDate, new BigDecimal("1200.00"),
                "Monthly internet bill", RecurringType.EXPENSE, TransactionPeriodType.MONTHLY);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/financial-summary")
                        .param("start", "2020-01-01")
                        .param("finish", "2030-12-31")
                        .with(user(principal)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        SummaryResponseDTO response = objectMapper.readValue(
                result.getResponse().getContentAsString(), SummaryResponseDTO.class);

        assertEquals(0, new BigDecimal("1200.00").compareTo(response.projectedExpense()));
        assertEquals(1L, response.projectedCount());
        assertEquals(0, BigDecimal.ZERO.compareTo(response.scheduledExpense()));
    }

    @Test
    @DisplayName("Should show recurring rule as projected income when no matching income has been posted")
    void summarize_WhenRecurringIncomeRuleExistsAndNotPosted_ShouldAppearInProjectedBucket() throws Exception {
        User user = createUser();
        Category category = createCategory(user, "salary");
        UserPrincipal principal = new UserPrincipal(user);

        LocalDateTime ruleDate = LocalDate.now().plusDays(20).atStartOfDay();
        createRecurringRule(user, category, ruleDate, new BigDecimal("5000.00"),
                "Monthly salary", RecurringType.INCOME, TransactionPeriodType.MONTHLY);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/financial-summary")
                        .param("start", "2020-01-01")
                        .param("finish", "2030-12-31")
                        .with(user(principal)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        SummaryResponseDTO response = objectMapper.readValue(
                result.getResponse().getContentAsString(), SummaryResponseDTO.class);

        assertEquals(0, new BigDecimal("5000.00").compareTo(response.projectedIncome()));
        assertEquals(1L, response.projectedCount());
        assertEquals(0, BigDecimal.ZERO.compareTo(response.scheduledIncome()));
    }

    // ── summarize (isolation) ─────────────────────────────────────────────────

    @Test
    @DisplayName("Should not include transactions belonging to another user")
    void summarize_WhenTransactionsBelongToAnotherUser_ShouldNotIncludeTheirData() throws Exception {
        User owner = createUser("anderson@gmail.com");
        User other = createUser("other@gmail.com");
        Category ownerCategory = createCategory(owner, "salary");
        Category otherCategory = createCategory(other, "salary");
        UserPrincipal ownerPrincipal = new UserPrincipal(owner);

        createIncome(other, otherCategory, LocalDateTime.of(2025, 6, 5, 0, 0),
                new BigDecimal("9999.00"), "Other user salary");
        createExpense(other, otherCategory, LocalDateTime.of(2025, 6, 10, 0, 0),
                new BigDecimal("5000.00"), "Other user rent");

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/financial-summary")
                        .param("start", "2025-01-01")
                        .param("finish", "2025-12-31")
                        .with(user(ownerPrincipal)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        SummaryResponseDTO response = objectMapper.readValue(
                result.getResponse().getContentAsString(), SummaryResponseDTO.class);

        assertEquals(0, BigDecimal.ZERO.compareTo(response.realizedIncome()));
        assertEquals(0, BigDecimal.ZERO.compareTo(response.realizedExpense()));
        assertEquals(0L, response.totalCount());
    }

    // ── summarize (computed fields) ───────────────────────────────────────────

    @Test
    @DisplayName("Should calculate commitment rate correctly from realized income and expense")
    void summarize_WhenRealizedIncomeAndExpenseExist_ShouldCalculateCorrectCommitmentRate() throws Exception {
        User user = createUser();
        Category category = createCategory(user, "salary");
        UserPrincipal principal = new UserPrincipal(user);

        createIncome(user, category, LocalDateTime.of(2025, 6, 5, 0, 0),
                new BigDecimal("5000.00"), "June salary");
        createExpense(user, category, LocalDateTime.of(2025, 6, 10, 0, 0),
                new BigDecimal("2000.00"), "June rent");

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/financial-summary")
                        .param("start", "2025-06-01")
                        .param("finish", "2025-06-30")
                        .with(user(principal)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        SummaryResponseDTO response = objectMapper.readValue(
                result.getResponse().getContentAsString(), SummaryResponseDTO.class);

        // commitmentRate = (2000 / 5000) * 100 = 40.00
        assertEquals(0, new BigDecimal("40.00").compareTo(response.commitmentRate()));
    }

    @Test
    @DisplayName("Should compute correct totalBalance and totalCount across all populated buckets")
    void summarize_WhenAllBucketsHaveData_ShouldComputeCorrectTotalBalanceAndCount() throws Exception {
        User user = createUser();
        Category category = createCategory(user, "general");
        UserPrincipal principal = new UserPrincipal(user);

        // realized: income 5000, expense 2000
        createIncome(user, category, LocalDateTime.of(2025, 6, 5, 0, 0),
                new BigDecimal("5000.00"), "Past income");
        createExpense(user, category, LocalDateTime.of(2025, 6, 10, 0, 0),
                new BigDecimal("2000.00"), "Past expense");

        // scheduled: future expense 800 and future income 300
        LocalDateTime futureExpDate = LocalDate.now().plusDays(10).atStartOfDay();
        LocalDateTime futureIncDate = LocalDate.now().plusDays(12).atStartOfDay();
        createExpense(user, category, futureExpDate, new BigDecimal("800.00"), "Scheduled expense");
        createIncome(user, category, futureIncDate, new BigDecimal("300.00"), "Scheduled income");

        // projected: recurring rule expense 1000
        LocalDateTime ruleDate = LocalDate.now().plusDays(20).atStartOfDay();
        createRecurringRule(user, category, ruleDate, new BigDecimal("1000.00"),
                "Recurring expense", RecurringType.EXPENSE, TransactionPeriodType.MONTHLY);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/financial-summary")
                        .param("start", "2020-01-01")
                        .param("finish", "2030-12-31")
                        .with(user(principal)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        SummaryResponseDTO response = objectMapper.readValue(
                result.getResponse().getContentAsString(), SummaryResponseDTO.class);

        // realizedBalance = 5000 - 2000 = 3000
        // scheduledBalance = 300 - 800 = -500
        // projectedBalance = 0 - 1000 = -1000
        // totalBalance = 3000 - 500 - 1000 = 1500
        assertEquals(0, new BigDecimal("3000.00").compareTo(response.realizedBalance()));
        assertEquals(0, new BigDecimal("-500.00").compareTo(response.scheduledBalance()));
        assertEquals(0, new BigDecimal("-1000.00").compareTo(response.projectedBalance()));
        assertEquals(0, new BigDecimal("1500.00").compareTo(response.totalBalance()));

        // realizedCount = 2, scheduledCount = 2, projectedCount = 1 → totalCount = 5
        assertEquals(2L, response.realizedCount());
        assertEquals(2L, response.scheduledCount());
        assertEquals(1L, response.projectedCount());
        assertEquals(5L, response.totalCount());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private User createUser() {
        return createUser("anderson@gmail.com");
    }

    private User createUser(String email) {
        return userService.save(User.builder()
                .name("Anderson")
                .email(email)
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

    private Income createIncome(User owner, Category category, LocalDateTime date,
                                BigDecimal price, String description) {
        return incomeRepository.save(Income.builder()
                .owner(owner)
                .category(category)
                .transactionDate(date)
                .price(price)
                .description(description)
                .build());
    }

    private Expense createExpense(User owner, Category category, LocalDateTime date,
                                  BigDecimal price, String description) {
        return expenseRepository.save(Expense.builder()
                .owner(owner)
                .category(category)
                .transactionDate(date)
                .price(price)
                .description(description)
                .build());
    }

    private RecurringRule createRecurringRule(User owner, Category category, LocalDateTime date,
                                              BigDecimal price, String description,
                                              RecurringType recurringType,
                                              TransactionPeriodType periodType) {
        return recurringRuleRepository.save(RecurringRule.builder()
                .owner(owner)
                .category(category)
                .transactionDate(date)
                .price(price)
                .description(description)
                .recurringType(recurringType)
                .transactionPeriodType(periodType)
                .build());
    }
}
