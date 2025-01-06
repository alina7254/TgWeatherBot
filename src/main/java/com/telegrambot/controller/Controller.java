package com.telegrambot.controller;

import com.telegrambot.repository.WeatherRepository;
import com.telegrambot.response.LogRequest;
import com.telegrambot.model.UserAction;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/logs")
public class Controller {

    private final WeatherRepository weatherRepository;

    public Controller(WeatherRepository weatherRepository) {
        this.weatherRepository = weatherRepository;
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

    @PostMapping
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



