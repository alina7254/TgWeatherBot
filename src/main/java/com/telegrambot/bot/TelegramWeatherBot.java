package com.telegrambot.bot;

import com.telegrambot.response.WeatherResponse;
import com.telegrambot.model.UserAction;
import com.telegrambot.repository.WeatherRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class TelegramWeatherBot extends TelegramLongPollingBot {

    private static final String WEATHER_COMMAND = "/weather";
    private static final String HELP_COMMAND = "/help";

    private final RestTemplate restTemplate;
    private final WeatherRepository weatherRepository;
    private final String botUsername;
    private final String botToken;
    private final String weatherApiKey;

    private final Map<String, String> countryCapitalMap;

    public TelegramWeatherBot(
            @Value("${telegram.bot.username}") String botUsername,
            @Value("${telegram.bot.token}") String botToken,
            @Value("${weather.api.key}") String weatherApiKey,
            RestTemplate restTemplate,
            WeatherRepository weatherRepository
    ) {
        this.botUsername = botUsername;
        this.botToken = botToken;
        this.weatherApiKey = weatherApiKey;
        this.restTemplate = restTemplate;
        this.weatherRepository = weatherRepository;
        this.countryCapitalMap = Map.of(
                "France", "Paris",
                "Germany", "Berlin",
                "Italy", "Rome",
                "Spain", "Madrid",
                "United Kingdom", "London",
                "United States", "Washington D.C.",
                "Canada", "Ottawa",
                "Australia", "Canberra",
                "Japan", "Tokyo",
                "Brazil", "Brasília"
        );
    }

    private String getCapitalByCountry(String country) {
        return countryCapitalMap.getOrDefault(country, "Unknown");
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            Long userId = update.getMessage().getFrom().getId();
            Long chatId = update.getMessage().getChatId();

            String response;

            if (messageText.startsWith(WEATHER_COMMAND)) {
                String[] parts = messageText.split(" ", 2);
                if (parts.length < 2) {
                    response = "Please provide a country name after /weather command.";
                } else {
                    String country = parts[1];
                    String capital = getCapitalByCountry(country);
                    if (capital != null) {
                        response = String.format("The capital of %s is %s. Fetching weather information...", country, capital);
                        response += "\n" + getWeather(capital);
                    } else {
                        response = "Country not found. Please try again.";
                    }
                }
            } else if (messageText.equals(HELP_COMMAND)) {
                response = getHelpMessage();
            } else {
                response = "Unknown command. Type /help to see available commands.";
            }

            sendMessage(chatId, response);
            logRequest(userId, messageText, response);
        }
    }



    private String getWeather(String city) {
        String url = String.format("https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=metric", city, weatherApiKey);
        try {
            WeatherResponse weatherResponse = restTemplate.getForObject(url, WeatherResponse.class);
            if (weatherResponse != null && weatherResponse.getMain() != null && !weatherResponse.getWeather().isEmpty()) {
                return String.format("Weather in %s:\nTemperature: %.2f°C\nFeels like: %.2f°C\nWeather: %s\nHumidity: %d%%\nWind speed: %.2f m/s",
                        city, weatherResponse.getMain().getTemp(),
                        weatherResponse.getMain().getFeels_like(),
                        weatherResponse.getWeather().get(0).getDescription(),
                        weatherResponse.getMain().getHumidity(),
                        weatherResponse.getWind().getSpeed());
            } else {
                return "Unable to fetch weather data. Please try again later.";
            }
        } catch (Exception e) {
            return "Error fetching weather data: " + e.getMessage();
        }
    }

    private String getHelpMessage() {
        return """
                Available commands:
                /weather [country] - Get the current weather for the capital city of the specified country.
                /help - Show available commands.
                """;
    }

    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void logRequest(Long userId, String command, String response) {
        UserAction log = new UserAction();
        log.setUserId(userId);
        log.setCommand(command);
        log.setRequestTime(LocalDateTime.now());
        log.setResponse(response);
        weatherRepository.save(log);
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
}


