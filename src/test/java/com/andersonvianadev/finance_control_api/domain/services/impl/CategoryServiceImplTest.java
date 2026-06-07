package com.andersonvianadev.finance_control_api.domain.services.impl;

import com.andersonvianadev.finance_control_api.domain.models.Category;
import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.models.enums.UserRole;
import com.andersonvianadev.finance_control_api.infra.exceptions.NotFoundException;
import com.andersonvianadev.finance_control_api.infra.exceptions.QuotaExceededException;
import com.andersonvianadev.finance_control_api.infra.exceptions.ResourceAlreadyExistsException;
import com.andersonvianadev.finance_control_api.infra.repositories.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

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
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository repository;

    @InjectMocks
    private CategoryServiceImpl service;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(service, "freePlanCategoryLimit", 10);
    }

    @Test
    @DisplayName("Should save category successfully when category is valid")
    void save_WhenCategoryIsValid_ShouldReturnSavedCategory() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Anderson")
                .email("anderson@gmail.com")
                .password("password")
                .role(UserRole.ROLE_USER)
                .build();

        Category categoryRequest = Category.builder()
                .name("food")
                .description("food description")
                .icon("food")
                .owner(user)
                .build();

        Category categorySaved = Category.builder()
                .id(UUID.randomUUID())
                .name("food")
                .description("food description")
                .icon("food")
                .owner(user)
                .build();

        doReturn(false).when(repository).existsByNameAndOwnerOrGlobal(categoryRequest.getName(), categoryRequest.getOwner());
        doReturn(0).when(repository).countByOwner(categoryRequest.getOwner());
        doReturn(categorySaved).when(repository).save(categoryRequest);

        Category categoryResult = service.save(categoryRequest);

        verify(repository, times(1)).existsByNameAndOwnerOrGlobal(categoryRequest.getName(), user);
        verify(repository, times(1)).countByOwner(any());
        verify(repository, times(1)).save(any());
        assertEquals(categorySaved, categoryResult);
    }

    @Test
    @DisplayName("Should throw ResourceAlreadyExistsException when category name already exists")
    void save_WhenCategoryNameAlreadyExists_ShouldThrowResourceAlreadyExistsException() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Anderson")
                .email("anderson@gmail.com")
                .password("password")
                .role(UserRole.ROLE_USER)
                .build();

        Category categoryRequest = Category.builder()
                .name("food")
                .description("food description")
                .icon("food")
                .owner(user)
                .build();

        doReturn(true).when(repository).existsByNameAndOwnerOrGlobal(categoryRequest.getName(), user);

        assertThrows(ResourceAlreadyExistsException.class, () -> service.save(categoryRequest));

        verify(repository, never()).countByOwner(any());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw QuotaExceededException when free plan category limit is reached")
    void save_WhenFreePlanCategoryLimitIsReached_ShouldThrowQuotaExceededException() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Anderson")
                .email("anderson@gmail.com")
                .password("password")
                .role(UserRole.ROLE_USER)
                .build();

        Category categoryRequest = Category.builder()
                .name("food")
                .description("food description")
                .icon("food")
                .owner(user)
                .build();

        doReturn(false).when(repository).existsByNameAndOwnerOrGlobal(categoryRequest.getName(), user);
        doReturn(10).when(repository).countByOwner(user);

        assertThrows(QuotaExceededException.class, () -> service.save(categoryRequest));

        verify(repository, times(1)).existsByNameAndOwnerOrGlobal(categoryRequest.getName(), user);
        verify(repository, times(1)).countByOwner(any());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceAlreadyExistsException when data integrity violation occurs")
    void save_WhenDataIntegrityViolationOccurs_ShouldThrowResourceAlreadyExistsException() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Anderson")
                .email("anderson@gmail.com")
                .password("password")
                .role(UserRole.ROLE_USER)
                .build();

        Category categoryRequest = Category.builder()
                .name("food")
                .description("food description")
                .icon("food")
                .owner(user)
                .build();

        doReturn(false).when(repository).existsByNameAndOwnerOrGlobal(categoryRequest.getName(), user);
        doReturn(0).when(repository).countByOwner(user);

        doThrow(new DataIntegrityViolationException("email already exists")).when(repository).save(categoryRequest);

        assertThrows(ResourceAlreadyExistsException.class, () -> service.save(categoryRequest));

        verify(repository, times(1)).existsByNameAndOwnerOrGlobal(categoryRequest.getName(), user);
        verify(repository, times(1)).countByOwner(any());
        verify(repository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should return category when a valid ID is provided")
    void findById_WhenCategoryExists_ShouldReturnCategory() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("anderson")
                .email("anderson@gmail.com")
                .password("Arthur@1406")
                .role(UserRole.ROLE_USER)
                .build();

        Category category = Category.builder()
                .id(UUID.randomUUID())
                .name("food")
                .description("food")
                .icon("food")
                .owner(user)
                .build();

        doReturn(Optional.of(category)).when(repository).findByIdAndOwnerOrOwnerIsNull(category.getId(), user);

        Category categoryResult = service.findByIdAndOwnerOrOwnerIsNull(category.getId(), user);

        assertEquals(category, categoryResult);
        verify(repository, times(1)).findByIdAndOwnerOrOwnerIsNull(category.getId(), user);
    }

    @Test
    @DisplayName("Should throw NotFoundException when category does not exist")
    void findById_WhenCategoryDoesNotExist_ShouldThrowNotFoundException() {
        UUID id = UUID.randomUUID();

        User user = User.builder()
                .id(UUID.randomUUID())
                .name("anderson")
                .email("anderson@gmail.com")
                .password("Arthur@1406")
                .role(UserRole.ROLE_USER)
                .build();

        doReturn(Optional.empty()).when(repository).findByIdAndOwnerOrOwnerIsNull(id, user);

        assertThrows(NotFoundException.class, () -> service.findByIdAndOwnerOrOwnerIsNull(id, user));
    }

    @Test
    @DisplayName("Should delete category successfully when category exists")
    void delete_WhenCategoryExists_ShouldDeleteCategory() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("anderson")
                .email("anderson@gmail.com")
                .password("Arthur@1406")
                .role(UserRole.ROLE_USER)
                .build();

        Category category = Category.builder()
                .id(UUID.randomUUID())
                .name("food")
                .description("food")
                .icon("food")
                .owner(user)
                .build();

        doReturn(Optional.of(category)).when(repository).findByIdAndOwner(category.getId(), user);

        service.delete(category.getId(), user);

        verify(repository, times(1)).findByIdAndOwner(category.getId(), user);
        verify(repository, times(1)).delete(category);
    }

    @Test
    @DisplayName("Should throw NotFoundException when category does not exist")
    void delete_WhenCategoryDoesNotExist_ShouldThrowNotFoundException() {
        UUID id = UUID.randomUUID();

        User user = User.builder()
                .id(UUID.randomUUID())
                .name("anderson")
                .email("anderson@gmail.com")
                .password("Arthur@1406")
                .role(UserRole.ROLE_USER)
                .build();

        doReturn(Optional.empty()).when(repository).findByIdAndOwner(id, user);

        assertThrows(NotFoundException.class, () -> service.delete(id, user));

        verify(repository, times(1)).findByIdAndOwner(id, user);
        verify(repository, never()).delete(any());
    }

    @Test
    @DisplayName("Should update category successfully when category exists")
    void update_WhenCategoryExists_ShouldUpdateCategory() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("anderson")
                .email("anderson@gmail.com")
                .password("Arthur@1406")
                .role(UserRole.ROLE_USER)
                .build();

        Category categoryActual = Category.builder()
                .id(UUID.randomUUID())
                .name("food")
                .description("food description")
                .icon("food")
                .owner(user)
                .build();

        Category categoryUpdate = Category.builder()
                .id(categoryActual.getId())
                .name("fitness")
                .description("fitness description")
                .icon("fitness")
                .owner(user)
                .build();

        doReturn(Optional.of(categoryActual)).when(repository).findByIdAndOwner(categoryActual.getId(), user);
        doAnswer(invocation -> invocation.getArgument(0)).when(repository).save(any());

        Category categoryResult = service.update(categoryUpdate);

        assertEquals("fitness", categoryResult.getName());
        assertEquals("fitness description", categoryResult.getDescription());
        assertEquals("fitness", categoryResult.getIcon());
        verify(repository, times(1)).findByIdAndOwner(categoryActual.getId(), user);
        verify(repository, times(1)).save(categoryActual);
    }

    @Test
    @DisplayName("Should throw NotFoundException when category does not exist")
    void update_WhenCategoryDoesNotExist_ShouldThrowNotFoundException() {
        UUID id = UUID.randomUUID();

        User user = User.builder()
                .id(UUID.randomUUID())
                .name("anderson")
                .email("anderson@gmail.com")
                .password("Arthur@1406")
                .role(UserRole.ROLE_USER)
                .build();

        Category categoryUpdate = Category.builder()
                .id(id)
                .name("fitness")
                .description("fitness description")
                .icon("fitness")
                .owner(user)
                .build();

        doReturn(Optional.empty()).when(repository).findByIdAndOwner(id, user);

        assertThrows(NotFoundException.class, () -> service.update(categoryUpdate));

        verify(repository, times(1)).findByIdAndOwner(id, user);
        verify(repository, never()).save(any());
    }
}