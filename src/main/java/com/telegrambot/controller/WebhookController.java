package com.telegrambot.controller;

import com.telegrambot.bot.TelegramWeatherBot;
import com.telegrambot.repository.WeatherRepository;
import com.telegrambot.response.LogRequest;
import com.telegrambot.model.UserAction;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDateTime;
import java.util.List;

import static com.telegrambot.bot.TelegramWeatherBot.logger;

@RestController
@RequestMapping("/webhook/telegram")
public class WebhookController {

    private final TelegramWeatherBot bot;
    private final WeatherRepository weatherRepository;

    public WebhookController(TelegramWeatherBot bot, WeatherRepository weatherRepository) {
        this.bot = bot;
        this.weatherRepository = weatherRepository;
    }

    @PostMapping
    public ResponseEntity<Void> handleUpdate(@RequestBody Update update) {
        logger.info("Получено обновление: {}", update);
        bot.handleUpdate(update);
        return ResponseEntity.ok().build();
    }


    @GetMapping
    public ResponseEntity<List<UserAction>> getAllLogs() {
        List<UserAction> logs = weatherRepository.findAll();
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<UserAction>> getLogsByUser(@PathVariable Long userId) {
        List<UserAction> logs = weatherRepository.findByUserId(userId);
        if (logs.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(logs);
    }

    @PostMapping("/log")
    public ResponseEntity<UserAction> addLog(@RequestBody LogRequest request) {
        UserAction log = new UserAction();
        log.setUserId(request.getUserId());
        log.setCommand(request.getCommand());
        log.setRequestTime(LocalDateTime.now());
        log.setResponse("Запрос обработан успешно");

        UserAction savedLog = weatherRepository.save(log);
        return ResponseEntity.ok(savedLog);
    }
}



