package com.andersonvianadev.finance_control_api.controllers;

import com.andersonvianadev.finance_control_api.controllers.docs.ICategoryController;
import com.andersonvianadev.finance_control_api.controllers.dtos.requests.CategoryRequestDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.CategoryResponseDTO;
import com.andersonvianadev.finance_control_api.controllers.mappers.CategoryMapper;
import com.andersonvianadev.finance_control_api.domain.models.Category;
import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.services.ICategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController implements ICategoryController {

    private final ICategoryService service;

    @Override
    @PostMapping
    public ResponseEntity<CategoryResponseDTO> save(@AuthenticationPrincipal(expression = "user") User user,
                                                    @RequestBody @Valid CategoryRequestDTO request) {
        Category category = CategoryMapper.toDomain(user, request);
        category = service.save(category);

        CategoryResponseDTO response = CategoryMapper.toResponse(category);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
