package com.andersonvianadev.finance_control_api.infra.security;

import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.infra.exceptions.NotFoundException;
import com.andersonvianadev.finance_control_api.infra.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerUserDetailsService implements UserDetailsService {

    private final UserRepository repository;

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        User user;
        try {
            UUID id = UUID.fromString(identifier);
            user = repository.findById(id)
                    .orElseThrow(() -> new NotFoundException(String.format("User with id %s not found", id)));
        } catch (IllegalArgumentException e) {
            user = repository.findByEmail(identifier)
                    .orElseThrow(() -> new NotFoundException(String.format("User with email %s not found", identifier)));
        }
        return new UserPrincipal(user);
    }
}
