package com.andersonvianadev.finance_control_api.infra.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class CorrelationIdFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {
        HttpServletRequest req = (HttpServletRequest) request;
        String corrId = req.getHeader("X-Correlation-Id");
        if (corrId == null) {
            corrId = UUID.randomUUID().toString();
        }

        // Adiciona ao MDC para ser incluído nos logs
        MDC.put("correlationId", corrId);

        try {
            chain.doFilter(request, response);
        } finally {
            // Limpa o MDC para evitar vazamento de memória/contexto
            MDC.clear();
        }
    }
}
