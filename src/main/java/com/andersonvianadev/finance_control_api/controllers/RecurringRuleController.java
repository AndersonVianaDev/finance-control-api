package com.andersonvianadev.finance_control_api.controllers;

import com.andersonvianadev.finance_control_api.controllers.docs.IRecurringRuleController;
import com.andersonvianadev.finance_control_api.controllers.dtos.requests.RecurringRuleRequestDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.RecurringRuleResponseDTO;
import com.andersonvianadev.finance_control_api.controllers.mappers.RecurringRuleMapper;
import com.andersonvianadev.finance_control_api.domain.models.RecurringRule;
import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.services.IRecurringRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recurring-rule")
@RequiredArgsConstructor
public class RecurringRuleController implements IRecurringRuleController {

    private final IRecurringRuleService service;

    @Override
    public ResponseEntity<RecurringRuleResponseDTO> save(User owner, RecurringRuleRequestDTO request) {
        RecurringRule rule = RecurringRuleMapper.toDomain(owner, request);
        RecurringRule saved = service.save(rule);
        return ResponseEntity.status(HttpStatus.CREATED).body(RecurringRuleMapper.toResponse(saved));
    }
}
