package com.telegrambot;

import com.telegrambot.bot.TelegramWeatherBot;
import org.modelmapper.ModelMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableScheduling
@EnableJpaRepositories("com.telegrambot")
@EntityScan("com.telegrambot")
public class TelegramWeatherBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(TelegramWeatherBotApplication.class, args);
    }

    @Bean
    public CommandLineRunner run(TelegramWeatherBot bot) {
        return args -> {
            String webhookUrl = "https://grove-geode-beret.glitch.me/webhook/telegram";
            bot.setWebhook(webhookUrl);
        };
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }
}



