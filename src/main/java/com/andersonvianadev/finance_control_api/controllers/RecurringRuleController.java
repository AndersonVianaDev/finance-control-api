package com.andersonvianadev.finance_control_api.controllers;

import com.andersonvianadev.finance_control_api.controllers.docs.IRecurringRuleController;
import com.andersonvianadev.finance_control_api.controllers.dtos.requests.RecurringRuleRequestDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.RecurringRuleResponseDTO;
import com.andersonvianadev.finance_control_api.controllers.mappers.RecurringRuleMapper;
import com.andersonvianadev.finance_control_api.domain.models.RecurringRule;
import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.services.IRecurringRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/recurring-rule")
@RequiredArgsConstructor
public class RecurringRuleController implements IRecurringRuleController {

    private final IRecurringRuleService service;

    @Override
    @PostMapping
    public ResponseEntity<RecurringRuleResponseDTO> save(
            @AuthenticationPrincipal(expression = "user") User owner,
            @RequestBody @Valid RecurringRuleRequestDTO request
    ) {
        RecurringRule rule = RecurringRuleMapper.toDomain(owner, request);
        RecurringRule saved = service.save(rule);
        return ResponseEntity.status(HttpStatus.CREATED).body(RecurringRuleMapper.toResponse(saved));
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<RecurringRuleResponseDTO> findById(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable UUID id
    ) {
        RecurringRule recurringRule = service.findById(user, id);
        RecurringRuleResponseDTO response = RecurringRuleMapper.toResponse(recurringRule);
        return ResponseEntity.ok(response);
    }
}
