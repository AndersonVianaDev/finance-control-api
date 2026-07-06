package com.andersonvianadev.finance_control_api.domain.services.impl;

import com.andersonvianadev.finance_control_api.domain.models.Expense;
import com.andersonvianadev.finance_control_api.domain.models.Income;
import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.models.dtos.SummaryResponseDTO;
import com.andersonvianadev.finance_control_api.domain.services.IExpenseService;
import com.andersonvianadev.finance_control_api.domain.services.IFinancialSummaryService;
import com.andersonvianadev.finance_control_api.domain.services.IIncomeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinancialSummaryImpl implements IFinancialSummaryService {

    private final IExpenseService expenseService;
    private final IIncomeService incomeService;

    @Override
    public SummaryResponseDTO summarize(User owner, LocalDate start, LocalDate finish) {
        LocalDate now = LocalDate.now();

        if(start == null) {
            int month = now.getMonthValue();
            int year = now.getYear();

            start = LocalDate.of(year, month, 1);
            log.debug("Start date set for standard: {}", start);
        }

        if(finish == null) {
            finish = now.with(TemporalAdjusters.lastDayOfMonth());
            log.debug("End date set for standard: {}", finish);
        }

        LocalDate realizedEnd = finish.isAfter(now) ? now : finish;

        BigDecimal realizedIncome;
        BigDecimal realizedExpense;

        if (realizedEnd.isBefore(start)) {
            realizedIncome = BigDecimal.ZERO;
            realizedExpense = BigDecimal.ZERO;
        } else {
            realizedIncome = incomeService.sumByOwnerAndDateRange(owner, start, realizedEnd);
            realizedExpense = expenseService.sumByOwnerAndDateRange(owner, start, realizedEnd);
        }

        long scheduledCount = 0;
        BigDecimal totalScheduledExpenses = BigDecimal.ZERO;
        BigDecimal totalScheduledIncomes = BigDecimal.ZERO;

        long projectedCount = 0;
        BigDecimal totalProjectedExpenses = BigDecimal.ZERO;
        BigDecimal totalProjectedIncomes = BigDecimal.ZERO;

        if(finish.isAfter(now)) {
            LocalDate futureStart = start.isAfter(now) ? start : now.plusDays(1);

            List<Expense> scheduledExpenses = expenseService.findScheduledExpensesByDateRange(owner, futureStart, finish);
            List<Income> scheduledIncomes = incomeService.findScheduledIncomesByDateRange(owner, futureStart, finish);

            scheduledCount = scheduledExpenses.size() + scheduledIncomes.size();

            totalScheduledExpenses = scheduledExpenses.stream()
                    .map(Expense::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalScheduledIncomes = scheduledIncomes.stream()
                    .map(Income::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            List<Expense> projectedExpenses = expenseService.findProjectedExpensesByDateRange(owner, futureStart, finish);
            List<Income> projectedIncomes = incomeService.findProjectedIncomesByDateRange(owner, futureStart, finish);

            projectedCount = projectedExpenses.size() + projectedIncomes.size();

            totalProjectedExpenses = projectedExpenses.stream()
                    .map(Expense::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalProjectedIncomes = projectedIncomes.stream()
                    .map(Income::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        long totalTransactions = realizedEnd.isBefore(start) ? 0 :
                incomeService.countByOwnerAndDateRange(owner, start, realizedEnd) +
                expenseService.countByOwnerAndDateRange(owner, start, realizedEnd);

        BigDecimal balance = realizedIncome.subtract(realizedExpense);
        BigDecimal balanceScheduled = totalScheduledIncomes.subtract(totalScheduledExpenses);
        BigDecimal balanceProjected = totalProjectedIncomes.subtract(totalProjectedExpenses);
        BigDecimal balanceTotal = balance.add(balanceScheduled).add(balanceProjected);
        long totalCount = totalTransactions + scheduledCount + projectedCount;

        BigDecimal commitmentRate = BigDecimal.ZERO;
        if (realizedIncome.compareTo(BigDecimal.ZERO) > 0) {
            commitmentRate = realizedExpense
                    .divide(realizedIncome, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        SummaryResponseDTO summary = new SummaryResponseDTO(
                realizedIncome,
                realizedExpense,
                balance,
                totalTransactions,
                totalScheduledIncomes,
                totalScheduledExpenses,
                balanceScheduled,
                scheduledCount,
                totalProjectedIncomes,
                totalProjectedExpenses,
                balanceProjected,
                projectedCount,
                balanceTotal,
                totalCount,
                commitmentRate
        );
        log.debug("User financial summary successfully retrieved. Owner: {}, DateStart: {}, EndDate: {}, FinancialSummary: {}",
                owner.getId(), start, finish, summary);

        return summary;
    }
}
