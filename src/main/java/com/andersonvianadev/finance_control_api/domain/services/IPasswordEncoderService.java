package com.andersonvianadev.finance_control_api.domain.services;

public interface IPasswordEncoderService {

    String encode(String rawPassword);
}
