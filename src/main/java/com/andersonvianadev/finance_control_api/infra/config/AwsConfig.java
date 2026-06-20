package com.andersonvianadev.finance_control_api.infra.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.net.URI;

@Configuration
public class AwsConfig {

    @Bean
    @ConditionalOnProperty(name = "aws.enabled", havingValue = "true", matchIfMissing = true)
    public SqsClient sqsClient(
            @Value("${aws.region:us-east-1}") String region,
            @Value("${aws.access-key:test}") String accessKey,
            @Value("${aws.secret-key:test}") String secretKey,
            @Value("${aws.endpoint-override:}") String endpointOverride) {

        var builder = SqsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ));

        if (StringUtils.hasText(endpointOverride)) {
            builder.endpointOverride(URI.create(endpointOverride));
        }

        return builder.build();
    }
}
