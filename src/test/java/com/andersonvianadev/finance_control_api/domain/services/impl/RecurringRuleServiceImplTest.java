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
import com.andersonvianadev.finance_control_api.domain.services.IIncomeService;
import com.andersonvianadev.finance_control_api.infra.exceptions.NotFoundException;
import com.andersonvianadev.finance_control_api.infra.exceptions.ResourceAlreadyExistsException;
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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    private IIncomeService incomeService;

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
        doReturn(Expense.builder().id(UUID.randomUUID()).build()).when(expenseService).save(any(), eq(true), eq(true));

        try (MockedStatic<LocalDate> mock = Mockito.mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS)) {
            mock.when(LocalDate::now).thenReturn(today);

            service.processRecurringOccurrences();
        }

        verify(expenseService, times(1)).save(any(), eq(true), eq(true));
        verify(incomeService, never()).save(any(), anyBoolean());
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

        verify(expenseService, never()).save(any(), anyBoolean(), anyBoolean());
        verify(incomeService, never()).save(any(), anyBoolean());
    }

    @Test
    @DisplayName("Should generate income when weekly income rule is due today")
    void processRecurringOccurrences_WhenWeeklyIncomeRuleIsDueToday_ShouldGenerateIncome() {
        LocalDate today = LocalDate.of(2026, 6, 15); // Monday
        assertEquals(DayOfWeek.MONDAY, today.getDayOfWeek());

        RecurringRule rule = buildRuleWithDate(TransactionPeriodType.WEEKLY, RecurringType.INCOME,
                LocalDateTime.of(2026, 6, 8, 0, 0)); // also a Monday

        doReturn(List.of(rule)).when(repository).findAllByIsActiveTrue();
        doReturn(Income.builder().id(UUID.randomUUID()).build()).when(incomeService).save(any(), eq(true));

        try (MockedStatic<LocalDate> mock = Mockito.mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS)) {
            mock.when(LocalDate::now).thenReturn(today);

            service.processRecurringOccurrences();
        }

        verify(incomeService, times(1)).save(any(), eq(true));
        verify(expenseService, never()).save(any(), anyBoolean(), anyBoolean());
    }

    @Test
    @DisplayName("Should skip income generation when income already exists for today")
    void processRecurringOccurrences_WhenIncomeAlreadyExists_ShouldSkipGeneration() {
        LocalDate today = LocalDate.of(2026, 6, 15);
        RecurringRule rule = buildRuleWithDate(TransactionPeriodType.MONTHLY, RecurringType.INCOME,
                LocalDateTime.of(2026, 1, 15, 0, 0));

        doReturn(List.of(rule)).when(repository).findAllByIsActiveTrue();
        doThrow(new ResourceAlreadyExistsException("Income already registered"))
                .when(incomeService).save(any(), eq(true));

        try (MockedStatic<LocalDate> mock = Mockito.mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS)) {
            mock.when(LocalDate::now).thenReturn(today);

            service.processRecurringOccurrences();
        }

        verify(incomeService, times(1)).save(any(), eq(true));
    }

    @Test
    @DisplayName("Should skip expense generation when expense already exists for today")
    void processRecurringOccurrences_WhenExpenseAlreadyExists_ShouldSkipGeneration() {
        LocalDate today = LocalDate.of(2026, 6, 15);
        RecurringRule rule = buildRuleWithDate(TransactionPeriodType.MONTHLY, RecurringType.EXPENSE,
                LocalDateTime.of(2026, 1, 15, 0, 0));

        doReturn(List.of(rule)).when(repository).findAllByIsActiveTrue();
        doThrow(new ResourceAlreadyExistsException("Expense already registered"))
                .when(expenseService).save(any(), eq(true), eq(true));

        try (MockedStatic<LocalDate> mock = Mockito.mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS)) {
            mock.when(LocalDate::now).thenReturn(today);

            service.processRecurringOccurrences();
        }

        verify(expenseService, times(1)).save(any(), eq(true), eq(true));
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
        doThrow(new RuntimeException("unexpected error")).when(expenseService).save(any(), eq(true), eq(true));
        doReturn(Income.builder().id(UUID.randomUUID()).build()).when(incomeService).save(any(), eq(true));

        try (MockedStatic<LocalDate> mock = Mockito.mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS)) {
            mock.when(LocalDate::now).thenReturn(today);

            service.processRecurringOccurrences();
        }

        verify(expenseService, times(1)).save(any(), eq(true), eq(true));
        verify(incomeService, times(1)).save(any(), eq(true));
    }

    // ── findAll ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return page of rules when owner has rules")
    void findAll_WhenRulesExist_ShouldReturnPage() {
        User user = buildUser();
        RecurringRule rule = buildRule(user, UUID.randomUUID(), TransactionPeriodType.MONTHLY, RecurringType.INCOME);
        Page<RecurringRule> page = new PageImpl<>(List.of(rule));
        PageRequest pageable = PageRequest.of(0, 10);

        doReturn(page).when(repository).findAllByOwnerId(user.getId(), pageable);

        Page<RecurringRule> result = service.findAll(user, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(repository, times(1)).findAllByOwnerId(user.getId(), pageable);
    }

    @Test
    @DisplayName("Should return empty page when owner has no rules")
    void findAll_WhenNoRules_ShouldReturnEmptyPage() {
        User user = buildUser();
        Page<RecurringRule> empty = new PageImpl<>(List.of());
        PageRequest pageable = PageRequest.of(0, 10);

        doReturn(empty).when(repository).findAllByOwnerId(user.getId(), pageable);

        Page<RecurringRule> result = service.findAll(user, pageable);

        assertEquals(0, result.getTotalElements());
    }

    // ── delete ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should delete rule when it exists and belongs to the user")
    void delete_WhenRuleExists_ShouldDelete() {
        User user = buildUser();
        UUID id = UUID.randomUUID();
        RecurringRule rule = buildRule(user, UUID.randomUUID(), TransactionPeriodType.MONTHLY, RecurringType.INCOME);

        doReturn(Optional.of(rule)).when(repository).findByOwnerIdAndId(user.getId(), id);

        service.delete(user, id);

        verify(repository, times(1)).delete(rule);
    }

    @Test
    @DisplayName("Should throw NotFoundException when rule to delete does not exist")
    void delete_WhenRuleNotFound_ShouldThrowNotFoundException() {
        User user = buildUser();
        UUID id = UUID.randomUUID();

        doReturn(Optional.empty()).when(repository).findByOwnerIdAndId(user.getId(), id);

        assertThrows(NotFoundException.class, () -> service.delete(user, id));
        verify(repository, never()).delete(any(RecurringRule.class));
    }

    // ── toggle ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should deactivate rule when it is currently active")
    void toggle_WhenRuleIsActive_ShouldDeactivate() {
        User user = buildUser();
        UUID id = UUID.randomUUID();
        RecurringRule rule = RecurringRule.builder()
                .id(id).owner(user).category(buildCategory(user))
                .transactionDate(LocalDateTime.of(2026, 6, 5, 0, 0))
                .price(new BigDecimal("5000.00")).description("Salary")
                .transactionPeriodType(TransactionPeriodType.MONTHLY)
                .recurringType(RecurringType.INCOME)
                .isActive(true)
                .build();

        doReturn(Optional.of(rule)).when(repository).findByOwnerIdAndId(user.getId(), id);
        doReturn(rule).when(repository).save(rule);

        RecurringRule result = service.toggle(user, id);

        assertFalse(result.getIsActive());
        verify(repository, times(1)).save(rule);
    }

    @Test
    @DisplayName("Should activate rule when it is currently inactive")
    void toggle_WhenRuleIsInactive_ShouldActivate() {
        User user = buildUser();
        UUID id = UUID.randomUUID();
        RecurringRule rule = RecurringRule.builder()
                .id(id).owner(user).category(buildCategory(user))
                .transactionDate(LocalDateTime.of(2026, 6, 5, 0, 0))
                .price(new BigDecimal("5000.00")).description("Salary")
                .transactionPeriodType(TransactionPeriodType.MONTHLY)
                .recurringType(RecurringType.INCOME)
                .isActive(false)
                .build();

        doReturn(Optional.of(rule)).when(repository).findByOwnerIdAndId(user.getId(), id);
        doReturn(rule).when(repository).save(rule);

        RecurringRule result = service.toggle(user, id);

        assertTrue(result.getIsActive());
        verify(repository, times(1)).save(rule);
    }

    @Test
    @DisplayName("Should throw NotFoundException when rule to toggle does not exist")
    void toggle_WhenRuleNotFound_ShouldThrowNotFoundException() {
        User user = buildUser();
        UUID id = UUID.randomUUID();

        doReturn(Optional.empty()).when(repository).findByOwnerIdAndId(user.getId(), id);

        assertThrows(NotFoundException.class, () -> service.toggle(user, id));
        verify(repository, never()).save(any());
    }

    // ── update ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should update and return rule when data is valid")
    void update_WhenValid_ShouldUpdateAndReturn() {
        User user = buildUser();
        Category category = buildCategory(user);
        UUID id = UUID.randomUUID();

        RecurringRule existing = RecurringRule.builder()
                .id(id).owner(user).category(category)
                .transactionDate(LocalDateTime.of(2026, 6, 5, 0, 0))
                .price(new BigDecimal("5000.00")).description("Salary")
                .transactionPeriodType(TransactionPeriodType.MONTHLY)
                .recurringType(RecurringType.INCOME)
                .isActive(true)
                .build();

        RecurringRule incoming = RecurringRule.builder()
                .id(id).owner(user)
                .category(Category.builder().id(category.getId()).build())
                .transactionDate(LocalDateTime.of(2026, 7, 1, 0, 0))
                .price(new BigDecimal("6000.00")).description("Updated salary")
                .transactionPeriodType(TransactionPeriodType.MONTHLY)
                .recurringType(RecurringType.INCOME)
                .build();

        doReturn(Optional.of(existing)).when(repository).findByOwnerIdAndId(user.getId(), id);
        doReturn(category).when(categoryService).findByIdAndOwnerOrOwnerIsNull(category.getId(), user);
        doReturn(false).when(repository)
                .existsRecurringRuleByOwnerIdAndCategoryIdAndPriceAndTransactionDateAndDescriptionAndIdNot(
                        user.getId(), category.getId(),
                        incoming.getPrice(), incoming.getTransactionDate(),
                        incoming.getDescription(), id);
        doReturn(existing).when(repository).save(existing);

        RecurringRule result = service.update(incoming);

        assertNotNull(result);
        verify(repository, times(1)).save(existing);
    }

    @Test
    @DisplayName("Should throw NotFoundException when rule to update does not exist")
    void update_WhenRuleNotFound_ShouldThrowNotFoundException() {
        User user = buildUser();
        UUID id = UUID.randomUUID();
        RecurringRule incoming = buildRule(user, UUID.randomUUID(), TransactionPeriodType.MONTHLY, RecurringType.INCOME);
        incoming.setId(id);

        doReturn(Optional.empty()).when(repository).findByOwnerIdAndId(user.getId(), id);

        assertThrows(NotFoundException.class, () -> service.update(incoming));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw NotFoundException when category does not exist on update")
    void update_WhenCategoryNotFound_ShouldThrowNotFoundException() {
        User user = buildUser();
        UUID id = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        RecurringRule existing = buildRule(user, categoryId, TransactionPeriodType.MONTHLY, RecurringType.INCOME);
        existing.setId(id);
        RecurringRule incoming = buildRule(user, categoryId, TransactionPeriodType.MONTHLY, RecurringType.INCOME);
        incoming.setId(id);

        doReturn(Optional.of(existing)).when(repository).findByOwnerIdAndId(user.getId(), id);
        doThrow(new NotFoundException("Category not found."))
                .when(categoryService).findByIdAndOwnerOrOwnerIsNull(categoryId, user);

        assertThrows(NotFoundException.class, () -> service.update(incoming));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceAlreadyExistsException when update creates a duplicate")
    void update_WhenDuplicateExists_ShouldThrowResourceAlreadyExistsException() {
        User user = buildUser();
        Category category = buildCategory(user);
        UUID id = UUID.randomUUID();
        RecurringRule existing = buildRule(user, category.getId(), TransactionPeriodType.MONTHLY, RecurringType.INCOME);
        existing.setId(id);
        RecurringRule incoming = buildRule(user, category.getId(), TransactionPeriodType.MONTHLY, RecurringType.INCOME);
        incoming.setId(id);

        doReturn(Optional.of(existing)).when(repository).findByOwnerIdAndId(user.getId(), id);
        doReturn(category).when(categoryService).findByIdAndOwnerOrOwnerIsNull(category.getId(), user);
        doReturn(true).when(repository)
                .existsRecurringRuleByOwnerIdAndCategoryIdAndPriceAndTransactionDateAndDescriptionAndIdNot(
                        any(), any(), any(), any(), any(), any());

        assertThrows(ResourceAlreadyExistsException.class, () -> service.update(incoming));
        verify(repository, never()).save(any());
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
