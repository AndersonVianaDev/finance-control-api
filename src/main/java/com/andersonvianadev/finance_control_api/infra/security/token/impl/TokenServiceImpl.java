package com.andersonvianadev.finance_control_api.infra.security.token.impl;

import com.andersonvianadev.finance_control_api.infra.exceptions.TokenException;
import com.andersonvianadev.finance_control_api.infra.security.token.ITokenService;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class TokenServiceImpl implements ITokenService {
    @Value("${spring.security.secret}")
    private String secret;

    @Value("${spring.security.expiration}")
    private long expiration;

    @Override
    public String generate(UUID id) {
        try {
            return JWT.create()
                    .withIssuer("auth")
                    .withSubject(id.toString())
                    .withExpiresAt(Instant.now().plusMillis(expiration))
                    .sign(Algorithm.HMAC256(secret));
        } catch (JWTCreationException e) {
            throw new TokenException(e.toString());
        }
    }

    @Override
    public UUID extractId(String token) {
        try {
            String subject = JWT.require(Algorithm.HMAC256(secret))
                    .withIssuer("auth")
                    .build()
                    .verify(token)
                    .getSubject();
            return UUID.fromString(subject);
        } catch (Exception e) {
            throw new TokenException(e.toString());
        }
    }
}
