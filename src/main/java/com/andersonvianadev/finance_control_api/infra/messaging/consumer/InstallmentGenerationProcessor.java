package com.andersonvianadev.finance_control_api.infra.messaging.consumer;

import com.andersonvianadev.finance_control_api.domain.models.dtos.InstallmentGenerationMessage;
import com.andersonvianadev.finance_control_api.domain.services.IInstallmentPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstallmentGenerationProcessor {

    private final IInstallmentPlanService installmentPlanService;

    public void process(InstallmentGenerationMessage message) {
        installmentPlanService.generateRemainingInstallments(message.planId(), message.skipBudget());
    }
}
