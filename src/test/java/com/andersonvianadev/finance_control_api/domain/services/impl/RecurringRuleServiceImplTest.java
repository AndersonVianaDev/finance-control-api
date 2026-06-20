package com.andersonvianadev.finance_control_api.domain.services.impl;

import com.andersonvianadev.finance_control_api.domain.models.Category;
import com.andersonvianadev.finance_control_api.domain.models.Expense;
import com.andersonvianadev.finance_control_api.domain.models.Income;
import com.andersonvianadev.finance_control_api.domain.models.RecurringRule;
import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.models.enums.RecurringType;
import com.andersonvianadev.finance_control_api.domain.models.enums.TransactionPeriodType;
import com.andersonvianadev.finance_control_api.domain.models.enums.UserRole;
import com.andersonvianadev.finance_control_api.domain.services.ICategoryService;
import com.andersonvianadev.finance_control_api.domain.services.IExpenseService;
import com.andersonvianadev.finance_control_api.infra.exceptions.NotFoundException;
import com.andersonvianadev.finance_control_api.infra.exceptions.ResourceAlreadyExistsException;
import com.andersonvianadev.finance_control_api.infra.repositories.IncomeRepository;
import com.andersonvianadev.finance_control_api.infra.repositories.RecurringRuleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RecurringRuleServiceImplTest {

    @Mock
    private RecurringRuleRepository repository;

    @Mock
    private ICategoryService categoryService;

    @Mock
    private IExpenseService expenseService;

    @Mock
    private IncomeRepository incomeRepository;

    @InjectMocks
    private RecurringRuleServiceImpl service;

    // ── save ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should save recurring rule successfully when data is valid")
    void save_WhenValid_ShouldSaveAndReturnRule() {
        User owner = buildUser();
        Category category = buildCategory(owner);
        RecurringRule rule = buildRule(owner, category.getId(), TransactionPeriodType.MONTHLY, RecurringType.INCOME);
        RecurringRule saved = RecurringRule.builder().id(UUID.randomUUID()).build();

        doReturn(category).when(categoryService).findByIdAndOwnerOrOwnerIsNull(category.getId(), owner);
        doReturn(false).when(repository)
                .existsRecurringRuleByOwnerIdAndCategoryIdAndPriceAndTransactionDateAndDescription(
                        owner.getId(), category.getId(), rule.getPrice(), rule.getTransactionDate(), rule.getDescription());
        doReturn(saved).when(repository).save(rule);

        RecurringRule result = service.save(rule);

        verify(repository, times(1)).save(rule);
        assertEquals(saved, result);
    }

    @Test
    @DisplayName("Should throw NotFoundException when category does not exist")
    void save_WhenCategoryNotFound_ShouldThrowNotFoundException() {
        User owner = buildUser();
        UUID categoryId = UUID.randomUUID();
        RecurringRule rule = buildRule(owner, categoryId, TransactionPeriodType.WEEKLY, RecurringType.EXPENSE);

        doThrow(new NotFoundException("Category not found."))
                .when(categoryService).findByIdAndOwnerOrOwnerIsNull(categoryId, owner);

        assertThrows(NotFoundException.class, () -> service.save(rule));

        verify(repository, never()).existsRecurringRuleByOwnerIdAndCategoryIdAndPriceAndTransactionDateAndDescription(
                any(), any(), any(), any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceAlreadyExistsException when rule already exists")
    void save_WhenDuplicateRule_ShouldThrowResourceAlreadyExistsException() {
        User owner = buildUser();
        Category category = buildCategory(owner);
        RecurringRule rule = buildRule(owner, category.getId(), TransactionPeriodType.MONTHLY, RecurringType.EXPENSE);

        doReturn(category).when(categoryService).findByIdAndOwnerOrOwnerIsNull(category.getId(), owner);
        doReturn(true).when(repository)
                .existsRecurringRuleByOwnerIdAndCategoryIdAndPriceAndTransactionDateAndDescription(
                        owner.getId(), category.getId(), rule.getPrice(), rule.getTransactionDate(), rule.getDescription());

        assertThrows(ResourceAlreadyExistsException.class, () -> service.save(rule));

        verify(repository, never()).save(any());
    }

    // ── processRecurringOccurrences ───────────────────────────────────────────

    @Test
    @DisplayName("Should generate expense when monthly expense rule is due today")
    void processRecurringOccurrences_WhenMonthlyExpenseRuleIsDueToday_ShouldGenerateExpense() {
        LocalDate today = LocalDate.of(2026, 6, 15);
        RecurringRule rule = buildRuleWithDate(TransactionPeriodType.MONTHLY, RecurringType.EXPENSE,
                LocalDateTime.of(2026, 1, 15, 0, 0));

        doReturn(List.of(rule)).when(repository).findAllByIsActiveTrue();
        doReturn(Expense.builder().id(UUID.randomUUID()).build()).when(expenseService).save(any(), eq(true));

        try (MockedStatic<LocalDate> mock = Mockito.mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS)) {
            mock.when(LocalDate::now).thenReturn(today);

            service.processRecurringOccurrences();
        }

        verify(expenseService, times(1)).save(any(), eq(true));
        verify(incomeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should skip monthly rule when today is not the rule day")
    void processRecurringOccurrences_WhenMonthlyRuleNotDueToday_ShouldSkip() {
        LocalDate today = LocalDate.of(2026, 6, 10);
        RecurringRule rule = buildRuleWithDate(TransactionPeriodType.MONTHLY, RecurringType.EXPENSE,
                LocalDateTime.of(2026, 1, 15, 0, 0)); // day 15, today is day 10

        doReturn(List.of(rule)).when(repository).findAllByIsActiveTrue();

        try (MockedStatic<LocalDate> mock = Mockito.mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS)) {
            mock.when(LocalDate::now).thenReturn(today);

            service.processRecurringOccurrences();
        }

        verify(expenseService, never()).save(any(), anyBoolean());
        verify(incomeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should generate income when weekly income rule is due today")
    void processRecurringOccurrences_WhenWeeklyIncomeRuleIsDueToday_ShouldGenerateIncome() {
        LocalDate today = LocalDate.of(2026, 6, 15); // Monday
        assertEquals(DayOfWeek.MONDAY, today.getDayOfWeek());

        RecurringRule rule = buildRuleWithDate(TransactionPeriodType.WEEKLY, RecurringType.INCOME,
                LocalDateTime.of(2026, 6, 8, 0, 0)); // also a Monday

        doReturn(List.of(rule)).when(repository).findAllByIsActiveTrue();
        doReturn(false).when(incomeRepository).existsDuplicate(any(), any(), any(), any(), any());
        doReturn(Income.builder().id(UUID.randomUUID()).build()).when(incomeRepository).save(any());

        try (MockedStatic<LocalDate> mock = Mockito.mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS)) {
            mock.when(LocalDate::now).thenReturn(today);

            service.processRecurringOccurrences();
        }

        verify(incomeRepository, times(1)).save(any());
        verify(expenseService, never()).save(any(), anyBoolean());
    }

    @Test
    @DisplayName("Should skip income generation when income already exists for today")
    void processRecurringOccurrences_WhenIncomeAlreadyExists_ShouldSkipGeneration() {
        LocalDate today = LocalDate.of(2026, 6, 15);
        RecurringRule rule = buildRuleWithDate(TransactionPeriodType.MONTHLY, RecurringType.INCOME,
                LocalDateTime.of(2026, 1, 15, 0, 0));

        doReturn(List.of(rule)).when(repository).findAllByIsActiveTrue();
        doReturn(true).when(incomeRepository).existsDuplicate(any(), any(), any(), any(), any());

        try (MockedStatic<LocalDate> mock = Mockito.mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS)) {
            mock.when(LocalDate::now).thenReturn(today);

            service.processRecurringOccurrences();
        }

        verify(incomeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should continue processing remaining rules when one expense generation fails")
    void processRecurringOccurrences_WhenExpenseServiceThrows_ShouldContinueProcessing() {
        LocalDate today = LocalDate.of(2026, 6, 15);
        RecurringRule failingRule = buildRuleWithDate(TransactionPeriodType.MONTHLY, RecurringType.EXPENSE,
                LocalDateTime.of(2026, 1, 15, 0, 0));
        RecurringRule incomeRule = buildRuleWithDate(TransactionPeriodType.MONTHLY, RecurringType.INCOME,
                LocalDateTime.of(2026, 1, 15, 0, 0));

        doReturn(List.of(failingRule, incomeRule)).when(repository).findAllByIsActiveTrue();
        doThrow(new RuntimeException("unexpected error")).when(expenseService).save(any(), eq(true));
        doReturn(false).when(incomeRepository).existsDuplicate(any(), any(), any(), any(), any());
        doReturn(Income.builder().id(UUID.randomUUID()).build()).when(incomeRepository).save(any());

        try (MockedStatic<LocalDate> mock = Mockito.mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS)) {
            mock.when(LocalDate::now).thenReturn(today);

            service.processRecurringOccurrences();
        }

        verify(expenseService, times(1)).save(any(), eq(true));
        verify(incomeRepository, times(1)).save(any());
    }

    // ── findById ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return recurring rule when found")
    void findById_WhenRuleExists_ShouldReturnRule() {
        User user = buildUser();
        UUID id = UUID.randomUUID();
        RecurringRule rule = RecurringRule.builder()
                .id(id)
                .owner(user)
                .category(buildCategory(user))
                .transactionDate(LocalDateTime.of(2026, 6, 5, 0, 0))
                .price(new BigDecimal("5000.00"))
                .description("Monthly salary")
                .transactionPeriodType(TransactionPeriodType.MONTHLY)
                .recurringType(RecurringType.INCOME)
                .build();

        doReturn(Optional.of(rule)).when(repository).findByOwnerIdAndId(user.getId(), id);

        RecurringRule result = service.findById(user, id);

        assertEquals(rule, result);
        verify(repository, times(1)).findByOwnerIdAndId(user.getId(), id);
    }

    @Test
    @DisplayName("Should throw NotFoundException when rule does not exist or belongs to another user")
    void findById_WhenRuleNotFound_ShouldThrowNotFoundException() {
        User user = buildUser();
        UUID id = UUID.randomUUID();

        doReturn(Optional.empty()).when(repository).findByOwnerIdAndId(user.getId(), id);

        assertThrows(NotFoundException.class, () -> service.findById(user, id));
        verify(repository, times(1)).findByOwnerIdAndId(user.getId(), id);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private User buildUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .name("Anderson")
                .email("anderson@gmail.com")
                .password("Arthur@1406")
                .role(UserRole.ROLE_USER)
                .build();
    }

    private Category buildCategory(User owner) {
        return Category.builder()
                .id(UUID.randomUUID())
                .name("salary")
                .description("Employment income")
                .icon("wallet")
                .owner(owner)
                .build();
    }

    private RecurringRule buildRule(User owner, UUID categoryId, TransactionPeriodType period, RecurringType type) {
        return RecurringRule.builder()
                .id(UUID.randomUUID())
                .owner(owner)
                .category(Category.builder().id(categoryId).build())
                .transactionDate(LocalDateTime.of(2026, 6, 5, 0, 0))
                .price(new BigDecimal("5000.00"))
                .description("Monthly salary")
                .transactionPeriodType(period)
                .recurringType(type)
                .build();
    }

    private RecurringRule buildRuleWithDate(TransactionPeriodType period, RecurringType type, LocalDateTime date) {
        User owner = buildUser();
        return RecurringRule.builder()
                .id(UUID.randomUUID())
                .owner(owner)
                .category(buildCategory(owner))
                .transactionDate(date)
                .price(new BigDecimal("5000.00"))
                .description("Recurring transaction")
                .transactionPeriodType(period)
                .recurringType(type)
                .build();
    }
}
