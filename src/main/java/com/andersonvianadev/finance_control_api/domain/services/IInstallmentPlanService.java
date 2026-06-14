package com.andersonvianadev.finance_control_api.domain.services;

import com.andersonvianadev.finance_control_api.domain.models.InstallmentPlan;
import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.models.dtos.CreationResultDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface IInstallmentPlanService {
    CreationResultDTO create(InstallmentPlan plan, boolean skipBudget);
    void generateRemainingInstallments(UUID planId, boolean skipBudget);
    InstallmentPlan findById(User user, UUID id);
    Page<InstallmentPlan> findAll(User user, Pageable pageable);
    void cancel(User user, UUID id);
}
