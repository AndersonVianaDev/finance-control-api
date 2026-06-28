package com.andersonvianadev.finance_control_api.controllers.docs;

import com.andersonvianadev.finance_control_api.controllers.dtos.requests.IncomeRequestDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.IncomeResponseDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.PageResponseDTO;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@Tag(
        name = "Incomes",
        description = "Operations for income transactions."
)
public interface IIncomeController {

    @PostMapping
    @Operation(
            summary = "Create an income transaction",
            description = """
                    Records a new income transaction for the authenticated user.

                    Business rules:
                        - Duplicate transactions (same owner, category, price, date and description) are rejected.
                        - The category must belong to the authenticated user or be a global category.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Income created successfully.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = IncomeResponseDTO.class),
                                    examples = @ExampleObject(
                                            name = "Monthly salary income created.",
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
                                                        }
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "An income with the same data already exists.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = StandardException.class),
                                    examples = @ExampleObject(
                                            name = "Duplicate income.",
                                            value = """
                                                    {
                                                        "timestamp": "2026-06-07T09:00:00Z",
                                                        "status": 409,
                                                        "error": "Income already registered",
                                                        "path": "/nix-finance-api/incomes"
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
                                                        "path": "/nix-finance-api/incomes"
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
                                                        "path": "/nix-finance-api/incomes"
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
                                                        "path": "/nix-finance-api/incomes"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<IncomeResponseDTO> save(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "user") User user,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = """
                            Data required to create the income transaction.
                            Validation rules:
                                - transactionDate: required.
                                - price: required, must be positive.
                                - description: required, cannot be blank.
                                - categoryId: required, must belong to the user or be a global category.
                            """,
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = IncomeRequestDTO.class),
                            examples = @ExampleObject(
                                    name = "Monthly salary income.",
                                    value = """
                                            {
                                                "transactionDate": "2026-06-05T00:00:00",
                                                "price": 5000.00,
                                                "description": "Monthly salary",
                                                "categoryId": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
                                            }
                                            """
                            )
                    )
            )
            @RequestBody @Valid IncomeRequestDTO request
    );

    @GetMapping("/{id}")
    @Operation(
            summary = "Find Income by ID",
            description = """
                    Returns a single income by its ID.

                    The income must belong to the authenticated user.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Income created successfully.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = IncomeResponseDTO.class),
                                    examples = @ExampleObject(
                                            name = "Income found successfully.",
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
                                                        }
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Income not found or not accessible by the user.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = StandardException.class),
                                    examples = @ExampleObject(
                                            name = "Income not found.",
                                            value = """
                                                    {
                                                        "timestamp": "2026-06-07T09:00:00Z",
                                                        "status": 404,
                                                        "error": "Income with id a1b2c3d4-0000-111-2222-333 not found.",
                                                        "path": "/nix-finance-api/incomes"
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
                                                        "path": "/nix-finance-api/incomes"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<IncomeResponseDTO> findById(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "user") User user,
            @Parameter(description = "Income UUID.", required = true)
            @PathVariable UUID id
    );

    @GetMapping
    @Operation(
            summary = "List incomes",
            description = """
                    Returns a paginated list of incomes belonging to the authenticated user.
                    Pagination query parameters:
                        - page: page number (0-indexed, default: 0)
                        - size: page size (default: 10)
                        - sort: sorting criteria (e.g. sort=transactionDate,desc)
                    """,
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "retrieved successfully.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = IncomeResponseDTO.class),
                                    examples = @ExampleObject(
                                            name = "Incomes page.",
                                            value = """
                                                    {
                                                        "content": [
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
                                                                }
                                                            }
                                                        ],
                                                        "page": 0,
                                                        "size": 10,
                                                        "totalElement": 1,
                                                        "totalPages": 1,
                                                        "last": true
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
                                                        "path": "/nix-finance-api/incomes"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<PageResponseDTO<IncomeResponseDTO>> findAll(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "user") User user,
            @PageableDefault Pageable pageable
    );
}
