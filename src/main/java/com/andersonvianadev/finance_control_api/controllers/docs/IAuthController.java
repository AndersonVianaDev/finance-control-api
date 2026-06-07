package com.andersonvianadev.finance_control_api.controllers.docs;

import com.andersonvianadev.finance_control_api.controllers.dtos.requests.LoginRequestDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.LoginResponseDTO;
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
        name = "Authentication",
        description = "Operations for authentication"
)
public interface IAuthController {

    @Operation(
            summary = "User login",
            description = """
                    Authenticates the user with email and password and returns a JWT token.
                    This endpoint has a stricter rate limit per IP to protect against brute-force attacks.
                    Responses include rate limit headers: X-Rate-Limit-Limit, X-Rate-Limit-Remaining and X-Rate-Limit-Reset.
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Login successful. Returns the JWT token.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = LoginResponseDTO.class),
                                    examples = @ExampleObject(
                                            name = "Login successful.",
                                            value = """
                                                    {
                                                        "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIzZmE4NWY2NC01NzE3LTQ1NjItYjNmYy0yYzk2M2Y2NmFmYTYiLCJpc3MiOiJhdXRoIiwiZXhwIjoxNzE3NTAwMDAwfQ.signature"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Invalid credentials.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = StandardException.class),
                                    examples = @ExampleObject(
                                            name = "Invalid credentials.",
                                            value = """
                                                    {
                                                        "timestamp": "2026-06-04T11:30:00Z",
                                                        "status": 401,
                                                        "error": "Username does not exist or password is invalid.",
                                                        "path": "/nix-finance-api/users/login"
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
                                                        "path": "/nix-finance-api/users/login"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Too many login attempts from the same IP.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = StandardException.class),
                                    examples = @ExampleObject(
                                            name = "Rate limit exceeded.",
                                            value = """
                                                    {
                                                        "timestamp": "2026-06-07T17:42:07Z",
                                                        "status": 429,
                                                        "error": "Too many requests. Please try again later.",
                                                        "path": "/nix-finance-api/users/login"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<LoginResponseDTO> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = """
                            Credentials required for authentication.
                            Validation rules:
                                - email: required and must be a valid email
                                - password: required
                            """,
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = LoginRequestDTO.class),
                            examples = @ExampleObject(
                                    name = "Valid credentials.",
                                    value = """
                                            {
                                                "email": "anderson@gmail.com",
                                                "password": "Arthur@1406"
                                            }
                                            """
                            )
                    )
            )
            @RequestBody @Valid LoginRequestDTO request
    );
}
