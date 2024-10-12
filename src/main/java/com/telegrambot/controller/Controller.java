package com.telegrambot.controller;

import com.telegrambot.repository.WeatherRepository;
import com.telegrambot.response.LogRequest;
import com.telegrambot.model.UserAction;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/logs")
public class Controller {
    private final WeatherRepository WeatherRepository;

    public Controller(WeatherRepository WeatherRepository) {
        this.WeatherRepository = WeatherRepository;
    }

    @GetMapping
    public List<UserAction> getAllLogs() {
        return WeatherRepository.findAll();
    }

    @GetMapping("/{userId}")
    public List<UserAction> getLogsByUser(@PathVariable Long userId) {
        return WeatherRepository.findByUserId(userId);
    }

    @PostMapping("/addLog")
    public UserAction addLog(@RequestBody LogRequest request) {
        UserAction log = new UserAction();
        log.setUserId(request.getUserId());
        log.setCommand(request.getCommand());
        log.setRequestTime(LocalDateTime.now());
        log.setResponse("Some response message");

        return WeatherRepository.save(log);
    }
}


