package com.andersonvianadev.finance_control_api.infra.messaging;

public interface ISqsMessageSender {

    <T> void send(String queueUrl, T body);
}
