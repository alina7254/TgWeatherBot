package com.telegrambot.bot;

import com.telegrambot.client.CraiyonClient;
import com.telegrambot.response.WeatherResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@EnableScheduling
public class TelegramWeatherBot extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(TelegramWeatherBot.class);

    private static final String START_COMMAND = "/start";
    private static final String WEATHER_COMMAND = "Погода";
    private static final String SET_CITY_COMMAND = "Ввести город";
    private static final String MAGNETIC_STORMS_COMMAND = "Магнитные бури";
    private static final String LUNAR_INFO_COMMAND = "Лунные сутки";


    private final String botUsername;
    private final String botToken;
    private final String weatherApiKey;
    private final RestTemplate restTemplate;
    private final Map<Long, String> userCities = new HashMap<>();

    public TelegramWeatherBot(
            @Value("${telegram.bot.username}") String botUsername,
            @Value("${telegram.bot.token}") String botToken,
            @Value("${weather.api.key}") String weatherApiKey
    ) {
        this.botUsername = botUsername;
        this.botToken = botToken;
        this.weatherApiKey = weatherApiKey;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public void onUpdateReceived(Update update) {
        logger.info("Получено обновление: {}", update);

        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();

            logger.info("Получено сообщение от пользователя (chatId: {}): {}", chatId, messageText);

            switch (messageText) {
                case START_COMMAND, "Назад" -> sendMainMenu(chatId);
                case WEATHER_COMMAND -> sendWeatherMenu(chatId);
                case SET_CITY_COMMAND -> sendMessage(chatId, "Введите город для прогноза погоды:");
                case MAGNETIC_STORMS_COMMAND -> sendMagneticStormsInfo(chatId);
                case LUNAR_INFO_COMMAND -> sendLunarInfo(chatId);
                default -> {
                    userCities.put(chatId, messageText);
                    handleWeatherRequest(chatId, messageText);
                }
            }
        }
    }

    private void sendMainMenu(Long chatId) {
        logger.info("Отправка главного меню пользователю с chatId: {}", chatId);
        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId.toString());
            message.setText("Выберите действие:");

            ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
            keyboardMarkup.setResizeKeyboard(true);

            List<KeyboardRow> keyboard = new ArrayList<>();
            KeyboardRow row = new KeyboardRow();
            row.add(new KeyboardButton(WEATHER_COMMAND));
            row.add(new KeyboardButton(MAGNETIC_STORMS_COMMAND));
            row.add(new KeyboardButton(LUNAR_INFO_COMMAND));

            keyboard.add(row);
            keyboardMarkup.setKeyboard(keyboard);
            message.setReplyMarkup(keyboardMarkup);

            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Ошибка при отправке главного меню", e);
        }
    }

    private void sendImageWithCaption(Long chatId, String imageUrl, String caption) {
        try {
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(chatId.toString());
            sendPhoto.setPhoto(new InputFile(imageUrl));
            sendPhoto.setCaption(caption);
            execute(sendPhoto);
        } catch (TelegramApiException e) {
            logger.error("Ошибка при отправке изображения", e);
            sendMessage(chatId, "Ошибка при отправке изображения. Вот текстовые данные:\n" + caption);
        }
    }


    private void sendLunarInfo(Long chatId) {
        LocalDateTime now = LocalDateTime.now();
        int lunarDay = calculateLunarDay(now);
        String moonPhase = determineMoonPhase(lunarDay);
        String zodiacSign = calculateLunarZodiacSign(now);

        String message = String.format(
                "Сегодня %d лунный день\nФаза Луны: %s\nЛуна в знаке зодиака: %s",
                lunarDay, moonPhase, zodiacSign
        );

        String prompt = String.format(
                "Decoupage Art, Drawing. Today is lunar day %d with a %s moon, currently in the zodiac sign of %s.",
                lunarDay, moonPhase, zodiacSign
        );

        String imageUrl;
        try {
            imageUrl = CraiyonClient.generateImage(prompt);
        } catch (Exception e) {
            logger.error("Ошибка генерации изображения через Craiyon", e);
            sendMessage(chatId, "\n" + message);
            return;
        }

        sendImageWithCaption(chatId, imageUrl, message);
    }


    private int calculateLunarDay(LocalDateTime now) {
        LocalDateTime newMoon = LocalDateTime.of(2024, 12, 30, 9, 28);

        Duration duration = Duration.between(newMoon, now);
        double daysSinceNewMoon = duration.toMinutes() / (60.0 * 24.0);

        int lunarDay = (int) (Math.floor(daysSinceNewMoon) % 29) + 1;

        return lunarDay;
    }

    private String determineMoonPhase(int lunarDay) {
        if (lunarDay == 1) {
            return "Новолуние";
        } else if (lunarDay <= 7) {
            return "Растущий серп";
        } else if (lunarDay == 8) {
            return "Первая четверть";
        } else if (lunarDay <= 15) {
            return "Растущая Луна";
        } else if (lunarDay == 16) {
            return "Полнолуние";
        } else if (lunarDay <= 22) {
            return "Убывающая Луна";
        } else if (lunarDay == 23) {
            return "Последняя четверть";
        } else {
            return "Убывающий серп";
        }
    }

    private String calculateLunarZodiacSign(LocalDateTime now) {
        double longitude = calculateLunarLongitude(now);
        String[] zodiacSigns =
                        {"Овен ♈",
                        "Телец ♉",
                        "Близнецы ♊",
                        "Рак ♋",
                        "Лев ♌",
                        "Дева ♍",
                        "Весы ♎",
                        "Скорпион ♏",
                        "Стрелец ♐",
                        "Козерог ♑",
                        "Водолей ♒",
                        "Рыбы ♓"};
        int index = (int) (longitude / 29) % 12;
        return zodiacSigns[index];
    }

    private double calculateLunarLongitude(LocalDateTime now) {
        long daysSinceEpoch = java.time.Duration.between
                (LocalDateTime.of(2000, 1, 1, 0, 0), now).toDays();
        return (218.316 + 13.176396 * daysSinceEpoch) % 360;
    }


    private void sendWeatherMenu(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Выберите действие:");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton(SET_CITY_COMMAND));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("Назад"));

        keyboard.add(row1);
        keyboard.add(row2);
        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleWeatherRequest(Long chatId, String city) {
        String url = String.format(
                "https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=metric&lang=ru",
                city, weatherApiKey
        );
        logger.info("Запрос погоды для города: {}", city);
        try {
            WeatherResponse weatherResponse = restTemplate.getForObject(url, WeatherResponse.class);

            if (weatherResponse != null) {
                String response = String.format(
                        "Погода в городе %s:\nТемпература: %.1f°C\nОщущается как: %.1f°C\nВлажность: %d%%\nСкорость ветра: %.1f м/с\nОписание: %s",
                        weatherResponse.getName(),
                        weatherResponse.getMain().getTemp(),
                        weatherResponse.getMain().getFeelsLike(),
                        weatherResponse.getMain().getHumidity(),
                        weatherResponse.getWind().getSpeed(),
                        weatherResponse.getWeather().get(0).getDescription()
                );

                String prompt = String.format(
                        "Decoupage Art, Drawing. The weather is %s with a temperature of %.1f°C (feels like %.1f°C), " +
                                "humidity at %d%%, and wind speed %.1f m/s.",
                        weatherResponse.getWeather().get(0).getDescription(),
                        weatherResponse.getMain().getTemp(),
                        weatherResponse.getMain().getFeelsLike(),
                        weatherResponse.getMain().getHumidity(),
                        weatherResponse.getWind().getSpeed()
                );

                String imageUrl;
                try {
                    imageUrl = CraiyonClient.generateImage(prompt);
                } catch (Exception e) {
                    logger.error("Ошибка генерации изображения через Craiyon", e);
                    sendMessage(chatId, "\n" + response);
                    return;
                }

                sendImageWithCaption(chatId, imageUrl, response);
            } else {
                sendMessage(chatId, "Не удалось получить данные о погоде для города: " + city);
            }
        } catch (Exception e) {
            sendMessage(chatId, "Ошибка получения данных о погоде для города: " + city);
        }
    }


    private void sendMagneticStormsInfo(Long chatId) {
        String url = "https://services.swpc.noaa.gov/json/planetary_k_index_1m.json";

        try {
            ResponseEntity<List<Map<String, Object>>> response =
                    restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});

            List<Map<String, Object>> data = response.getBody();

            if (data == null || data.isEmpty()) {
                sendMessage(chatId, "Данные о магнитных бурях недоступны.");
                return;
            }

            Map<String, Object> latestData = data.get(data.size() - 1);
            Object kpIndexObj = latestData.get("kp_index");

            if (kpIndexObj == null) {
                sendMessage(chatId, "Данные о магнитных бурях отсутствуют.");
                return;
            }

            double kpIndex = Double.parseDouble(kpIndexObj.toString());
            String gLevel;
            String description;

            if (kpIndex < 5) {
                gLevel = "G1";
                description = "Слабая буря";
            } else if (kpIndex < 6) {
                gLevel = "G2";
                description = "Средняя буря";
            } else if (kpIndex < 7) {
                gLevel = "G3";
                description = "Сильная буря";
            } else if (kpIndex < 9) {
                gLevel = "G4";
                description = "Очень сильная буря";
            } else {
                gLevel = "G5";
                description = "Экстремально сильная буря";
            }

            String message = String.format(
                    "Уровень геомагнитной бури: %s (%s)\nИндекс Kp: %.1f",
                    gLevel, description, kpIndex
            );

            String prompt = String.format(
                    "Decoupage Art, Drawing. The magnetic storm level is %s (%s) with a Kp index of %.1f.",
                    gLevel, description, kpIndex
            );

            String imageUrl;
            try {
                imageUrl = CraiyonClient.generateImage(prompt);
            } catch (Exception e) {
                logger.error("Ошибка генерации изображения через Craiyon", e);
                sendMessage(chatId, "\n" + message);
                return;
            }

            sendImageWithCaption(chatId, imageUrl, message);

        } catch (Exception e) {
            sendMessage(chatId, "Ошибка при получении данных о магнитных бурях.");
        }
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

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
}








