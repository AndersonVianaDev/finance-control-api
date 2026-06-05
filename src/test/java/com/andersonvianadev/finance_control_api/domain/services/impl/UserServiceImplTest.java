package com.andersonvianadev.finance_control_api.domain.services.impl;

import com.andersonvianadev.finance_control_api.controllers.dtos.responses.UserResponseDTO;
import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.services.IPasswordEncoderService;
import com.andersonvianadev.finance_control_api.infra.exceptions.NotFoundException;
import com.andersonvianadev.finance_control_api.infra.exceptions.ResourceAlreadyExistsException;
import com.andersonvianadev.finance_control_api.infra.repositories.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
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

        verify(repository, never()).save(any());
        verify(passwordEncoderService, never()).encode(user.getPassword());
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

    @Test
    @DisplayName("Should return user when a valid ID is provided")
    void findById_WhenUserExists_ShouldReturnUser(){
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Anderson")
                .email("anderson@gmail.com")
                .password("Aa@12092002")
                .build();

        doReturn(Optional.of(user)).when(repository).findById(user.getId());

        User userResponse = service.findById(user.getId());

        assertEquals(user, userResponse);
        verify(repository, times(1)).findById(any());
    }

    @Test
    @DisplayName("Should throw NotFoundException when user does not exist")
    void findById_WhenUserDoesNotExist_ShouldThrowNotFoundException() {
        UUID id = UUID.randomUUID();

        doReturn(Optional.empty()).when(repository).findById(id);

        assertThrows(NotFoundException.class, () -> service.findById(id));
    }

    @Test
    @DisplayName("Should delete user successfully when user exists")
    void deleteById_WhenUserExists_ShouldDeleteUser() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Anderson")
                .email("anderson@gmail.com")
                .password("Anderson@12")
                .build();

        doReturn(Optional.of(user)).when(repository).findById(user.getId());

        service.deleteById(user.getId());

        verify(repository, times(1)).findById(any());
        verify(repository, times(1)).delete(any());
    }

    @Test
    @DisplayName("Should throw NotFoundException when user does not exist")
    void deleteById_WhenUserDoesNotExist_ShouldThrowNotFoundException() {
        UUID id = UUID.randomUUID();

        doReturn(Optional.empty()).when(repository).findById(id);

        assertThrows(NotFoundException.class, () -> service.deleteById(id));

        verify(repository, times(1)).findById(any());
        verify(repository, never()).delete(any());
    }

    @Test
    @DisplayName("Should update user successfully when user exists")
    void update_WhenUserExists_ShouldUpdateUser() {
        UUID id = UUID.randomUUID();
        User userSaved = User.builder()
                .id(id)
                .name("Anderson")
                .email("anderson@gmail.com")
                .password("Arthur@1406")
                .build();

        User user = User.builder()
                .id(id)
                .name("Anderson12")
                .email("anderson12@gmail.com")
                .build();

        User userUpdated = User.builder()
                .id(id)
                .name("Anderson12")
                .email("anderson12@gmail.com")
                .password("Arthur@1406")
                .build();

        doReturn(Optional.of(userSaved)).when(repository).findById(id);
        doReturn(userUpdated).when(repository).save(any());

        User userResponse = service.update(user);

        assertEquals(user.getId(), userResponse.getId());
        assertEquals(user.getName(), userResponse.getName());
        assertEquals(user.getEmail(), userResponse.getEmail());
    }

    @Test
    @DisplayName("Should throw NotFoundException when user does not exist")
    void update_WhenUserDoesNotExist_ShouldThrowNotFoundException() {
        UUID id = UUID.randomUUID();

        User user = User.builder()
                        .id(UUID.randomUUID())
                        .name("Anderson12")
                        .email("Anderson@gmail.com")
                        .build();

        doReturn(Optional.empty()).when(repository).findById(user.getId());

        assertThrows(NotFoundException.class, () -> service.update(user));
    }

    @Test
    @DisplayName("Should throw ResourceAlreadyExistsException when email already exists")
    void update_ShouldThrowResourceAlreadyExistsException_WhenEmailAlreadyExists() {
        UUID id = UUID.randomUUID();
        User userSaved = User.builder()
                .id(id)
                .name("Anderson")
                .email("anderson@gmail.com")
                .password("Arthur@1406")
                .build();

        User user = User.builder()
                .id(id)
                .name("Anderson12")
                .email("anderson12@gmail.com")
                .build();

        doReturn(Optional.of(userSaved)).when(repository).findById(id);
        doReturn(true).when(repository).existsByEmail(user.getEmail());

        assertThrows(ResourceAlreadyExistsException.class, () -> service.update(user));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw DataIntegrityViolationException when email already exists")
    void update_ShouldThrowDataIntegrityViolationException_WhenEmailAlreadyExists() {
        UUID id = UUID.randomUUID();
        User userSaved = User.builder()
                .id(id)
                .name("Anderson")
                .email("anderson@gmail.com")
                .password("Arthur@1406")
                .build();

        User user = User.builder()
                .id(id)
                .name("Anderson12")
                .email("anderson12@gmail.com")
                .build();

        doReturn(Optional.of(userSaved)).when(repository).findById(id);
        doReturn(false).when(repository).existsByEmail(user.getEmail());

        doThrow(new DataIntegrityViolationException("email already exists")).when(repository).save(userSaved);

        assertThrows(ResourceAlreadyExistsException.class, () -> service.update(user));

        verify(repository, times(1)).save(any());
    }

}