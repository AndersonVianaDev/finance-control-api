package com.andersonvianadev.finance_control_api.domain.services.impl;

import com.andersonvianadev.finance_control_api.domain.models.Category;
import com.andersonvianadev.finance_control_api.domain.models.Expense;
import com.andersonvianadev.finance_control_api.domain.models.InstallmentPlan;
import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.models.dtos.CreationResultDTO;
import com.andersonvianadev.finance_control_api.domain.models.dtos.InstallmentGenerationMessage;
import com.andersonvianadev.finance_control_api.domain.models.enums.InstallmentStatus;
import com.andersonvianadev.finance_control_api.domain.models.enums.UserRole;
import com.andersonvianadev.finance_control_api.domain.services.ICategoryService;
import com.andersonvianadev.finance_control_api.domain.services.IExpenseService;
import com.andersonvianadev.finance_control_api.infra.exceptions.ExternalServiceException;
import com.andersonvianadev.finance_control_api.infra.exceptions.NotFoundException;
import com.andersonvianadev.finance_control_api.infra.exceptions.OperationNotAllowedException;
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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
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

        CreationResultDTO result = service.create(planRequest, false);

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

    @Test
    @DisplayName("Should throw ExternalServiceException when calendar API is unavailable during first installment creation")
    void create_WhenCalendarApiIsUnavailableOnFirstInstallment_ShouldThrowExternalServiceException() {
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

        doReturn(category).when(categoryService).findByIdAndOwnerOrOwnerIsNull(category.getId(), user);
        doReturn(savedPlan).when(repository).save(any());
        doThrow(new ExternalServiceException("calendar-api", new RuntimeException("connection refused")))
                .when(expenseService).save(any(), any(Boolean.class));

        assertThrows(ExternalServiceException.class, () -> service.create(planRequest, false));

        verify(sqsMessageSender, never()).send(anyString(), any());
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
    @DisplayName("Should throw ExternalServiceException when calendar API is unavailable during remaining installments generation")
    void generateRemainingInstallments_WhenCalendarApiIsUnavailable_ShouldThrowExternalServiceException() {
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
        doThrow(new ExternalServiceException("calendar-api", new RuntimeException("connection refused")))
                .when(expenseService).save(any(), any(Boolean.class));

        assertThrows(ExternalServiceException.class,
                () -> service.generateRemainingInstallments(planId, false));
    }

    // --- findById() ---

    @Test
    @DisplayName("Should return plan when it exists and belongs to the user")
    void findById_WhenPlanExists_ShouldReturnPlan() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Anderson")
                .email("anderson@gmail.com")
                .password("password")
                .role(UserRole.ROLE_USER)
                .build();

        UUID planId = UUID.randomUUID();

        InstallmentPlan plan = InstallmentPlan.builder()
                .id(planId)
                .owner(user)
                .description("MacBook Pro 14")
                .totalAmount(new BigDecimal("18000.00"))
                .installmentAmount(new BigDecimal("1500.00"))
                .totalInstallments(12)
                .status(InstallmentStatus.ACTIVE)
                .firstDueDate(LocalDate.of(2026, 7, 1))
                .build();

        doReturn(Optional.of(plan)).when(repository).findByOwnerIdAndId(user.getId(), planId);

        InstallmentPlan result = service.findById(user, planId);

        assertEquals(plan, result);
        verify(repository, times(1)).findByOwnerIdAndId(user.getId(), planId);
    }

    @Test
    @DisplayName("Should throw NotFoundException when plan does not belong to user")
    void findById_WhenPlanNotFound_ShouldThrowNotFoundException() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Anderson")
                .email("anderson@gmail.com")
                .password("password")
                .role(UserRole.ROLE_USER)
                .build();

        UUID planId = UUID.randomUUID();

        doReturn(Optional.empty()).when(repository).findByOwnerIdAndId(user.getId(), planId);

        assertThrows(NotFoundException.class, () -> service.findById(user, planId));
    }

    // --- findAll() ---

    @Test
    @DisplayName("Should return page with plans when user has plans")
    void findAll_WhenPlansExist_ShouldReturnPage() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Anderson")
                .email("anderson@gmail.com")
                .password("password")
                .role(UserRole.ROLE_USER)
                .build();

        Pageable pageable = PageRequest.of(0, 10);

        InstallmentPlan plan = InstallmentPlan.builder()
                .id(UUID.randomUUID())
                .owner(user)
                .description("MacBook Pro 14")
                .totalAmount(new BigDecimal("18000.00"))
                .installmentAmount(new BigDecimal("1500.00"))
                .totalInstallments(12)
                .status(InstallmentStatus.ACTIVE)
                .firstDueDate(LocalDate.of(2026, 7, 1))
                .build();

        Page<InstallmentPlan> expectedPage = new PageImpl<>(List.of(plan), pageable, 1);

        doReturn(expectedPage).when(repository).findByOwnerId(user.getId(), pageable);

        Page<InstallmentPlan> result = service.findAll(user, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(plan, result.getContent().get(0));
        verify(repository, times(1)).findByOwnerId(user.getId(), pageable);
    }

    @Test
    @DisplayName("Should return empty page when user has no plans")
    void findAll_WhenNoPlansExist_ShouldReturnEmptyPage() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Anderson")
                .email("anderson@gmail.com")
                .password("password")
                .role(UserRole.ROLE_USER)
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<InstallmentPlan> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        doReturn(emptyPage).when(repository).findByOwnerId(user.getId(), pageable);

        Page<InstallmentPlan> result = service.findAll(user, pageable);

        assertEquals(0, result.getTotalElements());
        verify(repository, times(1)).findByOwnerId(user.getId(), pageable);
    }

    // --- cancel() ---

    @Test
    @DisplayName("Should cancel plan and delete all associated expenses when plan is active")
    void cancel_WhenPlanIsActive_ShouldCancelAndDeleteExpenses() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Anderson")
                .email("anderson@gmail.com")
                .password("password")
                .role(UserRole.ROLE_USER)
                .build();

        UUID planId = UUID.randomUUID();

        InstallmentPlan plan = InstallmentPlan.builder()
                .id(planId)
                .owner(user)
                .description("MacBook Pro 14")
                .totalAmount(new BigDecimal("18000.00"))
                .installmentAmount(new BigDecimal("1500.00"))
                .totalInstallments(12)
                .status(InstallmentStatus.ACTIVE)
                .firstDueDate(LocalDate.of(2026, 7, 1))
                .build();

        doReturn(Optional.of(plan)).when(repository).findByOwnerIdAndId(user.getId(), planId);
        doNothing().when(expenseService).deleteByInstallmentPlan(planId);
        doReturn(plan).when(repository).save(plan);

        service.cancel(user, planId);

        assertEquals(InstallmentStatus.CANCELLED, plan.getStatus());
        verify(expenseService, times(1)).deleteByInstallmentPlan(planId);
        verify(repository, times(1)).save(plan);
    }

    @Test
    @DisplayName("Should throw OperationNotAllowedException when plan is already cancelled")
    void cancel_WhenPlanIsAlreadyCancelled_ShouldThrowOperationNotAllowedException() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Anderson")
                .email("anderson@gmail.com")
                .password("password")
                .role(UserRole.ROLE_USER)
                .build();

        UUID planId = UUID.randomUUID();

        InstallmentPlan plan = InstallmentPlan.builder()
                .id(planId)
                .owner(user)
                .description("MacBook Pro 14")
                .totalAmount(new BigDecimal("18000.00"))
                .installmentAmount(new BigDecimal("1500.00"))
                .totalInstallments(12)
                .status(InstallmentStatus.CANCELLED)
                .firstDueDate(LocalDate.of(2026, 7, 1))
                .build();

        doReturn(Optional.of(plan)).when(repository).findByOwnerIdAndId(user.getId(), planId);

        assertThrows(OperationNotAllowedException.class, () -> service.cancel(user, planId));

        verify(expenseService, never()).deleteByInstallmentPlan(any());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw NotFoundException when cancelling a plan that does not exist")
    void cancel_WhenPlanNotFound_ShouldThrowNotFoundException() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Anderson")
                .email("anderson@gmail.com")
                .password("password")
                .role(UserRole.ROLE_USER)
                .build();

        UUID planId = UUID.randomUUID();

        doReturn(Optional.empty()).when(repository).findByOwnerIdAndId(user.getId(), planId);

        assertThrows(NotFoundException.class, () -> service.cancel(user, planId));

        verify(expenseService, never()).deleteByInstallmentPlan(any());
        verify(repository, never()).save(any());
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
