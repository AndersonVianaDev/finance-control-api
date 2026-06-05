package com.andersonvianadev.finance_control_api.controllers;

import com.andersonvianadev.finance_control_api.controllers.docs.IAuthController;
import com.andersonvianadev.finance_control_api.controllers.dtos.requests.LoginRequestDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.LoginResponseDTO;
import com.andersonvianadev.finance_control_api.infra.security.token.ITokenService;
import com.andersonvianadev.finance_control_api.infra.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/users")
@RequiredArgsConstructor
public class AuthController implements IAuthController {

    private final ITokenService tokenService;
    private final AuthenticationManager authenticationManager;

    @Override
    @PostMapping(value = "/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO request) {
        final var usernamePassword = new UsernamePasswordAuthenticationToken(request.email(), request.password());
        final UserPrincipal principal = (UserPrincipal) authenticationManager.authenticate(usernamePassword).getPrincipal();
        final String token = tokenService.generate(principal.getUser().getId());
        final LoginResponseDTO response = new LoginResponseDTO(token);

        return ResponseEntity.ok(response);
    }
}
