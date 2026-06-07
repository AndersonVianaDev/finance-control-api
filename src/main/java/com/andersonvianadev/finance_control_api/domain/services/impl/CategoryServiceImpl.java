package com.andersonvianadev.finance_control_api.domain.services.impl;

import com.andersonvianadev.finance_control_api.domain.models.Category;
import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.models.enums.UserRole;
import com.andersonvianadev.finance_control_api.domain.services.ICategoryService;
import com.andersonvianadev.finance_control_api.domain.services.IUserService;
import com.andersonvianadev.finance_control_api.infra.exceptions.NotFoundException;
import com.andersonvianadev.finance_control_api.infra.exceptions.QuotaExceededException;
import com.andersonvianadev.finance_control_api.infra.exceptions.ResourceAlreadyExistsException;
import com.andersonvianadev.finance_control_api.infra.repositories.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements ICategoryService {

    private final CategoryRepository repository;
    private final IUserService userService;

    @Value("${app.saas.plans.free.category-limit}")
    private Integer freePlanCategoryLimit;

    @Override
    public Category save(Category category) {
        if(repository.existsByNameAndOwnerOrGlobal(category.getName(), category.getOwner())) {
            log.warn(
                    "Attempt to create category with existing name: {}",
                    category.getName()
            );
            throw new ResourceAlreadyExistsException(String.format("A category with this name %s already exists.", category.getName()));
        }

        User owner = category.getOwner();
        int count = repository.countByOwner(owner);
        if(UserRole.ROLE_USER.equals(owner.getRole())) {
            if(freePlanCategoryLimit <= count) {
                log.warn(
                        "User {} reached the free plan category limit. Current categories: {}, Limit: {}",
                        owner.getName(),
                        count,
                        freePlanCategoryLimit
                );
                throw new QuotaExceededException("You have reached the maximum category limit for the free plan.");
            }
        }

        try {
            return repository.save(category);
        } catch (DataIntegrityViolationException e) {
            log.error(
                    "Database constraint violation while creating user with name: {}",
                    category.getName(),
                    e
            );
            throw new ResourceAlreadyExistsException(String.format("A category with this name %s already exists.", category.getName()));
        }

    }

    @Override
    public Category findByIdAndOwnerOrOwnerIsNull(UUID id, User owner) {
        return repository.findByIdAndOwnerOrOwnerIsNull(id, owner)
                .orElseThrow(() -> new NotFoundException(String.format("Category with id %s not found", id.toString())));
    }

    @Override
    public void delete(UUID id, User owner) {
        Category category = repository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new NotFoundException(String.format("Category with id %s not found", id.toString())));

        repository.delete(category);
    }

    @Override
    public Category update(Category category) {
        UUID categoryId = category.getId();
        Category categoryActual = repository.findByIdAndOwner(categoryId, category.getOwner())
                .orElseThrow(() -> new NotFoundException(String.format("Category with id %s not found", categoryId.toString())));

        if(category.getName() != null && !category.getName().equals(categoryActual.getName())) {
            categoryActual.setName(category.getName());
        }

        if(category.getDescription() != null && !category.getDescription().equals(categoryActual.getDescription())) {
            categoryActual.setDescription(category.getDescription());
        }

        if(category.getIcon() != null && !category.getIcon().equals(categoryActual.getIcon())) {
            categoryActual.setIcon(category.getIcon());
        }

        return repository.save(categoryActual);
    }

    @Override
    public Page<Category> findAll(User owner, Pageable pageable) {
        return repository.findByOwnerOrOwnerIsNull(owner, pageable);
    }
}
