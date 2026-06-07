package com.andersonvianadev.finance_control_api.controllers;

import com.andersonvianadev.finance_control_api.controllers.docs.IBudgetController;
import com.andersonvianadev.finance_control_api.controllers.dtos.requests.BudgetRequestDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.requests.BudgetUpdateDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.BudgetResponseDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.PageResponseDTO;
import com.andersonvianadev.finance_control_api.controllers.mappers.BudgetMapper;
import com.andersonvianadev.finance_control_api.domain.models.Budget;
import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.services.IBudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @Override
    @PutMapping("/{id}")
    public ResponseEntity<BudgetResponseDTO> update(@AuthenticationPrincipal(expression = "user") User user,
                                                    @PathVariable UUID id,
                                                    @RequestBody @Valid BudgetUpdateDTO request) {
        Budget budget = BudgetMapper.toDomain(id, user, request);
        budget = service.update(budget);

        BudgetResponseDTO response = BudgetMapper.toResponse(budget);
        return ResponseEntity.ok(response);
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal(expression = "user") User user,
                                       @PathVariable UUID id) {
        service.delete(user, id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping
    public ResponseEntity<PageResponseDTO<BudgetResponseDTO>> findAll(@AuthenticationPrincipal(expression = "user") User user,
                                                                      @PageableDefault(size = 10) Pageable pageable) {
        Page<BudgetResponseDTO> budgets = service.findAll(user, pageable).map(BudgetMapper::toResponse);
        PageResponseDTO<BudgetResponseDTO> response = PageResponseDTO.of(budgets);

        return ResponseEntity.ok(response);
    }
}
