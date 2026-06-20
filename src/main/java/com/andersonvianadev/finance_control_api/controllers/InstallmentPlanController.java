package com.andersonvianadev.finance_control_api.controllers;

import com.andersonvianadev.finance_control_api.controllers.docs.IInstallmentPlanController;
import com.andersonvianadev.finance_control_api.controllers.dtos.requests.InstallmentPlanRequestDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.InstallmentPlanResponseDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.PageResponseDTO;
import com.andersonvianadev.finance_control_api.controllers.mappers.InstallmentPlanMapper;
import com.andersonvianadev.finance_control_api.domain.models.Expense;
import com.andersonvianadev.finance_control_api.domain.models.InstallmentPlan;
import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.models.dtos.CreationResultDTO;
import com.andersonvianadev.finance_control_api.domain.services.IExpenseService;
import com.andersonvianadev.finance_control_api.domain.services.IInstallmentPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/installment-plans")
@RequiredArgsConstructor
public class InstallmentPlanController implements IInstallmentPlanController {

    private final IInstallmentPlanService service;
    private final IExpenseService expenseService;

    @Override
    @PostMapping
    public ResponseEntity<InstallmentPlanResponseDTO> create(
            @AuthenticationPrincipal(expression = "user") User user,
            @RequestHeader(value = "X-SKIP-BUDGET", required = false, defaultValue = "false") boolean skipBudget,
            @RequestBody @Valid InstallmentPlanRequestDTO request) {

        InstallmentPlan plan = InstallmentPlanMapper.toDomain(user, request);
        CreationResultDTO result = service.create(plan, skipBudget);

        InstallmentPlanResponseDTO response = InstallmentPlanMapper.toResponse(
                result.plan(), List.of(result.firstExpense()));

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<InstallmentPlanResponseDTO> findById(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable UUID id
    ) {
        InstallmentPlan plan = service.findById(user, id);
        List<Expense> expenses = expenseService.findByInstallmentPlan(plan.getId());
        return ResponseEntity.ok(InstallmentPlanMapper.toResponse(plan, expenses));
    }

    @Override
    @GetMapping
    public ResponseEntity<PageResponseDTO<InstallmentPlanResponseDTO>> findAll(
            @AuthenticationPrincipal(expression = "user") User user,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<InstallmentPlanResponseDTO> responsePage = service.findAll(user, pageable)
                .map(InstallmentPlanMapper::toResponse);
        return ResponseEntity.ok(PageResponseDTO.of(responsePage));
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable UUID id
    ) {
        service.cancel(user, id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping(params = {"start", "finish"})
    public ResponseEntity<PageResponseDTO<InstallmentPlanResponseDTO>> findBetweenFirstDueDate(
            @AuthenticationPrincipal(expression = "user") User user,
            @RequestParam(value = "start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(value = "finish") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate finish,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<InstallmentPlanResponseDTO> responsePage = service.findBetweenFirstDueDate(
                user, start, finish, pageable
        ).map(InstallmentPlanMapper::toResponse);

        return ResponseEntity.ok(PageResponseDTO.of(responsePage));
    }
}
