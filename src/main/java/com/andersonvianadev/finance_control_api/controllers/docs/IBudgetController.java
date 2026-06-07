package com.andersonvianadev.finance_control_api.controllers.docs;

import com.andersonvianadev.finance_control_api.controllers.dtos.requests.BudgetRequestDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.requests.BudgetUpdateDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.BudgetResponseDTO;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@Tag(
        name = "Budgets",
        description = "Operations for budgets"
)
public interface IBudgetController {

    @Operation(
            summary = "Create budget",
            description = """
                    Creates a new spending budget linked to a category for the authenticated user.
                    Each user can have only one budget per category.
                    The category must belong to the user or be a global category.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Budget created successfully.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = BudgetResponseDTO.class),
                                    examples = @ExampleObject(
                                            name = "Budget created.",
                                            value = """
                                                    {
                                                        "user": {
                                                            "id": "9cb12d90-4623-41c2-b3fc-1a963f77bcf1",
                                                            "name": "Anderson",
                                                            "email": "anderson@gmail.com"
                                                        },
                                                        "category": {
                                                            "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                                            "name": "Food",
                                                            "description": "Grocery and restaurant expenses",
                                                            "icon": "food",
                                                            "owner": {
                                                                "id": "9cb12d90-4623-41c2-b3fc-1a963f77bcf1",
                                                                "name": "Anderson",
                                                                "email": "anderson@gmail.com"
                                                            }
                                                        },
                                                        "budgetType": "MONTHLY",
                                                        "limitAmount": 1500.00,
                                                        "active": true
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "A budget already exists for this category.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = StandardException.class),
                                    examples = @ExampleObject(
                                            name = "Budget already exists.",
                                            value = """
                                                    {
                                                        "timestamp": "2026-06-07T09:00:00Z",
                                                        "status": 409,
                                                        "error": "A budget for category Food already exists.",
                                                        "path": "/nix-finance-api/budgets"
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
                                                        "path": "/nix-finance-api/budgets"
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
                                            name = "Limit must be positive.",
                                            value = """
                                                    {
                                                        "timestamp": "2026-06-07T09:00:00Z",
                                                        "status": 400,
                                                        "error": "the limit value must be positive.",
                                                        "path": "/nix-finance-api/budgets"
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
                                                        "path": "/nix-finance-api/budgets"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<BudgetResponseDTO> save(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "user") User user,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = """
                            Data required to create the budget.
                            Validation rules:
                                - categoryId: required, must be a category owned by the user or global
                                - budgetType: required (MONTHLY or WEEKLY)
                                - limitAmount: required, must be a positive value
                            """,
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = BudgetRequestDTO.class),
                            examples = @ExampleObject(
                                    name = "Valid budget.",
                                    value = """
                                            {
                                                "categoryId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                                "budgetType": "MONTHLY",
                                                "limitAmount": 1500.00
                                            }
                                            """
                            )
                    )
            )
            @RequestBody @Valid BudgetRequestDTO request
    );

