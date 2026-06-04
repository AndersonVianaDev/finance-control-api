package com.andersonvianadev.finance_control_api.domain.services;

import com.andersonvianadev.finance_control_api.domain.models.User;

import java.util.UUID;

public interface IUserService {

    User save(User user);

    User findById(UUID id);

    void deleteById(UUID id);

    User update(User user);
}
