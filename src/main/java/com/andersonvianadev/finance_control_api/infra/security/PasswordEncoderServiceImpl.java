package com.andersonvianadev.finance_control_api.infra.security;

import com.andersonvianadev.finance_control_api.domain.services.IPasswordEncoderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PasswordEncoderServiceImpl implements IPasswordEncoderService {

    private final PasswordEncoder passwordEncoder;

    @Override
    public String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
}
