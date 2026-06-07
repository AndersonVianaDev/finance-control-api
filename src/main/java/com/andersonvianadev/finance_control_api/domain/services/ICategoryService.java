package com.andersonvianadev.finance_control_api.domain.services;

import com.andersonvianadev.finance_control_api.domain.models.Category;
import com.andersonvianadev.finance_control_api.domain.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ICategoryService {
    Category save(Category category);
    Category findByIdAndOwnerOrOwnerIsNull(UUID id, User owner);
    void delete(UUID id, User owner);
    Category update(Category category);
    Page<Category> findAll(User owner, Pageable pageable);
}
