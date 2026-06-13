package com.andersonvianadev.finance_control_api.config;

import com.andersonvianadev.finance_control_api.infra.messaging.ISqsMessageSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestSqsConfig {

    @Bean
    @ConditionalOnProperty(name = "aws.enabled", havingValue = "false")
    public ISqsMessageSender noOpSqsMessageSender() {
        return new ISqsMessageSender() {
            @Override
            public <T> void send(String queueUrl, T body) {
                // no-op for tests
            }
        };
    }
}
