package com.telegrambot.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
public class WeatherResponse {

    private Main main;
    private List<Weather> weather;
    private Wind wind;
    private Sys sys;
    private String name;

    @Setter
    @Getter
    @NoArgsConstructor
    public static class Main {
        private double temp;
        @JsonProperty("feels_like")
        private double feelsLike;
        private int humidity;
    }

    @Setter
    @Getter
    @NoArgsConstructor
    public static class Weather {
        private String main;
        private String description;
        private String icon;
    }

    @Setter
    @Getter
    @NoArgsConstructor
    public static class Wind {
        private double speed;
    }

    @Setter
    @Getter
    @NoArgsConstructor
    public static class Sys {
        @JsonProperty("country")
        private String country;
    }
}



