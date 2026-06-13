package com.andersonvianadev.finance_control_api.controllers;

import com.andersonvianadev.finance_control_api.controllers.docs.IInstallmentPlanController;
import com.andersonvianadev.finance_control_api.controllers.dtos.requests.InstallmentPlanRequestDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.InstallmentPlanResponseDTO;
import com.andersonvianadev.finance_control_api.controllers.mappers.InstallmentPlanMapper;
import com.andersonvianadev.finance_control_api.domain.models.InstallmentPlan;
import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.services.IInstallmentPlanService;
import com.andersonvianadev.finance_control_api.domain.services.IInstallmentPlanService.CreationResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/installment-plans")
@RequiredArgsConstructor
public class InstallmentPlanController implements IInstallmentPlanController {

    private final IInstallmentPlanService service;

    @Override
    @PostMapping
    public ResponseEntity<InstallmentPlanResponseDTO> create(
            @AuthenticationPrincipal(expression = "user") User user,
            @RequestHeader(value = "X-SKIP-BUDGET", required = false, defaultValue = "false") boolean skipBudget,
            @RequestBody @Valid InstallmentPlanRequestDTO request) {

        InstallmentPlan plan = InstallmentPlanMapper.toDomain(user, request);
        CreationResult result = service.create(plan, skipBudget);

        InstallmentPlanResponseDTO response = InstallmentPlanMapper.toResponse(result.plan(), result.firstExpense());

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
