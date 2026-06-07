package com.andersonvianadev.finance_control_api.controllers;

import com.andersonvianadev.finance_control_api.controllers.docs.IBudgetController;
import com.andersonvianadev.finance_control_api.controllers.dtos.requests.BudgetRequestDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.BudgetResponseDTO;
import com.andersonvianadev.finance_control_api.controllers.mappers.BudgetMapper;
import com.andersonvianadev.finance_control_api.domain.models.Budget;
import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.services.IBudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/budgets")
@RequiredArgsConstructor
public class BudgetController implements IBudgetController {

    private final IBudgetService service;

    @Override
    @PostMapping
    public ResponseEntity<BudgetResponseDTO> save(@AuthenticationPrincipal(expression = "user") User user,
                                                  @RequestBody @Valid BudgetRequestDTO request) {
        Budget budget = BudgetMapper.toDomain(user, request);
        budget = service.save(budget);

        BudgetResponseDTO response = BudgetMapper.toResponse(budget);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<BudgetResponseDTO> findById(@AuthenticationPrincipal(expression = "user") User user,
                                                      @PathVariable UUID id) {
        Budget budget = service.findById(user, id);
        BudgetResponseDTO response = BudgetMapper.toResponse(budget);

        return ResponseEntity.ok(response);
    }
}
