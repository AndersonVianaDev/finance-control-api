package com.andersonvianadev.finance_control_api.domain.services.impl;

import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.services.IPasswordEncoderService;
import com.andersonvianadev.finance_control_api.infra.exceptions.ResourceAlreadyExistsException;
import com.andersonvianadev.finance_control_api.infra.repositories.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository repository;

    @Mock
    private IPasswordEncoderService passwordEncoderService;

    @InjectMocks
    private UserServiceImpl service;

    @Test
    @DisplayName("Should save user successfully when email does not exist")
    void save_ShouldReturnSavedUser_WhenEmailDoesNotExists() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Anderson")
                .email("anderson@gmail.com")
                .password("aaaa")
                .build();

        doReturn(false).when(repository).existsByEmail(user.getEmail());
        doReturn("hash").when(passwordEncoderService).encode(user.getPassword());
        doReturn(user).when(repository).save(user);

        User userCreated = service.save(user);

        assertEquals(user, userCreated);
        verify(repository, times(1)).save(user);
        verify(passwordEncoderService, times(1)).encode(any());
    }

    @Test
    @DisplayName("Should throw ResourceAlreadyExistsException when email already exists")
    void save_ShouldThrowResourceAlreadyExistsException_WhenEmailAlreadyExists() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Anderson")
                .email("anderson@gmail.com")
                .password("aaaa")
                .build();

        doReturn(true).when(repository).existsByEmail(user.getEmail());

        assertThrows(ResourceAlreadyExistsException.class, () -> service.save(user));

        verify(repository, times(0)).save(any());
        verify(passwordEncoderService, times(0)).encode(user.getPassword());
    }

    @Test
    @DisplayName("Should throw DataIntegrityViolationException when email already exists")
    void save_ShouldThrowDataIntegrityViolationException_WhenEmailAlreadyExists() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Anderson")
                .email("anderson@gmail.com")
                .password("aaaa")
                .build();

        doReturn(false).when(repository).existsByEmail(user.getEmail());
        doReturn("hash").when(passwordEncoderService).encode(user.getPassword());

        doThrow(new DataIntegrityViolationException("email already exists")).when(repository).save(user);

        assertThrows(ResourceAlreadyExistsException.class, () -> service.save(user));

        verify(repository, times(1)).save(any());
        verify(passwordEncoderService, times(1)).encode(any());
    }
}