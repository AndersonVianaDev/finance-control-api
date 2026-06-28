package com.andersonvianadev.finance_control_api.controllers.docs;

import com.andersonvianadev.finance_control_api.controllers.dtos.requests.IncomeRequestDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.requests.IncomeUpdateDTO;
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
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
            summary = "Find income by ID",
            description = """
                    Returns a single income by its ID.

                    The income must belong to the authenticated user.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Income found successfully.",
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
                                                        "path": "/nix-finance-api/incomes/{id}"
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
                                                        "path": "/nix-finance-api/incomes/{id}"
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
                            description = "Incomes retrieved successfully.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = PageResponseDTO.class),
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

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete income by ID",
            description = """
                    Permanently deletes an income by its ID.

                    The income must belong to the authenticated user.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "Income deleted successfully.",
                            content = @Content
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Income not found or does not belong to the authenticated user.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = StandardException.class),
                                    examples = @ExampleObject(
                                            name = "Income not found.",
                                            value = """
                                                    {
                                                        "timestamp": "2026-06-07T09:00:00Z",
                                                        "status": 404,
                                                        "error": "Income not found.",
                                                        "path": "/nix-finance-api/incomes/{id}"
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
                                                        "path": "/nix-finance-api/incomes/{id}"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<Void> delete(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "user") User user,
            @Parameter(description = "Income UUID.", required = true)
            @PathVariable UUID id
    );

    @PutMapping("/{id}")
    @Operation(
            summary = "Update income",
            description = """
                    Updates an existing income for the authenticated user.

                    All fields are optional — only provided fields are applied.
                    Duplicate check is applied after mutation, excluding the income itself.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Income updated successfully.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = IncomeResponseDTO.class),
                                    examples = @ExampleObject(
                                            name = "Income updated.",
                                            value = """
                                                    {
                                                        "id": "a1b2c3d4-0000-1111-2222-333344445555",
                                                        "transactionDate": "2026-06-05T00:00:00",
                                                        "price": 5500.00,
                                                        "description": "Updated monthly salary",
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
                            description = "Updated values conflict with another existing income.",
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
                                                        "path": "/nix-finance-api/incomes/{id}"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Income not found or new category not accessible by the user.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = StandardException.class),
                                    examples = @ExampleObject(
                                            name = "Income not found.",
                                            value = """
                                                    {
                                                        "timestamp": "2026-06-07T09:00:00Z",
                                                        "status": 404,
                                                        "error": "Income not found.",
                                                        "path": "/nix-finance-api/incomes/{id}"
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
                                            name = "Invalid price.",
                                            value = """
                                                    {
                                                        "timestamp": "2026-06-07T09:00:00Z",
                                                        "status": 400,
                                                        "error": "the price value must be positive.",
                                                        "path": "/nix-finance-api/incomes/{id}"
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
                                                        "path": "/nix-finance-api/incomes/{id}"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<IncomeResponseDTO> update(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "user") User user,
            @Parameter(description = "Income UUID.", required = true)
            @PathVariable UUID id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = """
                            Fields to update. All fields are optional — only provided fields are applied.
                            """,
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = IncomeUpdateDTO.class),
                            examples = @ExampleObject(
                                    name = "Partial update.",
                                    value = """
                                            {
                                                "price": 5500.00,
                                                "description": "Updated monthly salary"
                                            }
                                            """
                            )
                    )
            )
            @RequestBody @Valid IncomeUpdateDTO request
    );

    @GetMapping(params = {"start", "finish"})
    @Operation(
            summary = "List incomes by transaction date range",
            description = """
                    Returns a paginated list of the authenticated user's incomes whose transactionDate
                    falls within the given date range (both start and finish are inclusive, covering the full day).

                    Both parameters must be provided in ISO-8601 date format (yyyy-MM-dd).
                    The filter covers the entirety of the finish day — incomes registered at any time
                    on the finish date are included.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Page of incomes within the range. May be empty if no incomes fall within the specified dates.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = PageResponseDTO.class),
                                    examples = @ExampleObject(
                                            name = "Page with one matching income.",
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
    ResponseEntity<PageResponseDTO<IncomeResponseDTO>> findBetweenTransactionDate(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "user") User user,
            @Parameter(description = "Start date of the range (inclusive), format: yyyy-MM-dd", example = "2026-06-01")
            @RequestParam(value = "start") LocalDate start,
            @Parameter(description = "End date of the range (inclusive, full day), format: yyyy-MM-dd", example = "2026-06-30")
            @RequestParam(value = "finish") LocalDate finish,
            @PageableDefault(size = 10) Pageable pageable
    );
}
