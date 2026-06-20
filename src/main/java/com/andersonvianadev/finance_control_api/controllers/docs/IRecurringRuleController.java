package com.andersonvianadev.finance_control_api.controllers.docs;

import com.andersonvianadev.finance_control_api.controllers.dtos.requests.RecurringRuleRequestDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.RecurringRuleResponseDTO;
import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.infra.exceptions.StandardException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@Tag(
        name = "Recurring Rules",
        description = "Operations for recurring income and expense rules. " +
                "Rules are executed automatically every day at midnight. " +
                "MONTHLY rules fire on the day-of-month of transactionDate. " +
                "WEEKLY rules fire on the day-of-week of transactionDate."
)
public interface IRecurringRuleController {

    @PostMapping
    @Operation(
            summary = "Create a recurring rule",
            description = """
                    Creates a rule that automatically generates income or expense transactions on the configured schedule.

                    Scheduling behaviour:
                        - MONTHLY: the day-of-month from transactionDate determines when the transaction is generated each month (e.g. day 5 → fires every 5th).
                        - WEEKLY: the day-of-week from transactionDate determines when the transaction is generated each week (e.g. Monday → fires every Monday).

                    Business rules:
                        - Expense rules are subject to budget validation on generation; income rules are not.
                        - Duplicate rules (same owner, category, price, date and description) are rejected.
                        - The category must belong to the authenticated user or be a global category.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Recurring rule created successfully.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = RecurringRuleResponseDTO.class),
                                    examples = @ExampleObject(
                                            name = "Monthly income rule created.",
                                            value = """
                                                    {
                                                        "id": "a1b2c3d4-0000-1111-2222-333344445555",
                                                        "transactionDate": "2026-06-05T00:00:00",
                                                        "price": 5000.00,
                                                        "description": "Monthly salary",
                                                        "category": {
                                                            "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                                            "name": "salary",
                                                            "description": "Employment income",
                                                            "icon": "wallet",
                                                            "owner": {
                                                                "id": "9cb12d90-4623-41c2-b3fc-1a963f77bcf1",
                                                                "name": "Anderson",
                                                                "email": "anderson@gmail.com"
                                                            }
                                                        },
                                                        "owner": {
                                                            "id": "9cb12d90-4623-41c2-b3fc-1a963f77bcf1",
                                                            "name": "Anderson",
                                                            "email": "anderson@gmail.com"
                                                        },
                                                        "transactionPeriodType": "MONTHLY",
                                                        "recurringType": "INCOME",
                                                        "isActive": true
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "A recurring rule with the same data already exists.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = StandardException.class),
                                    examples = @ExampleObject(
                                            name = "Duplicate recurring rule.",
                                            value = """
                                                    {
                                                        "timestamp": "2026-06-07T09:00:00Z",
                                                        "status": 409,
                                                        "error": "Recurring rule already registered",
                                                        "path": "/nix-finance-api/recurring-rule"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Category not found or not accessible by the user.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = StandardException.class),
                                    examples = @ExampleObject(
                                            name = "Category not found.",
                                            value = """
                                                    {
                                                        "timestamp": "2026-06-07T09:00:00Z",
                                                        "status": 404,
                                                        "error": "Category not found.",
                                                        "path": "/nix-finance-api/recurring-rule"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid field value.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = StandardException.class),
                                    examples = @ExampleObject(
                                            name = "Missing required field.",
                                            value = """
                                                    {
                                                        "timestamp": "2026-06-07T09:00:00Z",
                                                        "status": 400,
                                                        "error": "price field cannot be null.",
                                                        "path": "/nix-finance-api/recurring-rule"
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
                                                        "timestamp": "2026-06-07T09:00:00Z",
                                                        "status": 401,
                                                        "error": "Unauthorized",
                                                        "path": "/nix-finance-api/recurring-rule"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<RecurringRuleResponseDTO> save(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "user") User owner,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = """
                            Data required to create the recurring rule.
                            Validation rules:
                                - transactionDate: required. Day-of-month used for MONTHLY; day-of-week used for WEEKLY.
                                - price: required, must be positive.
                                - description: required, cannot be blank.
                                - categoryId: required, must belong to the user or be a global category.
                                - transactionPeriodType: required (MONTHLY or WEEKLY).
                                - recurringType: required (INCOME or EXPENSE).
                            """,
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = RecurringRuleRequestDTO.class),
                            examples = @ExampleObject(
                                    name = "Monthly income rule.",
                                    value = """
                                            {
                                                "transactionDate": "2026-06-05T00:00:00",
                                                "price": 5000.00,
                                                "description": "Monthly salary",
                                                "categoryId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                                "transactionPeriodType": "MONTHLY",
                                                "recurringType": "INCOME"
                                            }
                                            """
                            )
                    )
            )
            @RequestBody @Valid RecurringRuleRequestDTO request
    );

    @GetMapping("/{id}")
    @Operation(
            summary = "Get a recurring rule by ID",
            description = "Returns the recurring rule identified by the given ID. Only rules owned by the authenticated user are accessible.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Recurring rule found.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = RecurringRuleResponseDTO.class),
                                    examples = @ExampleObject(
                                            name = "Monthly income rule.",
                                            value = """
                                                    {
                                                        "id": "a1b2c3d4-0000-1111-2222-333344445555",
                                                        "transactionDate": "2026-06-05T00:00:00",
                                                        "price": 5000.00,
                                                        "description": "Monthly salary",
                                                        "category": {
                                                            "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                                            "name": "salary",
                                                            "description": "Employment income",
                                                            "icon": "wallet",
                                                            "owner": {
                                                                "id": "9cb12d90-4623-41c2-b3fc-1a963f77bcf1",
                                                                "name": "Anderson",
                                                                "email": "anderson@gmail.com"
                                                            }
                                                        },
                                                        "owner": {
                                                            "id": "9cb12d90-4623-41c2-b3fc-1a963f77bcf1",
                                                            "name": "Anderson",
                                                            "email": "anderson@gmail.com"
                                                        },
                                                        "transactionPeriodType": "MONTHLY",
                                                        "recurringType": "INCOME",
                                                        "isActive": true
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Recurring rule not found or does not belong to the authenticated user.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = StandardException.class),
                                    examples = @ExampleObject(
                                            name = "Rule not found.",
                                            value = """
                                                    {
                                                        "timestamp": "2026-06-07T09:00:00Z",
                                                        "status": 404,
                                                        "error": "Recurring rule with id a1b2c3d4-0000-1111-2222-333344445555 not found",
                                                        "path": "/nix-finance-api/recurring-rule/a1b2c3d4-0000-1111-2222-333344445555"
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
                                                        "timestamp": "2026-06-07T09:00:00Z",
                                                        "status": 401,
                                                        "error": "Unauthorized",
                                                        "path": "/nix-finance-api/recurring-rule/a1b2c3d4-0000-1111-2222-333344445555"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<RecurringRuleResponseDTO> findById(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "user") User user,
            @Parameter(description = "Recurring rule ID", required = true, example = "a1b2c3d4-0000-1111-2222-333344445555")
            @PathVariable UUID id
    );
}
