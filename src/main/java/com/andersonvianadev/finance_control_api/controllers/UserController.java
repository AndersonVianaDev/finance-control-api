package com.andersonvianadev.finance_control_api.controllers;

import com.andersonvianadev.finance_control_api.controllers.docs.IUserController;
import com.andersonvianadev.finance_control_api.controllers.dtos.requests.UserRequestDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.requests.UserUpdateDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.UserResponseDTO;
import com.andersonvianadev.finance_control_api.controllers.mappers.UserMapper;
import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.services.IUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController implements IUserController {

    private final IUserService service;

    @Override
    @PostMapping
    public ResponseEntity<UserResponseDTO> save(@RequestBody @Valid UserRequestDTO request) {
        User user = UserMapper.toDomain(request);
        user = service.save(user);

        UserResponseDTO response = UserMapper.toResponse(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @GetMapping
    public ResponseEntity<UserResponseDTO> findById(@AuthenticationPrincipal(expression = "user") User user) {
        UserResponseDTO response = UserMapper.toResponse(user);

        return ResponseEntity.ok(response);
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal(expression = "user") User user,
                                       @PathVariable UUID id) {
        service.deleteById(user, id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PutMapping
    public ResponseEntity<UserResponseDTO> update(@AuthenticationPrincipal(expression = "user") User user, @RequestBody @Valid UserUpdateDTO update) {
        user = UserMapper.toDomain(user, update);

        user = service.update(user);

        UserResponseDTO response = UserMapper.toResponse(user);

        return ResponseEntity.ok(response);
    }
}
