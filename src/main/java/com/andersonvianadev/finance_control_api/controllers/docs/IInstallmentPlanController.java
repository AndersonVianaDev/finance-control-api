package com.andersonvianadev.finance_control_api.controllers.docs;

import com.andersonvianadev.finance_control_api.controllers.dtos.requests.InstallmentPlanRequestDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.InstallmentPlanResponseDTO;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(
        name = "Installment Plans",
        description = "Operations for installment purchase plans"
)
public interface IInstallmentPlanController {

    @Operation(
            summary = "Create installment plan",
            description = """
                    Creates a new installment plan and immediately persists the first installment expense.
                    The remaining installments (2..N) are enqueued in SQS and generated asynchronously.

                    The response status is 202 Accepted, indicating that the plan was created and a message
                    has been published to the installment-generation queue for background processing.

                    Each installment is registered as a separate expense with a monthly interval
                    starting from firstDueDate. For each installment, the transaction date is automatically
                    adjusted to the next banking working day if it falls on a non-working day.
                    This check requires the Calendar API; if it is unavailable, a 502 is returned.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(
                            responseCode = "202",
                            description = "Plan created. First installment persisted. Remaining installments enqueued in SQS for async generation.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = InstallmentPlanResponseDTO.class),
                                    examples = @ExampleObject(
                                            name = "Plan accepted.",
                                            value = """
                                                    {
                                                        "id": "a1b2c3d4-1234-5678-abcd-ef0123456789",
                                                        "description": "MacBook Pro 14",
                                                        "totalAmount": 18000.00,
                                                        "installmentAmount": 1500.00,
                                                        "totalInstallments": 12,
                                                        "paidInstallments": 0,
                                                        "status": "ACTIVE",
                                                        "firstDueDate": "2026-07-01",
                                                        "category": {
                                                            "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                                            "name": "Technology",
                                                            "description": "Electronics and gadgets",
                                                            "icon": "tech",
                                                            "owner": {
                                                                "id": "9cb12d90-4623-41c2-b3fc-1a963f77bcf1",
                                                                "name": "Anderson",
                                                                "email": "anderson@gmail.com"
                                                            }
                                                        },
                                                        "firstExpense": {
                                                            "id": "f1e2d3c4-9876-5432-fedc-ba0987654321",
                                                            "transactionDate": "2026-07-01T00:00:00",
                                                            "price": 1500.00,
                                                            "description": "MacBook Pro 14",
                                                            "category": {
                                                                "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                                                "name": "Technology",
                                                                "description": "Electronics and gadgets",
                                                                "icon": "tech",
                                                                "owner": {
                                                                    "id": "9cb12d90-4623-41c2-b3fc-1a963f77bcf1",
                                                                    "name": "Anderson",
                                                                    "email": "anderson@gmail.com"
                                                                }
                                                            },
                                                            "installmentNumber": 1,
                                                            "totalInstallments": 12
                                                        }
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
                                                        "path": "/nix-finance-api/installment-plans"
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
                                            name = "Total installments must be at least 2.",
                                            value = """
                                                    {
                                                        "timestamp": "2026-06-07T09:00:00Z",
                                                        "status": 400,
                                                        "error": "totalInstallments must be greater than or equal to 2.",
                                                        "path": "/nix-finance-api/installment-plans"
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
                                                        "path": "/nix-finance-api/installment-plans"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "502",
                            description = "Calendar API unavailable. The banking working day check for the first installment could not be performed.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = StandardException.class),
                                    examples = @ExampleObject(
                                            name = "Calendar API unavailable.",
                                            value = """
                                                    {
                                                        "timestamp": "2026-06-07T09:00:00Z",
                                                        "status": 502,
                                                        "error": "External service unavailable: calendar-api",
                                                        "path": "/nix-finance-api/installment-plans"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<InstallmentPlanResponseDTO> create(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "user") User user,
            @Parameter(
                    description = "When true, skips budget limit validation for each generated installment expense.",
                    example = "false"
            )
            @RequestHeader(value = "X-SKIP-BUDGET", required = false, defaultValue = "false") boolean skipBudget,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = """
                            Data required to create the installment plan.
                            Validation rules:
                                - totalAmount: required, must be positive
                                - totalInstallments: required, minimum 2
                                - firstDueDate: required
                                - description: required, non-blank
                                - categoryId: required, must belong to the user or be a global category

                            The installmentAmount is calculated automatically as totalAmount / totalInstallments (HALF_UP rounding).
                            """,
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = InstallmentPlanRequestDTO.class),
                            examples = @ExampleObject(
                                    name = "Valid installment plan.",
                                    value = """
                                            {
                                                "totalAmount": 18000.00,
                                                "totalInstallments": 12,
                                                "firstDueDate": "2026-07-01",
                                                "description": "MacBook Pro 14",
                                                "categoryId": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
                                            }
                                            """
                            )
                    )
            )
            @RequestBody @Valid InstallmentPlanRequestDTO request
    );
}
