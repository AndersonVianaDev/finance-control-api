package com.andersonvianadev.finance_control_api.domain.services.impl;

import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.services.IPasswordEncoderService;
import com.andersonvianadev.finance_control_api.domain.services.IUserService;
import com.andersonvianadev.finance_control_api.infra.exceptions.NotFoundException;
import com.andersonvianadev.finance_control_api.infra.exceptions.ResourceAlreadyExistsException;
import com.andersonvianadev.finance_control_api.infra.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    private final UserRepository repository;
    private final IPasswordEncoderService passwordEncoderService;

    @Override
    public User save(User user) {
        if(repository.existsByEmail(user.getEmail())) {
            log.warn(
                    "Attempt to create user with existing email: {}",
                    user.getEmail()
            );
            throw new ResourceAlreadyExistsException(String.format("User with email %s already exists", user.getEmail()));
        }

        final String hashPassword = passwordEncoderService.encode(user.getPassword());
        user.setPassword(hashPassword);

        try {
            return repository.save(user);
        } catch (DataIntegrityViolationException e) {
            log.error(
                    "Database constraint violation while creating user with email: {}",
                    user.getEmail(),
                    e
            );
            throw new ResourceAlreadyExistsException(String.format("User with email %s already exists", user.getEmail()));
        }
    }

    @Override
    public User findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("User with id %s not found", id)));
    }

    @Override
    public void deleteById(UUID id) {
        User user = this.findById(id);

        repository.delete(user);
    }

    @Override
    public User update(User user) {
        User userSaved = this.findById(user.getId());

        if(user.getName() != null) {
            userSaved.setName(user.getName());
        }

        if(user.getEmail() != null && !Objects.equals(user.getEmail(), userSaved.getEmail())) {
            if(repository.existsByEmail(user.getEmail())) {
                log.warn(
                        "Attempt to update the user's email with the email already registered: {}",
                        user.getEmail()
                );
                throw new ResourceAlreadyExistsException(String.format("User with email %s already exists", user.getEmail()));
            }

            userSaved.setEmail(user.getEmail());
        }

        try {
            return repository.save(userSaved);
        } catch (DataIntegrityViolationException e) {
            log.error(
                    "Database constraint violation while updating user. id={}, email={}",
                    user.getId(),
                    user.getEmail(),
                    e
            );

            throw new ResourceAlreadyExistsException(String.format("User with email %s already exists", user.getEmail()));
        }

    }
}
