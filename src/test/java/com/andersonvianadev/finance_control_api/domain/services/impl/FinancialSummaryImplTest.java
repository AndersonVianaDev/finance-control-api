package com.andersonvianadev.finance_control_api.domain.services.impl;

import com.andersonvianadev.finance_control_api.domain.models.Category;
import com.andersonvianadev.finance_control_api.domain.models.Expense;
import com.andersonvianadev.finance_control_api.domain.models.Income;
import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.models.dtos.SummaryResponseDTO;
import com.andersonvianadev.finance_control_api.domain.models.enums.UserRole;
import com.andersonvianadev.finance_control_api.domain.services.IExpenseService;
import com.andersonvianadev.finance_control_api.domain.services.IIncomeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinancialSummaryImplTest {

    @Mock
    private IExpenseService expenseService;

    @Mock
    private IIncomeService incomeService;

    @InjectMocks
    private FinancialSummaryImpl service;

    // ── summarize (past period) ───────────────────────────────────────────────

    @Test
    @DisplayName("Should return only realized data when period is entirely in the past")
    void summarize_WhenPeriodIsEntirelyInPast_ShouldReturnOnlyRealizedBucketWithZeroFuture() {
        User owner = buildUser();
        LocalDate start = LocalDate.now().minusMonths(2).withDayOfMonth(1);
        LocalDate finish = LocalDate.now().minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());

        doReturn(new BigDecimal("5000.00")).when(incomeService).sumByOwnerAndDateRange(owner, start, finish);
        doReturn(new BigDecimal("2000.00")).when(expenseService).sumByOwnerAndDateRange(owner, start, finish);
        doReturn(3).when(incomeService).countByOwnerAndDateRange(owner, start, finish);
        doReturn(2).when(expenseService).countByOwnerAndDateRange(owner, start, finish);

        SummaryResponseDTO result = service.summarize(owner, start, finish);

        assertEquals(new BigDecimal("5000.00"), result.realizedIncome());
        assertEquals(new BigDecimal("2000.00"), result.realizedExpense());
        assertEquals(new BigDecimal("3000.00"), result.realizedBalance());
        assertEquals(5L, result.realizedCount());
        assertEquals(BigDecimal.ZERO, result.scheduledIncome());
        assertEquals(BigDecimal.ZERO, result.scheduledExpense());
        assertEquals(BigDecimal.ZERO, result.scheduledBalance());
        assertEquals(0L, result.scheduledCount());
        assertEquals(BigDecimal.ZERO, result.projectedIncome());
        assertEquals(BigDecimal.ZERO, result.projectedExpense());
        assertEquals(BigDecimal.ZERO, result.projectedBalance());
        assertEquals(0L, result.projectedCount());
        assertEquals(new BigDecimal("3000.00"), result.totalBalance());
        assertEquals(5L, result.totalCount());
    }

    @Test
    @DisplayName("Should not call scheduled or projected methods when period is entirely in the past")
    void summarize_WhenPeriodIsEntirelyInPast_ShouldNotCallFutureBucketMethods() {
        User owner = buildUser();
        LocalDate start = LocalDate.now().minusMonths(2).withDayOfMonth(1);
        LocalDate finish = LocalDate.now().minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());

        doReturn(BigDecimal.ZERO).when(incomeService).sumByOwnerAndDateRange(owner, start, finish);
        doReturn(BigDecimal.ZERO).when(expenseService).sumByOwnerAndDateRange(owner, start, finish);
        doReturn(0).when(incomeService).countByOwnerAndDateRange(owner, start, finish);
        doReturn(0).when(expenseService).countByOwnerAndDateRange(owner, start, finish);

        service.summarize(owner, start, finish);

        verify(expenseService, never()).findScheduledExpensesByDateRange(any(), any(), any());
        verify(incomeService, never()).findScheduledIncomesByDateRange(any(), any(), any());
        verify(expenseService, never()).findProjectedExpensesByDateRange(any(), any(), any());
        verify(incomeService, never()).findProjectedIncomesByDateRange(any(), any(), any());
    }

    // ── summarize (period with future) ───────────────────────────────────────

    @Test
    @DisplayName("Should populate all three buckets when period extends beyond today")
    void summarize_WhenPeriodExtendsBeyondToday_ShouldPopulateAllThreeBuckets() {
        User owner = buildUser();
        Category category = buildCategory(owner);
        LocalDate start = LocalDate.now().minusMonths(1).withDayOfMonth(1);
        LocalDate finish = LocalDate.now().plusMonths(1).with(TemporalAdjusters.lastDayOfMonth());

        Expense scheduledExpense = buildExpense(owner, category, new BigDecimal("500.00"));
        Income scheduledIncome = buildIncome(owner, category, new BigDecimal("300.00"));
        Expense projectedExpense = buildExpense(owner, category, new BigDecimal("200.00"));
        Income projectedIncome = buildIncome(owner, category, new BigDecimal("1000.00"));

        doReturn(new BigDecimal("5000.00")).when(incomeService).sumByOwnerAndDateRange(eq(owner), any(), any());
        doReturn(new BigDecimal("2000.00")).when(expenseService).sumByOwnerAndDateRange(eq(owner), any(), any());
        doReturn(3).when(incomeService).countByOwnerAndDateRange(eq(owner), any(), any());
        doReturn(2).when(expenseService).countByOwnerAndDateRange(eq(owner), any(), any());
        doReturn(List.of(scheduledExpense)).when(expenseService).findScheduledExpensesByDateRange(eq(owner), any(), any());
        doReturn(List.of(scheduledIncome)).when(incomeService).findScheduledIncomesByDateRange(eq(owner), any(), any());
        doReturn(List.of(projectedExpense)).when(expenseService).findProjectedExpensesByDateRange(eq(owner), any(), any());
        doReturn(List.of(projectedIncome)).when(incomeService).findProjectedIncomesByDateRange(eq(owner), any(), any());

        SummaryResponseDTO result = service.summarize(owner, start, finish);

        assertEquals(new BigDecimal("5000.00"), result.realizedIncome());
        assertEquals(new BigDecimal("2000.00"), result.realizedExpense());
        assertEquals(new BigDecimal("500.00"), result.scheduledExpense());
        assertEquals(new BigDecimal("300.00"), result.scheduledIncome());
        assertEquals(new BigDecimal("200.00"), result.projectedExpense());
        assertEquals(new BigDecimal("1000.00"), result.projectedIncome());
        assertEquals(2L, result.scheduledCount());
        assertEquals(2L, result.projectedCount());
        assertEquals(5L, result.realizedCount());
    }

    @Test
    @DisplayName("Should compute correct totalBalance across all three buckets")
    void summarize_WhenAllBucketsHaveValues_ShouldComputeCorrectTotalBalance() {
        User owner = buildUser();
        Category category = buildCategory(owner);
        LocalDate start = LocalDate.now().minusMonths(1).withDayOfMonth(1);
        LocalDate finish = LocalDate.now().plusMonths(1).with(TemporalAdjusters.lastDayOfMonth());

        doReturn(new BigDecimal("5000.00")).when(incomeService).sumByOwnerAndDateRange(eq(owner), any(), any());
        doReturn(new BigDecimal("2000.00")).when(expenseService).sumByOwnerAndDateRange(eq(owner), any(), any());
        doReturn(0).when(incomeService).countByOwnerAndDateRange(eq(owner), any(), any());
        doReturn(0).when(expenseService).countByOwnerAndDateRange(eq(owner), any(), any());
        doReturn(List.of(buildExpense(owner, category, new BigDecimal("500.00")))).when(expenseService)
                .findScheduledExpensesByDateRange(eq(owner), any(), any());
        doReturn(List.of(buildIncome(owner, category, new BigDecimal("300.00")))).when(incomeService)
                .findScheduledIncomesByDateRange(eq(owner), any(), any());
        doReturn(List.of(buildExpense(owner, category, new BigDecimal("200.00")))).when(expenseService)
                .findProjectedExpensesByDateRange(eq(owner), any(), any());
        doReturn(List.of(buildIncome(owner, category, new BigDecimal("1000.00")))).when(incomeService)
                .findProjectedIncomesByDateRange(eq(owner), any(), any());

        SummaryResponseDTO result = service.summarize(owner, start, finish);

        // realizedBalance = 5000 - 2000 = 3000
        // scheduledBalance = 300 - 500 = -200
        // projectedBalance = 1000 - 200 = 800
        // totalBalance = 3000 + (-200) + 800 = 3600
        assertEquals(0, new BigDecimal("3600.00").compareTo(result.totalBalance()));
    }

    @Test
    @DisplayName("Should compute correct totalCount as sum of all three bucket counts")
    void summarize_WhenAllBucketsHaveValues_ShouldComputeCorrectTotalCount() {
        User owner = buildUser();
        Category category = buildCategory(owner);
        LocalDate start = LocalDate.now().minusMonths(1).withDayOfMonth(1);
        LocalDate finish = LocalDate.now().plusMonths(1).with(TemporalAdjusters.lastDayOfMonth());

        doReturn(BigDecimal.ZERO).when(incomeService).sumByOwnerAndDateRange(eq(owner), any(), any());
        doReturn(BigDecimal.ZERO).when(expenseService).sumByOwnerAndDateRange(eq(owner), any(), any());
        doReturn(3).when(incomeService).countByOwnerAndDateRange(eq(owner), any(), any());
        doReturn(2).when(expenseService).countByOwnerAndDateRange(eq(owner), any(), any());
        doReturn(List.of(
                buildExpense(owner, category, new BigDecimal("500.00")),
                buildExpense(owner, category, new BigDecimal("300.00"))
        )).when(expenseService).findScheduledExpensesByDateRange(eq(owner), any(), any());
        doReturn(List.of(buildIncome(owner, category, new BigDecimal("200.00")))).when(incomeService)
                .findScheduledIncomesByDateRange(eq(owner), any(), any());
        doReturn(List.of(buildExpense(owner, category, new BigDecimal("100.00")))).when(expenseService)
                .findProjectedExpensesByDateRange(eq(owner), any(), any());
        doReturn(List.of()).when(incomeService).findProjectedIncomesByDateRange(eq(owner), any(), any());

        SummaryResponseDTO result = service.summarize(owner, start, finish);

        // realizedCount = 3 + 2 = 5
        // scheduledCount = 2 + 1 = 3
        // projectedCount = 1 + 0 = 1
        // totalCount = 9
        assertEquals(5L, result.realizedCount());
        assertEquals(3L, result.scheduledCount());
        assertEquals(1L, result.projectedCount());
        assertEquals(9L, result.totalCount());
    }

    // ── summarize (future period) ─────────────────────────────────────────────

    @Test
    @DisplayName("Should return zero realized amounts when period is entirely in the future")
    void summarize_WhenPeriodIsEntirelyInFuture_ShouldReturnZeroRealizedAndPopulateFutureBuckets() {
        User owner = buildUser();
        Category category = buildCategory(owner);
        LocalDate start = LocalDate.now().plusMonths(1).withDayOfMonth(1);
        LocalDate finish = LocalDate.now().plusMonths(2).with(TemporalAdjusters.lastDayOfMonth());

        doReturn(List.of(buildExpense(owner, category, new BigDecimal("1500.00")))).when(expenseService)
                .findScheduledExpensesByDateRange(eq(owner), eq(start), eq(finish));
        doReturn(List.of()).when(incomeService).findScheduledIncomesByDateRange(eq(owner), eq(start), eq(finish));
        doReturn(List.of()).when(expenseService).findProjectedExpensesByDateRange(eq(owner), eq(start), eq(finish));
        doReturn(List.of()).when(incomeService).findProjectedIncomesByDateRange(eq(owner), eq(start), eq(finish));

        SummaryResponseDTO result = service.summarize(owner, start, finish);

        assertEquals(BigDecimal.ZERO, result.realizedIncome());
        assertEquals(BigDecimal.ZERO, result.realizedExpense());
        assertEquals(BigDecimal.ZERO, result.realizedBalance());
        assertEquals(0L, result.realizedCount());
        assertEquals(new BigDecimal("1500.00"), result.scheduledExpense());
        assertEquals(1L, result.scheduledCount());
        assertEquals(BigDecimal.ZERO, result.commitmentRate());

        verify(incomeService, never()).sumByOwnerAndDateRange(any(), any(), any());
        verify(expenseService, never()).sumByOwnerAndDateRange(any(), any(), any());
    }

    // ── summarize (null dates) ────────────────────────────────────────────────

    @Test
    @DisplayName("Should default to current month when both dates are null")
    void summarize_WhenNullDates_ShouldDefaultToCurrentMonthAndCallSumService() {
        User owner = buildUser();

        doReturn(BigDecimal.ZERO).when(incomeService).sumByOwnerAndDateRange(eq(owner), any(), any());
        doReturn(BigDecimal.ZERO).when(expenseService).sumByOwnerAndDateRange(eq(owner), any(), any());
        doReturn(0).when(incomeService).countByOwnerAndDateRange(eq(owner), any(), any());
        doReturn(0).when(expenseService).countByOwnerAndDateRange(eq(owner), any(), any());
        // future bucket methods may or may not be called depending on whether today is the last day of the month
        lenient().doReturn(List.of()).when(expenseService).findScheduledExpensesByDateRange(eq(owner), any(), any());
        lenient().doReturn(List.of()).when(incomeService).findScheduledIncomesByDateRange(eq(owner), any(), any());
        lenient().doReturn(List.of()).when(expenseService).findProjectedExpensesByDateRange(eq(owner), any(), any());
        lenient().doReturn(List.of()).when(incomeService).findProjectedIncomesByDateRange(eq(owner), any(), any());

        SummaryResponseDTO result = service.summarize(owner, null, null);

        assertNotNull(result);
        verify(incomeService, atLeastOnce()).sumByOwnerAndDateRange(eq(owner), any(), any());
        verify(expenseService, atLeastOnce()).sumByOwnerAndDateRange(eq(owner), any(), any());
    }

    // ── summarize (commitmentRate) ────────────────────────────────────────────

    @Test
    @DisplayName("Should return commitmentRate as zero when realized income is zero")
    void summarize_WhenRealizedIncomeIsZero_ShouldReturnCommitmentRateAsZero() {
        User owner = buildUser();
        LocalDate start = LocalDate.now().minusMonths(2).withDayOfMonth(1);
        LocalDate finish = LocalDate.now().minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());

        doReturn(BigDecimal.ZERO).when(incomeService).sumByOwnerAndDateRange(owner, start, finish);
        doReturn(new BigDecimal("200.00")).when(expenseService).sumByOwnerAndDateRange(owner, start, finish);
        doReturn(0).when(incomeService).countByOwnerAndDateRange(owner, start, finish);
        doReturn(1).when(expenseService).countByOwnerAndDateRange(owner, start, finish);

        SummaryResponseDTO result = service.summarize(owner, start, finish);

        assertEquals(BigDecimal.ZERO, result.commitmentRate());
    }

    @Test
    @DisplayName("Should calculate commitment rate correctly when both income and expense are positive")
    void summarize_WhenRealizedIncomeAndExpenseArePositive_ShouldCalculateCommitmentRateCorrectly() {
        User owner = buildUser();
        LocalDate start = LocalDate.now().minusMonths(2).withDayOfMonth(1);
        LocalDate finish = LocalDate.now().minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());

        doReturn(new BigDecimal("5000.00")).when(incomeService).sumByOwnerAndDateRange(owner, start, finish);
        doReturn(new BigDecimal("2000.00")).when(expenseService).sumByOwnerAndDateRange(owner, start, finish);
        doReturn(0).when(incomeService).countByOwnerAndDateRange(owner, start, finish);
        doReturn(0).when(expenseService).countByOwnerAndDateRange(owner, start, finish);

        SummaryResponseDTO result = service.summarize(owner, start, finish);

        assertEquals(0, new BigDecimal("40.00").compareTo(result.commitmentRate()));
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

    private Expense buildExpense(User owner, Category category, BigDecimal price) {
        return Expense.builder()
                .owner(owner)
                .category(category)
                .transactionDate(LocalDateTime.now())
                .price(price)
                .description("Test expense")
                .build();
    }

    private Income buildIncome(User owner, Category category, BigDecimal price) {
        return Income.builder()
                .owner(owner)
                .category(category)
                .transactionDate(LocalDateTime.now())
                .price(price)
                .description("Test income")
                .build();
    }
}
