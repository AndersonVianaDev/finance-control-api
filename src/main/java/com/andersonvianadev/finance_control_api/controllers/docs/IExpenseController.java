package com.andersonvianadev.finance_control_api.controllers.docs;

import com.andersonvianadev.finance_control_api.controllers.dtos.requests.ExpenseRequestDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.ExpenseResponseDTO;
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
        name = "Expenses",
        description = "Operations for expense transactions"
)
public interface IExpenseController {

    @Operation(
            summary = "Register expense",
            description = """
                    Registers a new expense for the authenticated user.

                    The category must belong to the user or be a global category.
                    Duplicate expenses (same owner, category, date, price and description) are rejected.
                    Budget validation is applied by default; send X-SKIP-BUDGET: true to bypass it.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Expense registered successfully.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ExpenseResponseDTO.class),
                                    examples = @ExampleObject(
                                            name = "Expense created.",
                                            value = """
                                                    {
                                                        "id": "f1e2d3c4-9876-5432-fedc-ba0987654321",
                                                        "transactionDate": "2026-06-13T10:00:00",
                                                        "price": 150.00,
                                                        "description": "Monthly gym membership",
                                                        "category": {
                                                            "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                                            "name": "Fitness",
                                                            "description": "Gym and sports expenses",
                                                            "icon": "fitness",
                                                            "owner": {
                                                                "id": "9cb12d90-4623-41c2-b3fc-1a963f77bcf1",
                                                                "name": "Anderson",
                                                                "email": "anderson@gmail.com"
                                                            }
                                                        },
                                                        "installmentNumber": null,
                                                        "totalInstallments": null
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Duplicate expense already registered.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = StandardException.class),
                                    examples = @ExampleObject(
                                            name = "Expense already registered.",
                                            value = """
                                                    {
                                                        "timestamp": "2026-06-07T09:00:00Z",
                                                        "status": 409,
                                                        "error": "Expense already registered",
                                                        "path": "/nix-finance-api/expenses"
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
                                                        "path": "/nix-finance-api/expenses"
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
                                                        "error": "categoryId must not be null.",
                                                        "path": "/nix-finance-api/expenses"
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
                                                        "path": "/nix-finance-api/expenses"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<ExpenseResponseDTO> save(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "user") User user,
            @Parameter(
                    description = "When true, skips budget limit validation for this expense.",
                    example = "false"
            )
            @RequestHeader(value = "X-SKIP-BUDGET", required = false, defaultValue = "false") boolean skipBudget,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = """
                            Data required to register the expense.
                            Validation rules:
                                - transactionDate: required
                                - price: required
                                - description: required
                                - categoryId: required, must belong to the user or be a global category
                            """,
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = ExpenseRequestDTO.class),
                            examples = @ExampleObject(
                                    name = "Valid expense.",
                                    value = """
                                            {
                                                "transactionDate": "2026-06-13T10:00:00",
                                                "price": 150.00,
                                                "description": "Monthly gym membership",
                                                "categoryId": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
                                            }
                                            """
                            )
                    )
            )
            @RequestBody @Valid ExpenseRequestDTO request
    );
}
