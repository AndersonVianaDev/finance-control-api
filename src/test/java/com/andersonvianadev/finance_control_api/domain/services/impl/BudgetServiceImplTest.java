package com.andersonvianadev.finance_control_api.domain.services.impl;

import com.andersonvianadev.finance_control_api.domain.models.Budget;
import com.andersonvianadev.finance_control_api.domain.models.Category;
import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.models.enums.BudgetType;
import com.andersonvianadev.finance_control_api.domain.models.enums.UserRole;
import com.andersonvianadev.finance_control_api.domain.services.ICategoryService;
import com.andersonvianadev.finance_control_api.infra.exceptions.BudgetExceededException;
import com.andersonvianadev.finance_control_api.infra.exceptions.NotFoundException;
import com.andersonvianadev.finance_control_api.infra.exceptions.ResourceAlreadyExistsException;
import com.andersonvianadev.finance_control_api.infra.repositories.BudgetRepository;
import com.andersonvianadev.finance_control_api.infra.repositories.ExpenseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BudgetServiceImplTest {

    @Mock
    private BudgetRepository repository;

    @Mock
    private ICategoryService categoryService;

    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private BudgetServiceImpl service;

    @Test
    @DisplayName("Should save budget successfully when budget is valid")
    void save_WhenBudgetIsValid_ShouldReturnSavedBudget() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Anderson")
                .email("anderson@gmail.com")
                .password("password")
                .role(UserRole.ROLE_USER)
                .build();

        Category category = Category.builder()
                .id(UUID.randomUUID())
                .name("food")
                .description("food description")
                .icon("food")
                .owner(user)
                .build();

        Budget budgetRequest = Budget.builder()
                .owner(user)
                .category(Category.builder().id(category.getId()).build())
                .budgetType(BudgetType.MONTHLY)
                .limitAmount(new BigDecimal("1500.00"))
                .build();

        Budget budgetSaved = Budget.builder()
                .id(UUID.randomUUID())
                .owner(user)
                .category(category)
                .budgetType(BudgetType.MONTHLY)
                .limitAmount(new BigDecimal("1500.00"))
                .active(true)
                .build();

        doReturn(category).when(categoryService).findByIdAndOwnerOrOwnerIsNull(category.getId(), user);
        doReturn(false).when(repository).existsByCategoryAndOwner(category, user);
        doReturn(budgetSaved).when(repository).save(any());

        Budget result = service.save(budgetRequest);

        verify(categoryService, times(1)).findByIdAndOwnerOrOwnerIsNull(category.getId(), user);
        verify(repository, times(1)).existsByCategoryAndOwner(category, user);
        verify(repository, times(1)).save(any());
        assertEquals(budgetSaved, result);
    }

    @Test
    @DisplayName("Should throw NotFoundException when category does not exist")
    void save_WhenCategoryDoesNotExist_ShouldThrowNotFoundException() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Anderson")
                .email("anderson@gmail.com")
                .password("password")
                .role(UserRole.ROLE_USER)
                .build();

        UUID categoryId = UUID.randomUUID();

        Budget budgetRequest = Budget.builder()
                .owner(user)
                .category(Category.builder().id(categoryId).build())
                .budgetType(BudgetType.MONTHLY)
                .limitAmount(new BigDecimal("1500.00"))
                .build();

        doThrow(new NotFoundException("Category not found."))
                .when(categoryService).findByIdAndOwnerOrOwnerIsNull(categoryId, user);

        assertThrows(NotFoundException.class, () -> service.save(budgetRequest));

        verify(repository, never()).existsByCategoryAndOwner(any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceAlreadyExistsException when budget already exists for category")
    void save_WhenBudgetAlreadyExistsForCategory_ShouldThrowResourceAlreadyExistsException() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Anderson")
                .email("anderson@gmail.com")
                .password("password")
                .role(UserRole.ROLE_USER)
                .build();

        Category category = Category.builder()
                .id(UUID.randomUUID())
                .name("food")
                .description("food description")
                .icon("food")
                .owner(user)
                .build();

        Budget budgetRequest = Budget.builder()
                .owner(user)
                .category(Category.builder().id(category.getId()).build())
                .budgetType(BudgetType.MONTHLY)
                .limitAmount(new BigDecimal("1500.00"))
                .build();

        doReturn(category).when(categoryService).findByIdAndOwnerOrOwnerIsNull(category.getId(), user);
        doReturn(true).when(repository).existsByCategoryAndOwner(category, user);

        assertThrows(ResourceAlreadyExistsException.class, () -> service.save(budgetRequest));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceAlreadyExistsException when database constraint is violated")
    void save_ShouldThrowResourceAlreadyExistsException_WhenDataIntegrityViolationOccurs() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Anderson")
                .email("anderson@gmail.com")
                .password("password")
                .role(UserRole.ROLE_USER)
                .build();

        Category category = Category.builder()
                .id(UUID.randomUUID())
                .name("food")
                .description("food description")
                .icon("food")
                .owner(user)
                .build();

        Budget budgetRequest = Budget.builder()
                .owner(user)
                .category(Category.builder().id(category.getId()).build())
                .budgetType(BudgetType.MONTHLY)
                .limitAmount(new BigDecimal("1500.00"))
                .build();

        doReturn(category).when(categoryService).findByIdAndOwnerOrOwnerIsNull(category.getId(), user);
        doReturn(false).when(repository).existsByCategoryAndOwner(category, user);
        doThrow(new DataIntegrityViolationException("duplicate budget")).when(repository).save(any());

        assertThrows(ResourceAlreadyExistsException.class, () -> service.save(budgetRequest));

        verify(repository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should return budget when budget exists")
    void findById_WhenBudgetExists_ShouldReturnBudget() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Anderson")
                .email("anderson@gmail.com")
                .password("password")
                .role(UserRole.ROLE_USER)
                .build();

        Category category = Category.builder()
                .id(UUID.randomUUID())
                .name("food")
                .description("food description")
                .icon("food")
                .owner(user)
                .build();

        Budget budget = Budget.builder()
                .id(UUID.randomUUID())
                .owner(user)
                .category(category)
                .budgetType(BudgetType.MONTHLY)
                .limitAmount(new BigDecimal("1500.00"))
                .active(true)
                .build();

        doReturn(Optional.of(budget)).when(repository).findByIdAndOwner(budget.getId(), user);

        Budget result = service.findById(user, budget.getId());

        assertEquals(budget, result);
        verify(repository, times(1)).findByIdAndOwner(budget.getId(), user);
    }

    @Test
    @DisplayName("Should throw NotFoundException when budget does not exist")
    void findById_WhenBudgetDoesNotExist_ShouldThrowNotFoundException() {
        UUID id = UUID.randomUUID();

        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Anderson")
                .email("anderson@gmail.com")
                .password("password")
                .role(UserRole.ROLE_USER)
                .build();

        doReturn(Optional.empty()).when(repository).findByIdAndOwner(id, user);

        assertThrows(NotFoundException.class, () -> service.findById(user, id));

        verify(repository, times(1)).findByIdAndOwner(id, user);
    }

    @Test
    @DisplayName("Should update budget successfully when budget exists")
    void update_WhenBudgetExists_ShouldUpdateBudget() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Anderson")
                .email("anderson@gmail.com")
                .password("password")
                .role(UserRole.ROLE_USER)
                .build();

        Category category = Category.builder()
                .id(UUID.randomUUID())
                .name("food")
                .description("food description")
                .icon("food")
                .owner(user)
                .build();

        Budget budgetSaved = Budget.builder()
                .id(UUID.randomUUID())
                .owner(user)
                .category(category)
                .budgetType(BudgetType.MONTHLY)
                .limitAmount(new BigDecimal("1500.00"))
                .active(true)
                .build();

        Budget budgetUpdate = Budget.builder()
                .id(budgetSaved.getId())
                .owner(user)
                .budgetType(BudgetType.WEEKLY)
                .limitAmount(new BigDecimal("500.00"))
                .active(false)
                .build();

        doReturn(Optional.of(budgetSaved)).when(repository).findByIdAndOwner(budgetSaved.getId(), user);
        doAnswer(invocation -> invocation.getArgument(0)).when(repository).save(any());

        Budget result = service.update(budgetUpdate);

        assertEquals(BudgetType.WEEKLY, result.getBudgetType());
        assertEquals(0, new BigDecimal("500.00").compareTo(result.getLimitAmount()));
        assertEquals(false, result.getActive());
        verify(repository, times(1)).findByIdAndOwner(budgetSaved.getId(), user);
        verify(repository, times(1)).save(budgetSaved);
    }

    @Test
    @DisplayName("Should throw NotFoundException when budget does not exist")
    void update_WhenBudgetDoesNotExist_ShouldThrowNotFoundException() {
        UUID id = UUID.randomUUID();

        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Anderson")
                .email("anderson@gmail.com")
                .password("password")
                .role(UserRole.ROLE_USER)
                .build();

        Budget budgetUpdate = Budget.builder()
                .id(id)
                .owner(user)
                .budgetType(BudgetType.WEEKLY)
                .limitAmount(new BigDecimal("500.00"))
                .active(false)
                .build();

        doReturn(Optional.empty()).when(repository).findByIdAndOwner(id, user);

        assertThrows(NotFoundException.class, () -> service.update(budgetUpdate));

        verify(repository, times(1)).findByIdAndOwner(id, user);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should delete budget successfully when budget exists")
    void delete_WhenBudgetExists_ShouldDeleteBudget() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Anderson")
                .email("anderson@gmail.com")
                .password("password")
                .role(UserRole.ROLE_USER)
                .build();

        Category category = Category.builder()
                .id(UUID.randomUUID())
                .name("food")
                .description("food description")
                .icon("food")
                .owner(user)
                .build();

        Budget budget = Budget.builder()
                .id(UUID.randomUUID())
                .owner(user)
                .category(category)
                .budgetType(BudgetType.MONTHLY)
                .limitAmount(new BigDecimal("1500.00"))
                .active(true)
                .build();

        doReturn(Optional.of(budget)).when(repository).findByIdAndOwner(budget.getId(), user);

        service.delete(user, budget.getId());

        verify(repository, times(1)).findByIdAndOwner(budget.getId(), user);
        verify(repository, times(1)).delete(budget);
    }

    @Test
    @DisplayName("Should throw NotFoundException when budget does not exist")
    void delete_WhenBudgetDoesNotExist_ShouldThrowNotFoundException() {
        UUID id = UUID.randomUUID();

        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Anderson")
                .email("anderson@gmail.com")
                .password("password")
                .role(UserRole.ROLE_USER)
                .build();

        doReturn(Optional.empty()).when(repository).findByIdAndOwner(id, user);

        assertThrows(NotFoundException.class, () -> service.delete(user, id));

        verify(repository, times(1)).findByIdAndOwner(id, user);
        verify(repository, never()).delete(any());
    }

    @Test
    @DisplayName("Should return paginated budgets when budgets exist")
    void findAll_WhenBudgetsExist_ShouldReturnPageOfBudgets() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Anderson")
                .email("anderson@gmail.com")
                .password("password")
                .role(UserRole.ROLE_USER)
                .build();

        Category category = Category.builder()
                .id(UUID.randomUUID())
                .name("food")
                .description("food description")
                .icon("food")
                .owner(user)
                .build();

        Budget budget = Budget.builder()
                .id(UUID.randomUUID())
                .owner(user)
                .category(category)
                .budgetType(BudgetType.MONTHLY)
                .limitAmount(new BigDecimal("1500.00"))
                .active(true)
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<Budget> page = new PageImpl<>(List.of(budget), pageable, 1);

        doReturn(page).when(repository).findByOwner(user, pageable);

        Page<Budget> result = service.findAll(user, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(budget, result.getContent().getFirst());
        verify(repository, times(1)).findByOwner(user, pageable);
    }

    // ── validateTransactionRespectsBudget ─────────────────────────────────────

    @Test
    @DisplayName("Should pass validation when no active budget exists for category")
    void validateTransactionRespectsBudget_WhenNoBudgetExists_ShouldNotThrow() {
        User user = User.builder().id(UUID.randomUUID()).name("Anderson")
                .email("anderson@gmail.com").password("password").role(UserRole.ROLE_USER).build();
        Category category = Category.builder().id(UUID.randomUUID()).name("food")
                .description("food description").icon("food").owner(user).build();

        doReturn(Optional.empty()).when(repository).findByOwnerIdAndCategoryIdAndActiveTrue(user.getId(), category.getId());

        service.validateTransactionRespectsBudget(user, category, new BigDecimal("500.00"));

        verify(expenseRepository, never()).sumByOwnerAndCategoryAndDateRange(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should pass validation when budget exists and transaction is within limit")
    void validateTransactionRespectsBudget_WhenBudgetExistsAndTransactionIsWithinLimit_ShouldNotThrow() {
        User user = User.builder().id(UUID.randomUUID()).name("Anderson")
                .email("anderson@gmail.com").password("password").role(UserRole.ROLE_USER).build();
        Category category = Category.builder().id(UUID.randomUUID()).name("food")
                .description("food description").icon("food").owner(user).build();

        Budget budget = Budget.builder().id(UUID.randomUUID()).owner(user).category(category)
                .budgetType(BudgetType.MONTHLY).limitAmount(new BigDecimal("1000.00")).active(true).build();

        doReturn(Optional.of(budget)).when(repository).findByOwnerIdAndCategoryIdAndActiveTrue(user.getId(), category.getId());
        doReturn(new BigDecimal("400.00")).when(expenseRepository)
                .sumByOwnerAndCategoryAndDateRange(any(), any(), any(), any());

        service.validateTransactionRespectsBudget(user, category, new BigDecimal("500.00"));

        verify(expenseRepository, times(1)).sumByOwnerAndCategoryAndDateRange(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should throw BudgetExceededException when transaction would exceed budget limit")
    void validateTransactionRespectsBudget_WhenTransactionExceedsLimit_ShouldThrowBudgetExceededException() {
        User user = User.builder().id(UUID.randomUUID()).name("Anderson")
                .email("anderson@gmail.com").password("password").role(UserRole.ROLE_USER).build();
        Category category = Category.builder().id(UUID.randomUUID()).name("food")
                .description("food description").icon("food").owner(user).build();

        Budget budget = Budget.builder().id(UUID.randomUUID()).owner(user).category(category)
                .budgetType(BudgetType.MONTHLY).limitAmount(new BigDecimal("1000.00")).active(true).build();

        doReturn(Optional.of(budget)).when(repository).findByOwnerIdAndCategoryIdAndActiveTrue(user.getId(), category.getId());
        doReturn(new BigDecimal("800.00")).when(expenseRepository)
                .sumByOwnerAndCategoryAndDateRange(any(), any(), any(), any());

        assertThrows(BudgetExceededException.class,
                () -> service.validateTransactionRespectsBudget(user, category, new BigDecimal("300.00")));

        verify(expenseRepository, times(1)).sumByOwnerAndCategoryAndDateRange(any(), any(), any(), any());
    }

    // ── validateTransactionRespectsBudgetOnUpdate ─────────────────────────────

    @Test
    @DisplayName("Should pass update validation when no active budget exists for category")
    void validateTransactionRespectsBudgetOnUpdate_WhenNoBudgetExists_ShouldNotThrow() {
        User user = User.builder().id(UUID.randomUUID()).name("Anderson")
                .email("anderson@gmail.com").password("password").role(UserRole.ROLE_USER).build();
        Category category = Category.builder().id(UUID.randomUUID()).name("food")
                .description("food description").icon("food").owner(user).build();

        doReturn(Optional.empty()).when(repository).findByOwnerIdAndCategoryIdAndActiveTrue(user.getId(), category.getId());

        service.validateTransactionRespectsBudgetOnUpdate(
                user, category, new BigDecimal("100.00"), new BigDecimal("200.00"));

        verify(expenseRepository, never()).sumByOwnerAndCategoryAndDateRange(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should pass update validation when price increase keeps total within budget limit")
    void validateTransactionRespectsBudgetOnUpdate_WhenPriceIncreasedAndWithinLimit_ShouldNotThrow() {
        User user = User.builder().id(UUID.randomUUID()).name("Anderson")
                .email("anderson@gmail.com").password("password").role(UserRole.ROLE_USER).build();
        Category category = Category.builder().id(UUID.randomUUID()).name("food")
                .description("food description").icon("food").owner(user).build();

        Budget budget = Budget.builder().id(UUID.randomUUID()).owner(user).category(category)
                .budgetType(BudgetType.MONTHLY).limitAmount(new BigDecimal("1000.00")).active(true).build();

        // totalSpent=700 (includes old 100); projected = 700 - 100 + 200 = 800 <= 1000
        doReturn(Optional.of(budget)).when(repository).findByOwnerIdAndCategoryIdAndActiveTrue(user.getId(), category.getId());
        doReturn(new BigDecimal("700.00")).when(expenseRepository)
                .sumByOwnerAndCategoryAndDateRange(any(), any(), any(), any());

        service.validateTransactionRespectsBudgetOnUpdate(
                user, category, new BigDecimal("100.00"), new BigDecimal("200.00"));

        verify(expenseRepository, times(1)).sumByOwnerAndCategoryAndDateRange(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should throw BudgetExceededException when price increase causes total to exceed budget limit")
    void validateTransactionRespectsBudgetOnUpdate_WhenPriceIncreasedAndExceedsLimit_ShouldThrowBudgetExceededException() {
        User user = User.builder().id(UUID.randomUUID()).name("Anderson")
                .email("anderson@gmail.com").password("password").role(UserRole.ROLE_USER).build();
        Category category = Category.builder().id(UUID.randomUUID()).name("food")
                .description("food description").icon("food").owner(user).build();

        Budget budget = Budget.builder().id(UUID.randomUUID()).owner(user).category(category)
                .budgetType(BudgetType.MONTHLY).limitAmount(new BigDecimal("1000.00")).active(true).build();

        // totalSpent=900 (includes old 100); projected = 900 - 100 + 400 = 1200 > 1000
        doReturn(Optional.of(budget)).when(repository).findByOwnerIdAndCategoryIdAndActiveTrue(user.getId(), category.getId());
        doReturn(new BigDecimal("900.00")).when(expenseRepository)
                .sumByOwnerAndCategoryAndDateRange(any(), any(), any(), any());

        assertThrows(BudgetExceededException.class,
                () -> service.validateTransactionRespectsBudgetOnUpdate(
                        user, category, new BigDecimal("100.00"), new BigDecimal("400.00")));

        verify(expenseRepository, times(1)).sumByOwnerAndCategoryAndDateRange(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should pass update validation when price decreases since projected total can only improve")
    void validateTransactionRespectsBudgetOnUpdate_WhenPriceDecreased_ShouldNotThrow() {
        User user = User.builder().id(UUID.randomUUID()).name("Anderson")
                .email("anderson@gmail.com").password("password").role(UserRole.ROLE_USER).build();
        Category category = Category.builder().id(UUID.randomUUID()).name("food")
                .description("food description").icon("food").owner(user).build();

        Budget budget = Budget.builder().id(UUID.randomUUID()).owner(user).category(category)
                .budgetType(BudgetType.MONTHLY).limitAmount(new BigDecimal("1000.00")).active(true).build();

        // totalSpent=900 (includes old 300); projected = 900 - 300 + 100 = 700 <= 1000
        doReturn(Optional.of(budget)).when(repository).findByOwnerIdAndCategoryIdAndActiveTrue(user.getId(), category.getId());
        doReturn(new BigDecimal("900.00")).when(expenseRepository)
                .sumByOwnerAndCategoryAndDateRange(any(), any(), any(), any());

        service.validateTransactionRespectsBudgetOnUpdate(
                user, category, new BigDecimal("300.00"), new BigDecimal("100.00"));

        verify(expenseRepository, times(1)).sumByOwnerAndCategoryAndDateRange(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should return empty page when no budgets exist")
    void findAll_WhenNoBudgetsExist_ShouldReturnEmptyPage() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Anderson")
                .email("anderson@gmail.com")
                .password("password")
                .role(UserRole.ROLE_USER)
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<Budget> page = new PageImpl<>(List.of(), pageable, 0);

        doReturn(page).when(repository).findByOwner(user, pageable);

        Page<Budget> result = service.findAll(user, pageable);

        assertEquals(0, result.getTotalElements());
        assertEquals(0, result.getContent().size());
        verify(repository, times(1)).findByOwner(user, pageable);
    }
}
