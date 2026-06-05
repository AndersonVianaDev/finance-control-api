package com.andersonvianadev.finance_control_api.infra.security.token;

import java.util.UUID;

public interface ITokenService {
    String generate(UUID id);
    UUID extractId(String token);
}
