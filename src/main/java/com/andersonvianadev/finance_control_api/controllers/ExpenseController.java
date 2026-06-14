package com.andersonvianadev.finance_control_api.controllers;

import com.andersonvianadev.finance_control_api.controllers.docs.IExpenseController;
import com.andersonvianadev.finance_control_api.controllers.dtos.requests.ExpenseRequestDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.requests.ExpenseUpdateDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.ExpenseResponseDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.PageResponseDTO;
import com.andersonvianadev.finance_control_api.controllers.mappers.ExpenseMapper;
import com.andersonvianadev.finance_control_api.domain.models.Expense;
import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.services.IExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/expenses")
@RequiredArgsConstructor
public class ExpenseController implements IExpenseController {

    private final IExpenseService service;

    @Override
    @PostMapping
    public ResponseEntity<ExpenseResponseDTO> save(
            @AuthenticationPrincipal(expression = "user") User user,
            @RequestHeader(value = "X-SKIP-BUDGET", required = false, defaultValue = "false") boolean skipBudget,
            @RequestBody @Valid ExpenseRequestDTO request) {

        Expense expense = ExpenseMapper.toDomain(user, request);
        Expense saved = service.save(expense, skipBudget);
        return ResponseEntity.status(HttpStatus.CREATED).body(ExpenseMapper.toResponse(saved));
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<ExpenseResponseDTO> findById(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable UUID id
    ) {
        Expense expense = service.findById(user, id);
        ExpenseResponseDTO response = ExpenseMapper.toResponse(expense);
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping
    public ResponseEntity<PageResponseDTO<ExpenseResponseDTO>> findAll(
            @AuthenticationPrincipal(expression = "user") User user,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<ExpenseResponseDTO> responsePage = service.findAll(user, pageable).map(ExpenseMapper::toResponse);
        return ResponseEntity.ok(PageResponseDTO.of(responsePage));
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable UUID id
    ) {
        service.delete(user, id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PutMapping("/{id}")
    public ResponseEntity<ExpenseResponseDTO> update(
            @AuthenticationPrincipal(expression = "user") User user,
            @RequestHeader(value = "X-SKIP-BUDGET", required = false, defaultValue = "false") boolean skipBudget,
            @PathVariable UUID id,
            @RequestBody @Valid ExpenseUpdateDTO request
    ) {
        Expense expense = ExpenseMapper.toDomain(user, id, request);
        expense = service.update(expense, skipBudget);

        ExpenseResponseDTO response = ExpenseMapper.toResponse(expense);
        return ResponseEntity.ok(response);
    }
}
