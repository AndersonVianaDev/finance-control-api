package com.andersonvianadev.finance_control_api.controllers.docs;

import com.andersonvianadev.finance_control_api.controllers.dtos.requests.CategoryRequestDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.CategoryResponseDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.UserResponseDTO;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@Tag(
        name = "Categories",
        description = "Operations for categories"
)
public interface ICategoryController {

    @Operation(
            summary = "Create category",
            description = """
                    Creates a new category linked to the authenticated user.
                    Users on the free plan are limited to a maximum number of categories.
                    Category names must be unique per user.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Category created successfully.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = CategoryResponseDTO.class),
                                    examples = @ExampleObject(
                                            name = "Category created.",
                                            value = """
                                                    {
                                                        "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                                        "name": "Food",
                                                        "description": "Grocery and restaurant expenses",
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
                            description = "A category with the same name already exists for this user.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = StandardException.class),
                                    examples = @ExampleObject(
                                            name = "Category name already exists.",
                                            value = """
                                                    {
                                                        "timestamp": "2026-06-06T09:00:00Z",
                                                        "status": 409,
                                                        "error": "A category with this name Food already exists.",
                                                        "path": "/nix-finance-api/categories"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Category limit reached for the free plan.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = StandardException.class),
                                    examples = @ExampleObject(
                                            name = "Free plan limit exceeded.",
                                            value = """
                                                    {
                                                        "timestamp": "2026-06-06T09:00:00Z",
                                                        "status": 429,
                                                        "error": "You have reached the maximum category limit for the free plan.",
                                                        "path": "/nix-finance-api/categories"
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
                                            name = "Name exceeds maximum length.",
                                            value = """
                                                    {
                                                        "timestamp": "2026-06-06T09:00:00Z",
                                                        "status": 400,
                                                        "error": "Name field must contain at most 30 characters.",
                                                        "path": "/nix-finance-api/categories"
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
                                                        "timestamp": "2026-06-06T09:00:00Z",
                                                        "status": 401,
                                                        "error": "Unauthorized",
                                                        "path": "/nix-finance-api/categories"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<CategoryResponseDTO> save(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "user") User user,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = """
                            Data required to create the category.
                            Validation rules:
                                - name: required, max 30 characters, unique per user
                                - description: optional, max 50 characters
                            """,
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = CategoryRequestDTO.class),
                            examples = @ExampleObject(
                                    name = "Valid category.",
                                    value = """
                                            {
                                                "name": "Food",
                                                "description": "Grocery and restaurant expenses"
                                            }
                                            """
                            )
                    )
            )
            @RequestBody @Valid CategoryRequestDTO request
    );

    @Operation(
            summary = "Find category by ID",
            description = "Returns a single category by their UUID.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Category found successfully.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = UserResponseDTO.class),
                                    examples = @ExampleObject(
                                            name = "User found.",
                                            value = """
                                                    {
                                                        "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                                        "name": "Food",
                                                        "description": "Grocery and restaurant expenses",
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
                            description = "Category not found.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = StandardException.class),
                                    examples = @ExampleObject(
                                            name = "Category not found.",
                                            value = """
                                                    {
                                                        "timestamp": "2026-06-04T11:30:00Z",
                                                        "status": 404,
                                                        "error": "Category not found.",
                                                        "path": "/nix-finance-api/category/3fa85f64-5717-4562-b3fc-2c963f66afa6"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<CategoryResponseDTO> findById(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "user") User user,
            @Parameter(description = "UUID of the category to retrieve.", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID id
    );
}
