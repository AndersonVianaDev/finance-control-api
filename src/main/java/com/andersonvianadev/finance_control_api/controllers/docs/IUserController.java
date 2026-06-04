package com.andersonvianadev.finance_control_api.controllers.docs;

import com.andersonvianadev.finance_control_api.controllers.dtos.requests.UserRequestDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.UserResponseDTO;
import com.andersonvianadev.finance_control_api.infra.exceptions.StandardException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

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
}
