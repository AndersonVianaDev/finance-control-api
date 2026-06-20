package com.andersonvianadev.finance_control_api.controllers;

import com.andersonvianadev.finance_control_api.controllers.docs.IRecurringRuleController;
import com.andersonvianadev.finance_control_api.controllers.dtos.requests.RecurringRuleRequestDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.requests.RecurringRuleUpdateDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.PageResponseDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.RecurringRuleResponseDTO;
import com.andersonvianadev.finance_control_api.controllers.mappers.RecurringRuleMapper;
import com.andersonvianadev.finance_control_api.domain.models.RecurringRule;
import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.services.IRecurringRuleService;
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

    @Override
    @GetMapping
    public ResponseEntity<PageResponseDTO<RecurringRuleResponseDTO>> findAll(
            @AuthenticationPrincipal(expression = "user") User user,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<RecurringRuleResponseDTO> page = service.findAll(user, pageable)
                .map(RecurringRuleMapper::toResponse);
        return ResponseEntity.ok(PageResponseDTO.of(page));
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
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<RecurringRuleResponseDTO> toggle(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable UUID id
    ) {
        RecurringRule rule = service.toggle(user, id);
        return ResponseEntity.ok(RecurringRuleMapper.toResponse(rule));
    }

    @Override
    @PutMapping("/{id}")
    public ResponseEntity<RecurringRuleResponseDTO> update(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable UUID id,
            @RequestBody @Valid RecurringRuleUpdateDTO request
    ) {
        RecurringRule rule = RecurringRuleMapper.toDomain(user, id, request);
        rule = service.update(rule);
        return ResponseEntity.ok(RecurringRuleMapper.toResponse(rule));
    }
}
