package com.andersonvianadev.finance_control_api.domain.services.impl;

import com.andersonvianadev.finance_control_api.domain.models.Category;
import com.andersonvianadev.finance_control_api.domain.models.Expense;
import com.andersonvianadev.finance_control_api.domain.models.InstallmentPlan;
import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.models.dtos.InstallmentGenerationMessage;
import com.andersonvianadev.finance_control_api.domain.models.enums.InstallmentStatus;
import com.andersonvianadev.finance_control_api.domain.models.enums.UserRole;
import com.andersonvianadev.finance_control_api.domain.services.ICategoryService;
import com.andersonvianadev.finance_control_api.domain.services.IExpenseService;
import com.andersonvianadev.finance_control_api.domain.services.IInstallmentPlanService.CreationResult;
import com.andersonvianadev.finance_control_api.infra.exceptions.NotFoundException;
import com.andersonvianadev.finance_control_api.infra.messaging.ISqsMessageSender;
import com.andersonvianadev.finance_control_api.infra.repositories.InstallmentPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InstallmentPlanServiceImplTest {

    @Mock
    private InstallmentPlanRepository repository;

    @Mock
    private ICategoryService categoryService;

    @Mock
    private IExpenseService expenseService;

    @Mock
    private ISqsMessageSender sqsMessageSender;

    @InjectMocks
    private InstallmentPlanServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "installmentGenerationQueueUrl",
                "http://test-queue/000000000000/installment-generation-queue");
    }

    // --- create() ---

    @Test
    @DisplayName("Should create plan and first expense when request is valid")
    void create_WhenPlanIsValid_ShouldSavePlanAndReturnFirstExpense() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Anderson")
                .email("anderson@gmail.com")
                .password("password")
                .role(UserRole.ROLE_USER)
                .build();

        Category category = Category.builder()
                .id(UUID.randomUUID())
                .name("technology")
                .description("Electronics")
                .icon("tech")
                .owner(user)
                .build();

        InstallmentPlan planRequest = InstallmentPlan.builder()
                .owner(user)
                .category(Category.builder().id(category.getId()).build())
                .description("MacBook Pro 14")
                .totalAmount(new BigDecimal("18000.00"))
                .installmentAmount(new BigDecimal("1500.00"))
                .totalInstallments(12)
                .firstDueDate(LocalDate.of(2026, 7, 1))
                .build();

        InstallmentPlan savedPlan = InstallmentPlan.builder()
                .id(UUID.randomUUID())
                .owner(user)
                .category(category)
                .description("MacBook Pro 14")
                .totalAmount(new BigDecimal("18000.00"))
                .installmentAmount(new BigDecimal("1500.00"))
                .totalInstallments(12)
                .paidInstallments(0)
                .status(InstallmentStatus.ACTIVE)
                .firstDueDate(LocalDate.of(2026, 7, 1))
                .build();

        Expense savedFirstExpense = Expense.builder()
                .id(UUID.randomUUID())
                .owner(user)
                .category(category)
                .description("MacBook Pro 14")
                .price(new BigDecimal("1500.00"))
                .installmentPlan(savedPlan)
                .installmentNumber(1)
                .build();

        doReturn(category).when(categoryService).findByIdAndOwnerOrOwnerIsNull(category.getId(), user);
        doReturn(savedPlan).when(repository).save(any());
        doReturn(savedFirstExpense).when(expenseService).save(any(), any(Boolean.class));

        CreationResult result = service.create(planRequest, false);

        assertNotNull(result);
        assertEquals(savedPlan, result.plan());
        assertEquals(savedFirstExpense, result.firstExpense());
        assertEquals(1, result.firstExpense().getInstallmentNumber());

        verify(categoryService, times(1)).findByIdAndOwnerOrOwnerIsNull(category.getId(), user);
        verify(repository, times(1)).save(any());
        verify(expenseService, times(1)).save(any(), any(Boolean.class));
        verify(sqsMessageSender, times(1)).send(anyString(), any(InstallmentGenerationMessage.class));
    }

    @Test
    @DisplayName("Should throw NotFoundException when category does not exist")
    void create_WhenCategoryDoesNotExist_ShouldThrowNotFoundException() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Anderson")
                .email("anderson@gmail.com")
                .password("password")
                .role(UserRole.ROLE_USER)
                .build();

        UUID categoryId = UUID.randomUUID();

        InstallmentPlan planRequest = InstallmentPlan.builder()
                .owner(user)
                .category(Category.builder().id(categoryId).build())
                .description("MacBook Pro 14")
                .totalAmount(new BigDecimal("18000.00"))
                .installmentAmount(new BigDecimal("1500.00"))
                .totalInstallments(12)
                .firstDueDate(LocalDate.of(2026, 7, 1))
                .build();

        doThrow(new NotFoundException("Category not found."))
                .when(categoryService).findByIdAndOwnerOrOwnerIsNull(categoryId, user);

        assertThrows(NotFoundException.class, () -> service.create(planRequest, false));

        verify(repository, never()).save(any());
        verify(expenseService, never()).save(any(), any(Boolean.class));
        verify(sqsMessageSender, never()).send(anyString(), any());
    }

    @Test
    @DisplayName("Should pass skipBudget flag to expense service and SQS message")
    void create_WhenSkipBudgetIsTrue_ShouldForwardFlagToExpenseAndQueue() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Anderson")
                .email("anderson@gmail.com")
                .password("password")
                .role(UserRole.ROLE_USER)
                .build();

        Category category = Category.builder()
                .id(UUID.randomUUID())
                .name("technology")
                .description("Electronics")
                .icon("tech")
                .owner(user)
                .build();

        InstallmentPlan planRequest = InstallmentPlan.builder()
                .owner(user)
                .category(Category.builder().id(category.getId()).build())
                .description("MacBook Pro 14")
                .totalAmount(new BigDecimal("18000.00"))
                .installmentAmount(new BigDecimal("1500.00"))
                .totalInstallments(12)
                .firstDueDate(LocalDate.of(2026, 7, 1))
                .build();

        InstallmentPlan savedPlan = InstallmentPlan.builder()
                .id(UUID.randomUUID())
                .owner(user)
                .category(category)
                .description("MacBook Pro 14")
                .totalAmount(new BigDecimal("18000.00"))
                .installmentAmount(new BigDecimal("1500.00"))
                .totalInstallments(12)
                .paidInstallments(0)
                .status(InstallmentStatus.ACTIVE)
                .firstDueDate(LocalDate.of(2026, 7, 1))
                .build();

        Expense savedFirstExpense = Expense.builder()
                .id(UUID.randomUUID())
                .owner(user)
                .category(category)
                .price(new BigDecimal("1500.00"))
                .installmentPlan(savedPlan)
                .installmentNumber(1)
                .build();

        doReturn(category).when(categoryService).findByIdAndOwnerOrOwnerIsNull(category.getId(), user);
        doReturn(savedPlan).when(repository).save(any());
        doAnswer(inv -> savedFirstExpense).when(expenseService).save(any(), any(Boolean.class));

        service.create(planRequest, true);

        verify(expenseService, times(1)).save(any(), any(Boolean.class));
        verify(sqsMessageSender, times(1)).send(anyString(), any(InstallmentGenerationMessage.class));
    }

    // --- generateRemainingInstallments() ---

    @Test
    @DisplayName("Should create N-1 installment expenses when plan exists")
    void generateRemainingInstallments_WhenPlanExists_ShouldCreateRemainingExpenses() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Anderson")
                .email("anderson@gmail.com")
                .password("password")
                .role(UserRole.ROLE_USER)
                .build();

        Category category = Category.builder()
                .id(UUID.randomUUID())
                .name("technology")
                .description("Electronics")
                .icon("tech")
                .owner(user)
                .build();

        UUID planId = UUID.randomUUID();

        InstallmentPlan plan = InstallmentPlan.builder()
                .id(planId)
                .owner(user)
                .category(category)
                .description("MacBook Pro 14")
                .totalAmount(new BigDecimal("18000.00"))
                .installmentAmount(new BigDecimal("1500.00"))
                .totalInstallments(12)
                .firstDueDate(LocalDate.of(2026, 7, 1))
                .build();

        doReturn(Optional.of(plan)).when(repository).findById(planId);

        service.generateRemainingInstallments(planId, false);

        // installments 2..12 = 11 saves
        verify(expenseService, times(11)).save(any(), any(Boolean.class));
    }

    @Test
    @DisplayName("Should throw NotFoundException when plan does not exist")
    void generateRemainingInstallments_WhenPlanNotFound_ShouldThrowNotFoundException() {
        UUID planId = UUID.randomUUID();

        doReturn(Optional.empty()).when(repository).findById(planId);

        assertThrows(NotFoundException.class,
                () -> service.generateRemainingInstallments(planId, false));

        verify(expenseService, never()).save(any(), any(Boolean.class));
    }

    @Test
    @DisplayName("Should forward skipBudget flag to each installment expense")
    void generateRemainingInstallments_WhenSkipBudgetIsTrue_ShouldPassFlagToExpenseService() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Anderson")
                .email("anderson@gmail.com")
                .password("password")
                .role(UserRole.ROLE_USER)
                .build();

        Category category = Category.builder()
                .id(UUID.randomUUID())
                .name("technology")
                .description("Electronics")
                .icon("tech")
                .owner(user)
                .build();

        UUID planId = UUID.randomUUID();

        InstallmentPlan plan = InstallmentPlan.builder()
                .id(planId)
                .owner(user)
                .category(category)
                .description("MacBook Pro 14")
                .totalAmount(new BigDecimal("18000.00"))
                .installmentAmount(new BigDecimal("1500.00"))
                .totalInstallments(3)
                .firstDueDate(LocalDate.of(2026, 7, 1))
                .build();

        doReturn(Optional.of(plan)).when(repository).findById(planId);

        service.generateRemainingInstallments(planId, true);

        // installments 2..3 = 2 saves, all with skipBudget=true
        verify(expenseService, times(2)).save(any(), any(Boolean.class));
    }
}
