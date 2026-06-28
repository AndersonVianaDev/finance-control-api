package com.andersonvianadev.finance_control_api.domain.services.impl;

import com.andersonvianadev.finance_control_api.domain.models.Category;
import com.andersonvianadev.finance_control_api.domain.models.Income;
import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.models.dtos.CalendarDTO;
import com.andersonvianadev.finance_control_api.domain.models.enums.UserRole;
import com.andersonvianadev.finance_control_api.domain.services.ICalendarService;
import com.andersonvianadev.finance_control_api.domain.services.ICategoryService;
import com.andersonvianadev.finance_control_api.infra.exceptions.NotFoundException;
import com.andersonvianadev.finance_control_api.infra.exceptions.ResourceAlreadyExistsException;
import com.andersonvianadev.finance_control_api.infra.repositories.IncomeRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncomeServiceImplTest {

    @Mock
    private IncomeRepository repository;

    @Mock
    private ICategoryService categoryService;

    @Mock
    private ICalendarService calendarService;

    @InjectMocks
    private IncomeServiceImpl service;

    // ── save ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should save income without calendar adjustment when isRecurring is false")
    void save_WhenValidManualEntry_ShouldSaveAndReturnIncome() {
        User user = buildUser();
        Category category = buildCategory(user);
        LocalDateTime date = LocalDateTime.of(2026, 6, 5, 0, 0);

        Income income = buildIncome(user, category.getId(), date);
        Income saved = Income.builder().id(UUID.randomUUID()).owner(user).category(category)
                .transactionDate(date).price(income.getPrice()).description(income.getDescription()).build();

        doReturn(category).when(categoryService).findByIdAndOwnerOrOwnerIsNull(category.getId(), user);
        doReturn(false).when(repository).existsDuplicate(user.getId(), category.getId(), income.getPrice(), date, income.getDescription());
        doReturn(saved).when(repository).save(income);

        Income result = service.save(income, false);

        verify(calendarService, never()).getDate(any());
        verify(repository, times(1)).save(income);
        assertEquals(saved, result);
    }

    @Test
    @DisplayName("Should not change transaction date when recurring income falls on a working day")
    void save_WhenRecurringAndDateIsWorkingDay_ShouldNotAdjustDate() {
        User user = buildUser();
        Category category = buildCategory(user);
        LocalDateTime date = LocalDateTime.of(2026, 6, 5, 0, 0);

        Income income = buildIncome(user, category.getId(), date);
        CalendarDTO calendarDTO = new CalendarDTO(date.toLocalDate(), true, null, null);

        doReturn(category).when(categoryService).findByIdAndOwnerOrOwnerIsNull(category.getId(), user);
        doReturn(calendarDTO).when(calendarService).getDate(date.toLocalDate());
        doReturn(false).when(repository).existsDuplicate(user.getId(), category.getId(), income.getPrice(), date, income.getDescription());
        doReturn(Income.builder().id(UUID.randomUUID()).build()).when(repository).save(income);

        service.save(income, true);

        assertEquals(date, income.getTransactionDate());
        verify(calendarService, times(1)).getDate(date.toLocalDate());
        verify(repository, times(1)).save(income);
    }

    @Test
    @DisplayName("Should shift transaction date to next working day when recurring income falls on a non-working day")
    void save_WhenRecurringAndDateIsNonWorkingDay_ShouldShiftToNextWorkingDay() {
        User user = buildUser();
        Category category = buildCategory(user);
        LocalDateTime nonWorkingDate = LocalDateTime.of(2026, 6, 7, 0, 0);
        LocalDate nextWorkingDay = LocalDate.of(2026, 6, 9);

        Income income = buildIncome(user, category.getId(), nonWorkingDate);
        CalendarDTO calendarDTO = new CalendarDTO(nonWorkingDate.toLocalDate(), false, "Domingo", nextWorkingDay);

        doReturn(category).when(categoryService).findByIdAndOwnerOrOwnerIsNull(category.getId(), user);
        doReturn(calendarDTO).when(calendarService).getDate(nonWorkingDate.toLocalDate());
        doReturn(false).when(repository).existsDuplicate(user.getId(), category.getId(), income.getPrice(), nextWorkingDay.atStartOfDay(), income.getDescription());
        doReturn(Income.builder().id(UUID.randomUUID()).build()).when(repository).save(income);

        service.save(income, true);

        assertEquals(nextWorkingDay.atStartOfDay(), income.getTransactionDate());
        verify(calendarService, times(1)).getDate(nonWorkingDate.toLocalDate());
        verify(repository, times(1)).save(income);
    }

    @Test
    @DisplayName("Should throw NotFoundException when category does not exist")
    void save_WhenCategoryNotFound_ShouldThrowNotFoundException() {
        User user = buildUser();
        UUID categoryId = UUID.randomUUID();

        Income income = buildIncome(user, categoryId, LocalDateTime.of(2026, 6, 5, 0, 0));

        doThrow(new NotFoundException("Category not found."))
                .when(categoryService).findByIdAndOwnerOrOwnerIsNull(categoryId, user);

        assertThrows(NotFoundException.class, () -> service.save(income, false));

        verify(calendarService, never()).getDate(any());
        verify(repository, never()).existsDuplicate(any(), any(), any(), any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceAlreadyExistsException when income is duplicate")
    void save_WhenDuplicateExists_ShouldThrowResourceAlreadyExistsException() {
        User user = buildUser();
        Category category = buildCategory(user);
        LocalDateTime date = LocalDateTime.of(2026, 6, 5, 0, 0);

        Income income = buildIncome(user, category.getId(), date);

        doReturn(category).when(categoryService).findByIdAndOwnerOrOwnerIsNull(category.getId(), user);
        doReturn(true).when(repository).existsDuplicate(user.getId(), category.getId(), income.getPrice(), date, income.getDescription());

        assertThrows(ResourceAlreadyExistsException.class, () -> service.save(income, false));

        verify(calendarService, never()).getDate(any());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceAlreadyExistsException when database constraint is violated")
    void save_WhenDataIntegrityViolationOccurs_ShouldThrowResourceAlreadyExistsException() {
        User user = buildUser();
        Category category = buildCategory(user);
        LocalDateTime date = LocalDateTime.of(2026, 6, 5, 0, 0);

        Income income = buildIncome(user, category.getId(), date);

        doReturn(category).when(categoryService).findByIdAndOwnerOrOwnerIsNull(category.getId(), user);
        doReturn(false).when(repository).existsDuplicate(user.getId(), category.getId(), income.getPrice(), date, income.getDescription());
        doThrow(new DataIntegrityViolationException("duplicate")).when(repository).save(income);

        assertThrows(ResourceAlreadyExistsException.class, () -> service.save(income, false));

        verify(repository, times(1)).save(income);
    }

    // ── findById ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return income when it exists and belongs to user")
    void findById_WhenIncomeExists_ShouldReturnIncome() {
        User owner = buildUser();
        Category category = buildCategory(owner);
        Income income = buildIncome(owner, category.getId(), LocalDateTime.now());

        doReturn(Optional.of(income)).when(repository).findByOwnerIdAndId(owner.getId(), income.getId());

        Income response = service.findById(owner, income.getId());

        assertEquals(income, response);
    }

    @Test
    @DisplayName("Should throw NotFoundException when income does not exist")
    void findById_WhenIncomeDoesNotExist_ShouldThrowNotFoundException() {
        User owner = buildUser();
        UUID incomeId = UUID.randomUUID();

        doReturn(Optional.empty()).when(repository).findByOwnerIdAndId(owner.getId(), incomeId);

        assertThrows(NotFoundException.class, () -> service.findById(owner, incomeId));
    }

    // ── findAll ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return paginated incomes for user")
    void findAll_WhenIncomesExist_ShouldReturnPage() {
        User owner = buildUser();
        Category category = buildCategory(owner);
        Pageable pageable = PageRequest.of(0, 10);
        Income income = buildIncome(owner, category.getId(), LocalDateTime.now());
        Page<Income> page = new PageImpl<>(List.of(income), pageable, 1);

        doReturn(page).when(repository).findByOwnerId(owner.getId(), pageable);

        Page<Income> result = service.findAll(owner, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(income, result.getContent().getFirst());
        verify(repository, times(1)).findByOwnerId(owner.getId(), pageable);
    }

    @Test
    @DisplayName("Should return empty page when user has no incomes")
    void findAll_WhenNoIncomesExist_ShouldReturnEmptyPage() {
        User owner = buildUser();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Income> page = new PageImpl<>(List.of(), pageable, 0);

        doReturn(page).when(repository).findByOwnerId(owner.getId(), pageable);

        Page<Income> result = service.findAll(owner, pageable);

        assertEquals(0, result.getTotalElements());
        verify(repository, times(1)).findByOwnerId(owner.getId(), pageable);
    }

    // ── delete ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should delete income successfully when it exists and belongs to user")
    void delete_WhenIncomeExists_ShouldDelete() {
        User user = buildUser();
        Category category = buildCategory(user);
        UUID incomeId = UUID.randomUUID();
        Income income = buildIncome(user, category.getId(), LocalDateTime.of(2026, 6, 5, 0, 0));
        income.setId(incomeId);

        doReturn(Optional.of(income)).when(repository).findByOwnerIdAndId(user.getId(), incomeId);
        doNothing().when(repository).delete(income);

        service.delete(user, incomeId);

        verify(repository, times(1)).delete(income);
    }

    @Test
    @DisplayName("Should throw NotFoundException when income to delete does not exist")
    void delete_WhenIncomeDoesNotExist_ShouldThrowNotFoundException() {
        User user = buildUser();
        UUID incomeId = UUID.randomUUID();

        doReturn(Optional.empty()).when(repository).findByOwnerIdAndId(user.getId(), incomeId);

        assertThrows(NotFoundException.class, () -> service.delete(user, incomeId));

        verify(repository, never()).delete(any());
    }

    // ── update ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should update price and description and return updated income")
    void update_WhenValidFieldsChanged_ShouldSaveAndReturnIncome() {
        User user = buildUser();
        Category category = buildCategory(user);
        UUID incomeId = UUID.randomUUID();
        LocalDateTime date = LocalDateTime.of(2026, 6, 5, 0, 0);

        Income existing = Income.builder()
                .id(incomeId).owner(user).category(category)
                .price(new BigDecimal("5000.00")).description("Monthly salary")
                .transactionDate(date).build();

        Income input = Income.builder()
                .id(incomeId).owner(user)
                .price(new BigDecimal("5500.00"))
                .description("Updated monthly salary")
                .category(Category.builder().id(null).build())
                .build();

        doReturn(Optional.of(existing)).when(repository).findByOwnerIdAndId(user.getId(), incomeId);
        doReturn(false).when(repository).existsDuplicateExcluding(
                user.getId(), category.getId(), new BigDecimal("5500.00"), date, "Updated monthly salary", incomeId);
        doReturn(existing).when(repository).save(existing);

        Income result = service.update(input);

        verify(repository, times(1)).save(existing);
        assertEquals(existing, result);
    }

    @Test
    @DisplayName("Should resolve and apply new category when category changes")
    void update_WhenCategoryChanged_ShouldResolveNewCategoryAndUpdate() {
        User user = buildUser();
        Category oldCategory = buildCategory(user);
        Category newCategory = Category.builder().id(UUID.randomUUID()).name("freelance").owner(user).build();
        UUID incomeId = UUID.randomUUID();
        LocalDateTime date = LocalDateTime.of(2026, 6, 5, 0, 0);

        Income existing = Income.builder()
                .id(incomeId).owner(user).category(oldCategory)
                .price(new BigDecimal("5000.00")).description("Monthly salary")
                .transactionDate(date).build();

        Income input = Income.builder()
                .id(incomeId).owner(user)
                .category(Category.builder().id(newCategory.getId()).build())
                .build();

        doReturn(Optional.of(existing)).when(repository).findByOwnerIdAndId(user.getId(), incomeId);
        doReturn(newCategory).when(categoryService).findByIdAndOwnerOrOwnerIsNull(newCategory.getId(), user);
        doReturn(false).when(repository).existsDuplicateExcluding(
                user.getId(), newCategory.getId(), new BigDecimal("5000.00"), date, "Monthly salary", incomeId);
        doReturn(existing).when(repository).save(existing);

        service.update(input);

        assertEquals(newCategory, existing.getCategory());
        verify(categoryService, times(1)).findByIdAndOwnerOrOwnerIsNull(newCategory.getId(), user);
        verify(repository, times(1)).save(existing);
    }

    @Test
    @DisplayName("Should throw NotFoundException when income to update does not exist")
    void update_WhenIncomeNotFound_ShouldThrowNotFoundException() {
        User user = buildUser();
        UUID incomeId = UUID.randomUUID();

        Income input = Income.builder()
                .id(incomeId).owner(user)
                .category(Category.builder().id(null).build())
                .build();

        doReturn(Optional.empty()).when(repository).findByOwnerIdAndId(user.getId(), incomeId);

        assertThrows(NotFoundException.class, () -> service.update(input));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw NotFoundException when new category does not exist")
    void update_WhenNewCategoryNotFound_ShouldThrowNotFoundException() {
        User user = buildUser();
        Category category = buildCategory(user);
        UUID incomeId = UUID.randomUUID();
        UUID newCategoryId = UUID.randomUUID();

        Income existing = Income.builder()
                .id(incomeId).owner(user).category(category)
                .price(new BigDecimal("5000.00")).description("Monthly salary")
                .transactionDate(LocalDateTime.of(2026, 6, 5, 0, 0)).build();

        Income input = Income.builder()
                .id(incomeId).owner(user)
                .category(Category.builder().id(newCategoryId).build())
                .build();

        doReturn(Optional.of(existing)).when(repository).findByOwnerIdAndId(user.getId(), incomeId);
        doThrow(new NotFoundException("Category not found."))
                .when(categoryService).findByIdAndOwnerOrOwnerIsNull(newCategoryId, user);

        assertThrows(NotFoundException.class, () -> service.update(input));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceAlreadyExistsException when updated values conflict with another income")
    void update_WhenResultIsDuplicate_ShouldThrowResourceAlreadyExistsException() {
        User user = buildUser();
        Category category = buildCategory(user);
        UUID incomeId = UUID.randomUUID();
        LocalDateTime date = LocalDateTime.of(2026, 6, 5, 0, 0);

        Income existing = Income.builder()
                .id(incomeId).owner(user).category(category)
                .price(new BigDecimal("5000.00")).description("Monthly salary")
                .transactionDate(date).build();

        Income input = Income.builder()
                .id(incomeId).owner(user)
                .price(new BigDecimal("4000.00"))
                .category(Category.builder().id(null).build())
                .build();

        doReturn(Optional.of(existing)).when(repository).findByOwnerIdAndId(user.getId(), incomeId);
        doReturn(true).when(repository).existsDuplicateExcluding(
                user.getId(), category.getId(), new BigDecimal("4000.00"), date, "Monthly salary", incomeId);

        assertThrows(ResourceAlreadyExistsException.class, () -> service.update(input));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceAlreadyExistsException when database constraint is violated on update")
    void update_WhenDataIntegrityViolationOccurs_ShouldThrowResourceAlreadyExistsException() {
        User user = buildUser();
        Category category = buildCategory(user);
        UUID incomeId = UUID.randomUUID();
        LocalDateTime date = LocalDateTime.of(2026, 6, 5, 0, 0);

        Income existing = Income.builder()
                .id(incomeId).owner(user).category(category)
                .price(new BigDecimal("5000.00")).description("Monthly salary")
                .transactionDate(date).build();

        Income input = Income.builder()
                .id(incomeId).owner(user)
                .description("New description")
                .category(Category.builder().id(null).build())
                .build();

        doReturn(Optional.of(existing)).when(repository).findByOwnerIdAndId(user.getId(), incomeId);
        doReturn(false).when(repository).existsDuplicateExcluding(
                user.getId(), category.getId(), new BigDecimal("5000.00"), date, "New description", incomeId);
        doThrow(new DataIntegrityViolationException("constraint")).when(repository).save(any());

        assertThrows(ResourceAlreadyExistsException.class, () -> service.update(input));
    }

    @Test
    @DisplayName("Should update only description when only description changes")
    void update_WhenOnlyDescriptionChanges_ShouldUpdateOnlyDescription() {
        User user = buildUser();
        Category category = buildCategory(user);
        UUID incomeId = UUID.randomUUID();
        LocalDateTime date = LocalDateTime.of(2026, 6, 5, 0, 0);

        Income existing = Income.builder()
                .id(incomeId).owner(user).category(category)
                .price(new BigDecimal("5000.00")).description("Monthly salary")
                .transactionDate(date).build();

        Income input = Income.builder()
                .id(incomeId).owner(user)
                .description("Annual salary bonus")
                .category(Category.builder().id(null).build())
                .build();

        doReturn(Optional.of(existing)).when(repository).findByOwnerIdAndId(user.getId(), incomeId);
        doReturn(false).when(repository).existsDuplicateExcluding(
                user.getId(), category.getId(), new BigDecimal("5000.00"), date, "Annual salary bonus", incomeId);
        doReturn(existing).when(repository).save(existing);

        service.update(input);

        verify(categoryService, never()).findByIdAndOwnerOrOwnerIsNull(any(), any());
        verify(repository, times(1)).save(existing);
        assertEquals("Annual salary bonus", existing.getDescription());
        assertEquals(new BigDecimal("5000.00"), existing.getPrice());
    }

    // ── findBetweenTransactionDate ────────────────────────────────────────────

    @Test
    @DisplayName("Should return page with incomes when incomes exist within the given date range")
    void findBetweenTransactionDate_WhenIncomesExistInRange_ShouldReturnPage() {
        User user = buildUser();
        Category category = buildCategory(user);
        Pageable pageable = PageRequest.of(0, 10);

        LocalDateTime start = LocalDateTime.of(2026, 6, 1, 0, 0);
        LocalDateTime finish = LocalDateTime.of(2026, 6, 30, 23, 59, 59, 999_999_999);

        Income income = Income.builder()
                .id(UUID.randomUUID()).owner(user).category(category)
                .transactionDate(LocalDateTime.of(2026, 6, 5, 0, 0))
                .price(new BigDecimal("5000.00")).description("Monthly salary")
                .build();

        Page<Income> expectedPage = new PageImpl<>(List.of(income), pageable, 1);

        doReturn(expectedPage).when(repository)
                .findByOwnerIdAndTransactionDateBetween(user.getId(), start, finish, pageable);

        Page<Income> result = service.findBetweenTransactionDate(user, start, finish, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(income, result.getContent().getFirst());
        verify(repository, times(1))
                .findByOwnerIdAndTransactionDateBetween(user.getId(), start, finish, pageable);
    }

    @Test
    @DisplayName("Should return empty page when no incomes exist within the given date range")
    void findBetweenTransactionDate_WhenNoIncomesInRange_ShouldReturnEmptyPage() {
        User user = buildUser();
        Pageable pageable = PageRequest.of(0, 10);

        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime finish = LocalDateTime.of(2026, 1, 31, 23, 59, 59, 999_999_999);

        Page<Income> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        doReturn(emptyPage).when(repository)
                .findByOwnerIdAndTransactionDateBetween(user.getId(), start, finish, pageable);

        Page<Income> result = service.findBetweenTransactionDate(user, start, finish, pageable);

        assertEquals(0, result.getTotalElements());
        verify(repository, times(1))
                .findByOwnerIdAndTransactionDateBetween(user.getId(), start, finish, pageable);
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

    private Income buildIncome(User owner, UUID categoryId, LocalDateTime date) {
        return Income.builder()
                .owner(owner)
                .category(Category.builder().id(categoryId).build())
                .transactionDate(date)
                .price(new BigDecimal("5000.00"))
                .description("Monthly salary")
                .build();
    }
}
