package com.andersonvianadev.finance_control_api.domain.services.impl;

import com.andersonvianadev.finance_control_api.domain.models.Category;
import com.andersonvianadev.finance_control_api.domain.models.Expense;
import com.andersonvianadev.finance_control_api.domain.models.InstallmentPlan;
import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.models.dtos.CalendarDTO;
import com.andersonvianadev.finance_control_api.domain.models.enums.UserRole;
import com.andersonvianadev.finance_control_api.domain.services.IBudgetService;
import com.andersonvianadev.finance_control_api.domain.services.ICalendarService;
import com.andersonvianadev.finance_control_api.domain.services.ICategoryService;
import com.andersonvianadev.finance_control_api.infra.exceptions.DeleteNotAllowedException;
import com.andersonvianadev.finance_control_api.infra.exceptions.ExternalServiceException;
import com.andersonvianadev.finance_control_api.infra.exceptions.NotFoundException;
import com.andersonvianadev.finance_control_api.infra.exceptions.ResourceAlreadyExistsException;
import com.andersonvianadev.finance_control_api.infra.repositories.ExpenseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceImplTest {

    @Mock
    private ExpenseRepository repository;

    @Mock
    private ICategoryService categoryService;

    @Mock
    private IBudgetService budgetService;

    @Mock
    private ICalendarService calendarService;

    @InjectMocks
    private ExpenseServiceImpl service;

    // ── non-installment ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Should save expense and validate budget when expense is valid and skipBudget is false")
    void save_WhenExpenseIsValidAndSkipBudgetIsFalse_ShouldValidateBudgetAndSave() {
        User user = buildUser();
        Category category = buildCategory(user);
        LocalDateTime date = LocalDateTime.of(2026, 6, 13, 10, 0);

        Expense expense = buildExpense(user, category.getId(), date);
        Expense saved = Expense.builder().id(UUID.randomUUID()).owner(user).category(category)
                .transactionDate(date).price(expense.getPrice()).description(expense.getDescription()).build();

        doReturn(category).when(categoryService).findByIdAndOwnerOrOwnerIsNull(category.getId(), user);
        doReturn(false).when(repository).existsDuplicate(user.getId(), category.getId(), expense.getPrice(), date, expense.getDescription());
        doReturn(saved).when(repository).save(expense);

        Expense result = service.save(expense, false);

        verify(calendarService, never()).getDate(any());
        verify(budgetService, times(1)).validateTransactionRespectsBudget(user, category, expense.getPrice());
        verify(repository, times(1)).save(expense);
        assertEquals(saved, result);
    }

    @Test
    @DisplayName("Should save expense without budget validation when skipBudget is true")
    void save_WhenSkipBudgetIsTrue_ShouldNotValidateBudget() {
        User user = buildUser();
        Category category = buildCategory(user);
        LocalDateTime date = LocalDateTime.of(2026, 6, 13, 10, 0);

        Expense expense = buildExpense(user, category.getId(), date);
        Expense saved = Expense.builder().id(UUID.randomUUID()).build();

        doReturn(category).when(categoryService).findByIdAndOwnerOrOwnerIsNull(category.getId(), user);
        doReturn(false).when(repository).existsDuplicate(user.getId(), category.getId(), expense.getPrice(), date, expense.getDescription());
        doReturn(saved).when(repository).save(expense);

        service.save(expense, true);

        verify(calendarService, never()).getDate(any());
        verify(budgetService, never()).validateTransactionRespectsBudget(any(), any(), any());
        verify(repository, times(1)).save(expense);
    }

    @Test
    @DisplayName("Should throw NotFoundException when category does not exist")
    void save_WhenCategoryDoesNotExist_ShouldThrowNotFoundException() {
        User user = buildUser();
        UUID categoryId = UUID.randomUUID();

        Expense expense = buildExpense(user, categoryId, LocalDateTime.of(2026, 6, 13, 10, 0));

        doThrow(new NotFoundException("Category not found."))
                .when(categoryService).findByIdAndOwnerOrOwnerIsNull(categoryId, user);

        assertThrows(NotFoundException.class, () -> service.save(expense, false));

        verify(calendarService, never()).getDate(any());
        verify(repository, never()).existsDuplicate(any(), any(), any(), any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceAlreadyExistsException when expense is duplicate")
    void save_WhenExpenseIsDuplicate_ShouldThrowResourceAlreadyExistsException() {
        User user = buildUser();
        Category category = buildCategory(user);
        LocalDateTime date = LocalDateTime.of(2026, 6, 13, 10, 0);

        Expense expense = buildExpense(user, category.getId(), date);

        doReturn(category).when(categoryService).findByIdAndOwnerOrOwnerIsNull(category.getId(), user);
        doReturn(true).when(repository).existsDuplicate(user.getId(), category.getId(), expense.getPrice(), date, expense.getDescription());

        assertThrows(ResourceAlreadyExistsException.class, () -> service.save(expense, false));

        verify(calendarService, never()).getDate(any());
        verify(budgetService, never()).validateTransactionRespectsBudget(any(), any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceAlreadyExistsException when database constraint is violated")
    void save_WhenDataIntegrityViolationOccurs_ShouldThrowResourceAlreadyExistsException() {
        User user = buildUser();
        Category category = buildCategory(user);
        LocalDateTime date = LocalDateTime.of(2026, 6, 13, 10, 0);

        Expense expense = buildExpense(user, category.getId(), date);

        doReturn(category).when(categoryService).findByIdAndOwnerOrOwnerIsNull(category.getId(), user);
        doReturn(false).when(repository).existsDuplicate(user.getId(), category.getId(), expense.getPrice(), date, expense.getDescription());
        doThrow(new DataIntegrityViolationException("duplicate")).when(repository).save(expense);

        assertThrows(ResourceAlreadyExistsException.class, () -> service.save(expense, false));

        verify(repository, times(1)).save(expense);
    }

    // ── installment ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should not change transaction date when installment date is a banking working day")
    void save_WhenInstallmentDateIsWorkingDay_ShouldNotChangeDate() {
        User user = buildUser();
        Category category = buildCategory(user);
        LocalDateTime date = LocalDateTime.of(2026, 6, 15, 0, 0);

        Expense expense = buildInstallmentExpense(user, category.getId(), date);
        CalendarDTO calendarDTO = new CalendarDTO(date.toLocalDate(), true, null, null);

        doReturn(category).when(categoryService).findByIdAndOwnerOrOwnerIsNull(category.getId(), user);
        doReturn(calendarDTO).when(calendarService).getDate(date.toLocalDate());
        doReturn(Expense.builder().id(UUID.randomUUID()).build()).when(repository).save(expense);

        service.save(expense, true);

        assertEquals(date, expense.getTransactionDate());
        verify(calendarService, times(1)).getDate(date.toLocalDate());
        verify(repository, times(1)).save(expense);
    }

    @Test
    @DisplayName("Should shift transaction date to next banking working day when installment date is not a working day")
    void save_WhenInstallmentDateIsNotWorkingDay_ShouldShiftToNextWorkingDay() {
        User user = buildUser();
        Category category = buildCategory(user);
        LocalDateTime nonWorkingDate = LocalDateTime.of(2026, 6, 20, 0, 0);
        LocalDate nextWorkingDay = LocalDate.of(2026, 6, 22);

        Expense expense = buildInstallmentExpense(user, category.getId(), nonWorkingDate);
        CalendarDTO calendarDTO = new CalendarDTO(nonWorkingDate.toLocalDate(), false, "Sábado", nextWorkingDay);

        doReturn(category).when(categoryService).findByIdAndOwnerOrOwnerIsNull(category.getId(), user);
        doReturn(calendarDTO).when(calendarService).getDate(nonWorkingDate.toLocalDate());
        doReturn(Expense.builder().id(UUID.randomUUID()).build()).when(repository).save(expense);

        service.save(expense, true);

        assertEquals(nextWorkingDay.atStartOfDay(), expense.getTransactionDate());
        verify(calendarService, times(1)).getDate(nonWorkingDate.toLocalDate());
        verify(repository, times(1)).save(expense);
    }

    @Test
    @DisplayName("Should throw ExternalServiceException when calendar API is unavailable during installment save")
    void save_WhenCalendarApiIsUnavailable_ShouldThrowExternalServiceException() {
        User user = buildUser();
        Category category = buildCategory(user);
        LocalDateTime date = LocalDateTime.of(2026, 6, 15, 0, 0);

        Expense expense = buildInstallmentExpense(user, category.getId(), date);

        doReturn(category).when(categoryService).findByIdAndOwnerOrOwnerIsNull(category.getId(), user);
        doThrow(new ExternalServiceException("calendar-api", new RuntimeException("connection refused")))
                .when(calendarService).getDate(date.toLocalDate());

        assertThrows(ExternalServiceException.class, () -> service.save(expense, false));

        verify(repository, never()).save(any());
    }

    // ── findById ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return expense when it exists and belongs to user")
    void findById_WhenExpenseExists_ShouldReturnExpense() {
        User user = buildUser();
        Category category = buildCategory(user);
        UUID expenseId = UUID.randomUUID();
        Expense expense = Expense.builder().id(expenseId).owner(user).category(category).build();

        doReturn(java.util.Optional.of(expense)).when(repository).findExpenseByOwnerIdAndId(user.getId(), expenseId);

        Expense result = service.findById(user, expenseId);

        assertEquals(expense, result);
        verify(repository, times(1)).findExpenseByOwnerIdAndId(user.getId(), expenseId);
    }

    @Test
    @DisplayName("Should throw NotFoundException when expense does not exist")
    void findById_WhenExpenseDoesNotExist_ShouldThrowNotFoundException() {
        User user = buildUser();
        UUID expenseId = UUID.randomUUID();

        doReturn(java.util.Optional.empty()).when(repository).findExpenseByOwnerIdAndId(user.getId(), expenseId);

        assertThrows(NotFoundException.class, () -> service.findById(user, expenseId));

        verify(repository, times(1)).findExpenseByOwnerIdAndId(user.getId(), expenseId);
    }

    // ── findAll ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return paginated expenses for user")
    void findAll_WhenExpensesExist_ShouldReturnPage() {
        User user = buildUser();
        Category category = buildCategory(user);
        Pageable pageable = PageRequest.of(0, 10);
        Expense expense = Expense.builder().id(UUID.randomUUID()).owner(user).category(category).build();
        Page<Expense> page = new PageImpl<>(List.of(expense), pageable, 1);

        doReturn(page).when(repository).findByOwnerId(user.getId(), pageable);

        Page<Expense> result = service.findAll(user, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(expense, result.getContent().getFirst());
        verify(repository, times(1)).findByOwnerId(user.getId(), pageable);
    }

    @Test
    @DisplayName("Should return empty page when user has no expenses")
    void findAll_WhenNoExpensesExist_ShouldReturnEmptyPage() {
        User user = buildUser();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Expense> page = new PageImpl<>(List.of(), pageable, 0);

        doReturn(page).when(repository).findByOwnerId(user.getId(), pageable);

        Page<Expense> result = service.findAll(user, pageable);

        assertEquals(0, result.getTotalElements());
        verify(repository, times(1)).findByOwnerId(user.getId(), pageable);
    }

    // ── delete ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should delete expense successfully when it is not an installment")
    void delete_WhenExpenseIsNotInstallment_ShouldDelete() {
        User user = buildUser();
        Category category = buildCategory(user);
        UUID expenseId = UUID.randomUUID();
        Expense expense = buildExpense(user, category.getId(), LocalDateTime.of(2026, 7, 1, 10, 0));
        expense.setId(expenseId);

        doReturn(java.util.Optional.of(expense)).when(repository).findExpenseByOwnerIdAndId(user.getId(), expenseId);
        doNothing().when(repository).delete(expense);

        service.delete(user, expenseId);

        verify(repository, times(1)).delete(expense);
    }

    @Test
    @DisplayName("Should throw DeleteNotAllowedException when expense is linked to an installment plan")
    void delete_WhenExpenseIsInstallment_ShouldThrowDeleteNotAllowedException() {
        User user = buildUser();
        Category category = buildCategory(user);
        UUID expenseId = UUID.randomUUID();
        Expense expense = buildInstallmentExpense(user, category.getId(), LocalDateTime.of(2026, 7, 1, 0, 0));
        expense.setId(expenseId);

        doReturn(java.util.Optional.of(expense)).when(repository).findExpenseByOwnerIdAndId(user.getId(), expenseId);

        assertThrows(DeleteNotAllowedException.class, () -> service.delete(user, expenseId));

        verify(repository, never()).delete(any());
    }

    @Test
    @DisplayName("Should throw NotFoundException when expense to delete does not exist")
    void delete_WhenExpenseDoesNotExist_ShouldThrowNotFoundException() {
        User user = buildUser();
        UUID expenseId = UUID.randomUUID();

        doReturn(java.util.Optional.empty()).when(repository).findExpenseByOwnerIdAndId(user.getId(), expenseId);

        assertThrows(NotFoundException.class, () -> service.delete(user, expenseId));

        verify(repository, never()).delete(any());
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
                .name("technology")
                .description("Electronics")
                .icon("tech")
                .owner(owner)
                .build();
    }

    private Expense buildExpense(User owner, UUID categoryId, LocalDateTime date) {
        return Expense.builder()
                .owner(owner)
                .category(Category.builder().id(categoryId).build())
                .transactionDate(date)
                .price(new BigDecimal("150.00"))
                .description("Gym membership")
                .build();
    }

    private Expense buildInstallmentExpense(User owner, UUID categoryId, LocalDateTime date) {
        return Expense.builder()
                .owner(owner)
                .category(Category.builder().id(categoryId).build())
                .transactionDate(date)
                .price(new BigDecimal("1500.00"))
                .description("MacBook 1/12")
                .installmentPlan(InstallmentPlan.builder().id(UUID.randomUUID()).build())
                .installmentNumber(1)
                .build();
    }
}
