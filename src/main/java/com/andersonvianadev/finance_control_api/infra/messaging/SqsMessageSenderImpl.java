package com.andersonvianadev.finance_control_api.infra.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aws.enabled", havingValue = "true", matchIfMissing = true)
public class SqsMessageSenderImpl implements ISqsMessageSender {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    @Override
    public <T> void send(String queueUrl, T body) {
        String messageBody = objectMapper.writeValueAsString(body);
        sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(messageBody)
                .build());
        log.info("Message sent to queue: {}", queueUrl);
    }
}
