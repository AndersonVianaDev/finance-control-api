package com.andersonvianadev.finance_control_api.domain.services;

import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.models.dtos.SummaryResponseDTO;

import java.time.LocalDate;

public interface IFinancialSummaryService {
    SummaryResponseDTO summarize(User owner, LocalDate start, LocalDate finish);
}
