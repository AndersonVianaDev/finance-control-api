package com.andersonvianadev.finance_control_api.controllers;

import com.andersonvianadev.finance_control_api.controllers.docs.IUserController;
import com.andersonvianadev.finance_control_api.controllers.dtos.requests.UserRequestDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.UserResponseDTO;
import com.andersonvianadev.finance_control_api.controllers.mappers.UserMapper;
import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.services.IUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
