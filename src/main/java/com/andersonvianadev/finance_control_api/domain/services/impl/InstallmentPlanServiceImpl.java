package com.andersonvianadev.finance_control_api.domain.services.impl;

import com.andersonvianadev.finance_control_api.domain.models.Category;
import com.andersonvianadev.finance_control_api.domain.models.Expense;
import com.andersonvianadev.finance_control_api.domain.models.InstallmentPlan;
import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.models.dtos.CreationResultDTO;
import com.andersonvianadev.finance_control_api.domain.models.dtos.InstallmentGenerationMessage;
import com.andersonvianadev.finance_control_api.domain.services.ICategoryService;
import com.andersonvianadev.finance_control_api.domain.services.IExpenseService;
import com.andersonvianadev.finance_control_api.domain.services.IInstallmentPlanService;
import com.andersonvianadev.finance_control_api.infra.exceptions.ExternalServiceException;
import com.andersonvianadev.finance_control_api.infra.exceptions.NotFoundException;
import com.andersonvianadev.finance_control_api.infra.messaging.ISqsMessageSender;
import com.andersonvianadev.finance_control_api.infra.repositories.InstallmentPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstallmentPlanServiceImpl implements IInstallmentPlanService {

    private final InstallmentPlanRepository repository;
    private final ICategoryService categoryService;
    private final IExpenseService expenseService;
    private final ISqsMessageSender sqsMessageSender;

    @Value("${aws.sqs.queues.installment-generation}")
    private String installmentGenerationQueueUrl;

    @Override
    public CreationResultDTO create(InstallmentPlan plan, boolean skipBudget) {
        User owner = plan.getOwner();
        Category category = categoryService.findByIdAndOwnerOrOwnerIsNull(plan.getCategory().getId(), owner);
        plan.setCategory(category);

        InstallmentPlan savedPlan = repository.save(plan);

        Expense firstExpense = Expense.builder()
                .owner(owner)
                .category(category)
                .description(savedPlan.getDescription())
                .price(savedPlan.getInstallmentAmount())
                .transactionDate(savedPlan.getFirstDueDate().atStartOfDay())
                .installmentPlan(savedPlan)
                .installmentNumber(1)
                .build();

        Expense savedFirstExpense = expenseService.save(firstExpense, skipBudget);

        log.info("InstallmentPlan id={} created. Enqueueing generation for installments 2..{}",
                savedPlan.getId(), savedPlan.getTotalInstallments());

        sqsMessageSender.send(installmentGenerationQueueUrl,
                new InstallmentGenerationMessage(savedPlan.getId(), skipBudget));

        return new CreationResultDTO(savedPlan, savedFirstExpense);
    }

    @Override
    public void generateRemainingInstallments(UUID planId, boolean skipBudget) {
        InstallmentPlan plan = repository.findById(planId)
                .orElseThrow(() -> new NotFoundException("InstallmentPlan not found: " + planId));

        log.info("Generating installments 2..{} for plan id={}", plan.getTotalInstallments(), plan.getId());

        for (int i = 2; i <= plan.getTotalInstallments(); i++) {
            try {
                Expense installment = Expense.builder()
                        .owner(plan.getOwner())
                        .category(plan.getCategory())
                        .description(plan.getDescription())
                        .price(plan.getInstallmentAmount())
                        .transactionDate(plan.getFirstDueDate().plusMonths(i - 1L).atStartOfDay())
                        .installmentPlan(plan)
                        .installmentNumber(i)
                        .build();

                expenseService.save(installment, skipBudget);

                log.debug("Installment {}/{} created for plan id={}", i, plan.getTotalInstallments(), plan.getId());
            } catch (ExternalServiceException e) {
                throw e;
            } catch (Exception e) {
                log.error("Failed to create installment {}/{} for plan id={}. Reason: {}",
                        i, plan.getTotalInstallments(), plan.getId(), e.getMessage(), e);
            }
        }

        log.info("Installment generation finished for plan id={}", plan.getId());
    }
}
