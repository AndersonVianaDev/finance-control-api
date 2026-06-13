package com.andersonvianadev.finance_control_api.infra.messaging.consumer;

import com.andersonvianadev.finance_control_api.domain.models.dtos.InstallmentGenerationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aws.enabled", havingValue = "true", matchIfMissing = true)
public class SqsInstallmentConsumer implements SmartLifecycle {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final InstallmentGenerationProcessor processor;

    @Value("${aws.sqs.queues.installment-generation}")
    private String queueUrl;

    private volatile boolean running = false;
    private ExecutorService executor;

    @Override
    public void start() {
        running = true;
        executor = Executors.newVirtualThreadPerTaskExecutor();
        executor.submit(this::pollLoop);
        log.info("SQS installment consumer started. Queue: {}", queueUrl);
    }

    @Override
    public void stop() {
        running = false;
        if (executor != null) {
            executor.shutdown();
        }
        log.info("SQS installment consumer stopped");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void pollLoop() {
        while (running) {
            try {
                List<Message> messages = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(10)
                        .waitTimeSeconds(20)
                        .build()).messages();

                for (Message message : messages) {
                    executor.submit(() -> processMessage(message));
                }
            } catch (Exception e) {
                log.error("Error polling SQS queue: {}", queueUrl, e);
                parkSafe(5_000);
            }
        }
    }

    private void processMessage(Message message) {
        try {
            InstallmentGenerationMessage payload = objectMapper.readValue(
                    message.body(), InstallmentGenerationMessage.class);
            processor.process(payload);
            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(message.receiptHandle())
                    .build());
        } catch (Exception e) {
            log.error("Failed to process SQS message id={}. Reason: {}", message.messageId(), e.getMessage(), e);
        }
    }

    private static void parkSafe(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
