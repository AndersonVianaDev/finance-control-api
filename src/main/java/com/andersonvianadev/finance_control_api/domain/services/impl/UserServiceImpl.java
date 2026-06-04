package com.andersonvianadev.finance_control_api.domain.services.impl;

import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.services.IPasswordEncoderService;
import com.andersonvianadev.finance_control_api.domain.services.IUserService;
import com.andersonvianadev.finance_control_api.infra.exceptions.ResourceAlreadyExistsException;
import com.andersonvianadev.finance_control_api.infra.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

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
}
