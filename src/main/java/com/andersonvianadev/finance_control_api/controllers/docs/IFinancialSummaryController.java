package com.andersonvianadev.finance_control_api.controllers.docs;

import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.models.dtos.SummaryResponseDTO;
import com.andersonvianadev.finance_control_api.infra.exceptions.StandardException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Tag(
        name = "Financial Summary",
        description = "Operations for user financial summary."
)
public interface IFinancialSummaryController {

    @GetMapping
    @Operation(
            summary = "Retrieve financial summary by date range",
            description = """
                    Returns the financial summary for the authenticated user within a given date range.

                    Both parameters are optional. When omitted, the default period is the current calendar month
                    (first day to last day of the current month).

                    The response is split into three independent buckets:

                    Realized — transactions already recorded with transactionDate <= today:
                        - realizedIncome: total income received up to today within the period.
                        - realizedExpense: total expense paid up to today within the period.
                        - realizedBalance: realizedIncome minus realizedExpense.
                        - realizedCount: number of realized transactions.

                    Scheduled — transactions manually registered by the user for a future date (transactionDate > today):
                        - scheduledIncome: committed future income already registered in the system.
                        - scheduledExpense: committed future expense already registered in the system.
                        - scheduledBalance: scheduledIncome minus scheduledExpense.
                        - scheduledCount: number of scheduled transactions.

                    Projected — estimated future transactions generated from active recurring rules not yet posted:
                        - projectedIncome: estimated income from recurring rules within the remaining period.
                        - projectedExpense: estimated expense from recurring rules within the remaining period.
                        - projectedBalance: projectedIncome minus projectedExpense.
                        - projectedCount: number of estimated recurring occurrences.

                    Totals:
                        - totalBalance: realizedBalance + scheduledBalance + projectedBalance (full period outlook).
                        - totalCount: realizedCount + scheduledCount + projectedCount.
                        - commitmentRate: percentage of realized income already committed to expenses
                          (realizedExpense / realizedIncome * 100). Returns 0 when there is no realized income.

                    Scheduled and projected buckets are only populated when the finish date is after today.
                    Both parameters must be provided in ISO-8601 date format (yyyy-MM-dd).
                    """,
            security = @SecurityRequirement(name = "bearerToken"),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Financial summary retrieved successfully.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = SummaryResponseDTO.class),
                                    examples = @ExampleObject(
                                            name = "Summary for July 2026, queried on July 5th.",
                                            value = """
                                                    {
                                                        "realizedIncome": 5000.00,
                                                        "realizedExpense": 1850.75,
                                                        "realizedBalance": 3149.25,
                                                        "realizedCount": 8,
                                                        "scheduledIncome": 0.00,
                                                        "scheduledExpense": 450.00,
                                                        "scheduledBalance": -450.00,
                                                        "scheduledCount": 2,
                                                        "projectedIncome": 1200.00,
                                                        "projectedExpense": 320.00,
                                                        "projectedBalance": 880.00,
                                                        "projectedCount": 3,
                                                        "totalBalance": 3579.25,
                                                        "totalCount": 13,
                                                        "commitmentRate": 37.02
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized. Missing or invalid JWT token.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = StandardException.class),
                                    examples = @ExampleObject(
                                            name = "Missing or invalid token.",
                                            value = """
                                                    {
                                                        "timestamp": "2026-07-05T09:00:00Z",
                                                        "status": 401,
                                                        "error": "Unauthorized",
                                                        "path": "/nix-finance-api/financial-summary"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<SummaryResponseDTO> getSummary(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "user") User owner,
            @Parameter(description = "Start date of the period (inclusive), format: yyyy-MM-dd. Defaults to the first day of the current month.", example = "2026-07-01")
            @RequestParam(value = "start", required = false) LocalDate start,
            @Parameter(description = "End date of the period (inclusive, full day), format: yyyy-MM-dd. Defaults to the last day of the current month.", example = "2026-07-31")
            @RequestParam(value = "finish", required = false) LocalDate finish
    );
}