    @Operation(
            summary = "Find budget by ID",
            description = """
                    Returns a single budget by its UUID.
                    The budget must belong to the authenticated user.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Budget found successfully.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = BudgetResponseDTO.class),
                                    examples = @ExampleObject(
                                            name = "Budget found.",
                                            value = """
                                                    {
                                                        "user": {
                                                            "id": "9cb12d90-4623-41c2-b3fc-1a963f77bcf1",
                                                            "name": "Anderson",
                                                            "email": "anderson@gmail.com"
                                                        },
                                                        "category": {
                                                            "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                                            "name": "Food",
                                                            "description": "Grocery and restaurant expenses",
                                                            "icon": "food",
                                                            "owner": {
                                                                "id": "9cb12d90-4623-41c2-b3fc-1a963f77bcf1",
                                                                "name": "Anderson",
                                                                "email": "anderson@gmail.com"
                                                            }
                                                        },
                                                        "budgetType": "MONTHLY",
                                                        "limitAmount": 1500.00,
                                                        "active": true
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Budget not found.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = StandardException.class),
                                    examples = @ExampleObject(
                                            name = "Budget not found.",
                                            value = """
                                                    {
                                                        "timestamp": "2026-06-07T09:00:00Z",
                                                        "status": 404,
                                                        "error": "Budget with id 3fa85f64-5717-4562-b3fc-2c963f66afa6 not found",
                                                        "path": "/nix-finance-api/budgets/3fa85f64-5717-4562-b3fc-2c963f66afa6"
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
                                                        "path": "/nix-finance-api/budgets/3fa85f64-5717-4562-b3fc-2c963f66afa6"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<BudgetResponseDTO> findById(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "user") User user,
            @Parameter(description = "UUID of the budget to retrieve.", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID id
    );

    @Operation(
            summary = "Update budget",
            description = """
                    Updates the limit amount, budget type and/or active status of an existing budget.
                    Only budgets owned by the authenticated user can be updated.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Budget updated successfully.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = BudgetResponseDTO.class),
                                    examples = @ExampleObject(
                                            name = "Budget updated.",
                                            value = """
                                                    {
                                                        "user": {
                                                            "id": "9cb12d90-4623-41c2-b3fc-1a963f77bcf1",
                                                            "name": "Anderson",
                                                            "email": "anderson@gmail.com"
                                                        },
                                                        "category": {
                                                            "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                                            "name": "Food",
                                                            "description": "Grocery and restaurant expenses",
                                                            "icon": "food",
                                                            "owner": {
                                                                "id": "9cb12d90-4623-41c2-b3fc-1a963f77bcf1",
                                                                "name": "Anderson",
                                                                "email": "anderson@gmail.com"
                                                            }
                                                        },
                                                        "budgetType": "WEEKLY",
                                                        "limitAmount": 500.00,
                                                        "active": false
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Budget not found.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = StandardException.class),
                                    examples = @ExampleObject(
                                            name = "Budget not found.",
                                            value = """
                                                    {
                                                        "timestamp": "2026-06-07T09:00:00Z",
                                                        "status": 404,
                                                        "error": "Budget with id 3fa85f64-5717-4562-b3fc-2c963f66afa6 not found",
                                                        "path": "/nix-finance-api/budgets/3fa85f64-5717-4562-b3fc-2c963f66afa6"
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
                                            name = "Active field cannot be null.",
                                            value = """
                                                    {
                                                        "timestamp": "2026-06-07T09:00:00Z",
                                                        "status": 400,
                                                        "error": "active field cannot be null.",
                                                        "path": "/nix-finance-api/budgets/3fa85f64-5717-4562-b3fc-2c963f66afa6"
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
                                                        "path": "/nix-finance-api/budgets/3fa85f64-5717-4562-b3fc-2c963f66afa6"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<BudgetResponseDTO> update(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "user") User user,
            @Parameter(description = "UUID of the budget to update.", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = """
                            Fields to update. All fields are required.
                            Validation rules:
                                - limitAmount: required, must be a positive value
                                - budgetType: required (MONTHLY or WEEKLY)
                                - active: required
                            """,
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = BudgetUpdateDTO.class),
                            examples = @ExampleObject(
                                    name = "Valid update.",
                                    value = """
                                            {
                                                "limitAmount": 500.00,
                                                "budgetType": "WEEKLY",
                                                "active": false
                                            }
                                            """
                            )
                    )
            )
            @RequestBody @Valid BudgetUpdateDTO request
    );

    @Operation(
            summary = "Delete budget by ID",
            description = """
                    Permanently deletes a budget by its UUID.
                    Only budgets owned by the authenticated user can be deleted.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "Budget deleted successfully.",
                            content = @Content
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Budget not found.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = StandardException.class),
                                    examples = @ExampleObject(
                                            name = "Budget not found.",
                                            value = """
                                                    {
                                                        "timestamp": "2026-06-07T09:00:00Z",
                                                        "status": 404,
                                                        "error": "Budget with id 3fa85f64-5717-4562-b3fc-2c963f66afa6 not found",
                                                        "path": "/nix-finance-api/budgets/3fa85f64-5717-4562-b3fc-2c963f66afa6"
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
                                                        "path": "/nix-finance-api/budgets/3fa85f64-5717-4562-b3fc-2c963f66afa6"
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
            @Parameter(description = "UUID of the budget to delete.", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID id
    );

    @Operation(
            summary = "List budgets",
            description = """
                    Returns a paginated list of budgets belonging to the authenticated user.
                    Pagination query parameters:
                        - page: page number (0-indexed, default: 0)
                        - size: page size (default: 10)
                        - sort: sorting criteria (e.g. sort=limitAmount,asc)
                    """,
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Budgets retrieved successfully.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = PageResponseDTO.class),
                                    examples = @ExampleObject(
                                            name = "Budgets page.",
                                            value = """
                                                    {
                                                        "content": [
                                                            {
                                                                "user": {
                                                                    "id": "9cb12d90-4623-41c2-b3fc-1a963f77bcf1",
                                                                    "name": "Anderson",
                                                                    "email": "anderson@gmail.com"
                                                                },
                                                                "category": {
                                                                    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                                                    "name": "Food",
                                                                    "description": "Grocery and restaurant expenses",
                                                                    "icon": "food",
                                                                    "owner": {
                                                                        "id": "9cb12d90-4623-41c2-b3fc-1a963f77bcf1",
                                                                        "name": "Anderson",
                                                                        "email": "anderson@gmail.com"
                                                                    }
                                                                },
                                                                "budgetType": "MONTHLY",
                                                                "limitAmount": 1500.00,
                                                                "active": true
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
                                                        "path": "/nix-finance-api/budgets"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<PageResponseDTO<BudgetResponseDTO>> findAll(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "user") User user,
            @PageableDefault(size = 10) Pageable pageable
    );
}
