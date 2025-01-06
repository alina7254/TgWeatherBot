package com.telegrambot.response;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotNull;

@Setter
@Getter
public class LogRequest {
    @NotNull(message = "User ID cannot be null")
    private Long userId;

    @NotNull(message = "Command cannot be null")
    private String command;
}



