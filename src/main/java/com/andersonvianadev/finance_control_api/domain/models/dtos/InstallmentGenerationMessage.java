package com.andersonvianadev.finance_control_api.domain.models.dtos;

import java.util.UUID;

public record InstallmentGenerationMessage(UUID planId, boolean skipBudget) {}
