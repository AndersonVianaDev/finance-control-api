package com.andersonvianadev.finance_control_api.infra.security;

import com.andersonvianadev.finance_control_api.infra.security.token.ITokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Component
@RequiredArgsConstructor
public class SecurityFilter extends OncePerRequestFilter {

    private final CustomerUserDetailsService userDetailsService;
    private final ITokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        final String token = recoverToken(request);
        if (nonNull(token)) authenticate(token);
        filterChain.doFilter(request, response);
    }

    private void authenticate(String token) {
        UUID id = tokenService.extractId(token);
        UserDetails principal = userDetailsService.loadUserByUsername(id.toString());
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String recoverToken(HttpServletRequest request) {
        final String token = request.getHeader("Authorization");
        if (isNull(token) || !token.startsWith("Bearer ")) return null;
        return token.replace("Bearer ", "");
    }
}
