package com.andersonvianadev.finance_control_api.controllers;

import com.andersonvianadev.finance_control_api.controllers.docs.IFinancialSummaryController;
import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.models.dtos.SummaryResponseDTO;
import com.andersonvianadev.finance_control_api.domain.services.IFinancialSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/financial-summary")
@RequiredArgsConstructor
public class FinancialSummaryController implements IFinancialSummaryController {

    private final IFinancialSummaryService service;

    @Override
    public ResponseEntity<SummaryResponseDTO> getSummary(
            @AuthenticationPrincipal(expression = "user") User owner,
            @RequestParam(name = "start", required = false) LocalDate start,
            @RequestParam(name = "finish", required = false) LocalDate finish
    ) {
        SummaryResponseDTO response = service.summarize(owner, start, finish);

        return ResponseEntity.ok(response);
    }
}
