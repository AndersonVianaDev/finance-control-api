package com.andersonvianadev.finance_control_api.controllers.docs;

import com.andersonvianadev.finance_control_api.controllers.dtos.requests.UserRequestDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.requests.UserUpdateDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.UserResponseDTO;
import com.andersonvianadev.finance_control_api.infra.exceptions.StandardException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@Tag(
        name = "Users",
        description = "Operations for users"
)
public interface IUserController {

    @Operation(
            summary = "Register user",
            description = "Register new users",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "User successfully registered.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = UserResponseDTO.class),
                                    examples = @ExampleObject(
                                            name = "User successfully registered.",
                                            value = """
                                                    {
                                                        "name": "Anderson",
                                                        "email": "anderson@gmail.com"
                                                    }
                                                    """
                                            )

                            )
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Error because a user with the same email already exists.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = StandardException.class),
                                    examples = @ExampleObject(
                                        name = "Email address already used.",
                                        value = """
                                                {
                                                    "timestamp": "2026-06-04T11:30:00Z",
                                                    "status": 409,
                                                    "error": "Email already exists",
                                                    "path": "/nix-finance-api/users"
                                                }
                                                """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Error because any mandatory field is blank or invalid field",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = StandardException.class),
                                    examples = @ExampleObject(
                                            name = "Email address already used.",
                                            value = """
                                                {
                                                    "timestamp": "2026-06-04T11:30:00Z",
                                                    "status": 400,
                                                    "error": "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character.",
                                                    "path": "/nix-finance-api/users"
                                                }
                                                """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<UserResponseDTO> save(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = """
                            Data required to register the user.
                            Validation rules:
                                - name: required
                                - email: required and must be a valid email
                                - password: required, 8-12 characters, at least one uppercase letter,
                                  one lowercase letter, one number and one special character
                        """,
                    required = true,
                    content = @Content(schema = @Schema(implementation = UserRequestDTO.class),
                    examples = @ExampleObject(
                            name = "Valid user",
                            value = """
                                    {
                                        "name": "Anderson",
                                        "email": "anderson@gmail.com",
                                        "password": "Arthur@1406"
                                    }
                                    """
                    ))
            )
            @RequestBody @Valid UserRequestDTO request
    );


    @Operation(
            summary = "Find user by ID",
            description = "Returns a single user by their UUID.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "User found successfully.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = UserResponseDTO.class),
                                    examples = @ExampleObject(
                                            name = "User found.",
                                            value = """
                                                    {
                                                        "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                                        "name": "Anderson",
                                                        "email": "anderson@gmail.com"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "User not found.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = StandardException.class),
                                    examples = @ExampleObject(
                                            name = "User not found.",
                                            value = """
                                                    {
                                                        "timestamp": "2026-06-04T11:30:00Z",
                                                        "status": 404,
                                                        "error": "User not found.",
                                                        "path": "/nix-finance-api/users/3fa85f64-5717-4562-b3fc-2c963f66afa6"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<UserResponseDTO> findById(
            @Parameter(description = "UUID of the user to retrieve.", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID id
    );

    @Operation(
            summary = "Delete user by ID",
            description = "Permanently deletes a user by their UUID.",
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "User deleted successfully.",
                            content = @Content
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "User not found.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = StandardException.class),
                                    examples = @ExampleObject(
                                            name = "User not found.",
                                            value = """
                                                    {
                                                        "timestamp": "2026-06-04T11:30:00Z",
                                                        "status": 404,
                                                        "error": "User not found.",
                                                        "path": "/nix-finance-api/users/3fa85f64-5717-4562-b3fc-2c963f66afa6"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<Void> delete(
            @Parameter(description = "UUID of the user to delete.", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID id
    );

    @Operation(
            summary = "Update user",
            description = "Updates the name and/or email of an existing user.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "User updated successfully.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = UserResponseDTO.class),
                                    examples = @ExampleObject(
                                            name = "User updated.",
                                            value = """
                                                    {
                                                        "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                                        "name": "Anderson Viana",
                                                        "email": "anderson.viana@gmail.com"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "User not found.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = StandardException.class),
                                    examples = @ExampleObject(
                                            name = "User not found.",
                                            value = """
                                                    {
                                                        "timestamp": "2026-06-04T11:30:00Z",
                                                        "status": 404,
                                                        "error": "User not found.",
                                                        "path": "/nix-finance-api/users/3fa85f64-5717-4562-b3fc-2c963f66afa6"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Email already in use by another user.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = StandardException.class),
                                    examples = @ExampleObject(
                                            name = "Email already used.",
                                            value = """
                                                    {
                                                        "timestamp": "2026-06-04T11:30:00Z",
                                                        "status": 409,
                                                        "error": "Email already exists",
                                                        "path": "/nix-finance-api/users/3fa85f64-5717-4562-b3fc-2c963f66afa6"
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
                                            name = "Invalid email format.",
                                            value = """
                                                    {
                                                        "timestamp": "2026-06-04T11:30:00Z",
                                                        "status": 400,
                                                        "error": "email format is not valid.",
                                                        "path": "/nix-finance-api/users/3fa85f64-5717-4562-b3fc-2c963f66afa6"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<UserResponseDTO> update(
            @Parameter(description = "UUID of the user to update.", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Fields to update. All fields are optional — only the provided ones will be changed.",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = UserUpdateDTO.class),
                            examples = @ExampleObject(
                                    name = "Valid update.",
                                    value = """
                                            {
                                                "name": "Anderson Viana",
                                                "email": "anderson.viana@gmail.com"
                                            }
                                            """
                            )
                    )
            )
            @RequestBody @Valid UserUpdateDTO update
    );
}
