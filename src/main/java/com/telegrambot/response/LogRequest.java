package com.telegrambot.response;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LogRequest {
    private Long userId;
    private String command;

}


