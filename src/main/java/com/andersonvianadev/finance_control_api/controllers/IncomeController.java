package com.andersonvianadev.finance_control_api.controllers;

import com.andersonvianadev.finance_control_api.controllers.docs.IIncomeController;
import com.andersonvianadev.finance_control_api.controllers.dtos.requests.IncomeRequestDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.requests.IncomeUpdateDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.IncomeResponseDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.PageResponseDTO;
import com.andersonvianadev.finance_control_api.controllers.mappers.IncomeMapper;
import com.andersonvianadev.finance_control_api.domain.models.Income;
import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.services.IIncomeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

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

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<IncomeResponseDTO> findById(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable UUID id
    ) {
        Income income = service.findById(user, id);
        IncomeResponseDTO response = IncomeMapper.toResponse(income);
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping
    public ResponseEntity<PageResponseDTO<IncomeResponseDTO>> findAll(
            @AuthenticationPrincipal(expression = "user") User user,
            @PageableDefault Pageable pageable
    ) {
        Page<IncomeResponseDTO> page = service.findAll(user, pageable)
                .map(IncomeMapper::toResponse);
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
    @PutMapping("/{id}")
    public ResponseEntity<IncomeResponseDTO> update(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable UUID id,
            @RequestBody @Valid IncomeUpdateDTO request
    ) {
        Income income = IncomeMapper.toDomain(user, id, request);
        income = service.update(income);
        return ResponseEntity.ok(IncomeMapper.toResponse(income));
    }

    @Override
    @GetMapping(params = {"start", "finish"})
    public ResponseEntity<PageResponseDTO<IncomeResponseDTO>> findBetweenTransactionDate(
            @AuthenticationPrincipal(expression = "user") User user,
            @RequestParam(value = "start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(value = "finish") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate finish,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<IncomeResponseDTO> responsePage = service.findBetweenTransactionDate(
                user, start.atStartOfDay(), finish.atTime(LocalTime.MAX), pageable
        ).map(IncomeMapper::toResponse);

        return ResponseEntity.ok(PageResponseDTO.of(responsePage));
    }
}
