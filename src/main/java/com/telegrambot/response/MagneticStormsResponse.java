package com.telegrambot.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MagneticStormsResponse {
    private double kp_index;
    private String time_tag;
}

