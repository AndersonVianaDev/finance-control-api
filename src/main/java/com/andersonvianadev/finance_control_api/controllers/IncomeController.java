package com.andersonvianadev.finance_control_api.controllers;

import com.andersonvianadev.finance_control_api.controllers.docs.IIncomeController;
import com.andersonvianadev.finance_control_api.controllers.dtos.requests.IncomeRequestDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.IncomeResponseDTO;
import com.andersonvianadev.finance_control_api.controllers.mappers.IncomeMapper;
import com.andersonvianadev.finance_control_api.domain.models.Income;
import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.services.IIncomeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/incomes")
@RequiredArgsConstructor
public class IncomeController implements IIncomeController {

    private final IIncomeService service;

    @Override
    @PostMapping
    public ResponseEntity<IncomeResponseDTO> save(
            @AuthenticationPrincipal(expression = "user") User user,
            @RequestBody @Valid IncomeRequestDTO request
    ) {
        Income income = service.save(IncomeMapper.toDomain(user, request), false);
        IncomeResponseDTO response = IncomeMapper.toResponse(income);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
