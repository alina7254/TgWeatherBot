package com.telegrambot.bot;

import com.telegrambot.response.WeatherResponse;
import com.telegrambot.util.ImageGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component
@EnableScheduling
public class TelegramWeatherBot extends TelegramWebhookBot {

    public static final Logger logger = LoggerFactory.getLogger(TelegramWeatherBot.class);

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
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        logger.info("Получено обновление через Webhook: {}", update);
        handleUpdate(update);
        return null;
    }

    public void handleUpdate(Update update) {
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

    private void sendGeneratedImageWithCaption(Long chatId, byte[] imageBytes, String imageFileName, String caption) {
        try {
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(chatId.toString());
            sendPhoto.setPhoto(new InputFile(new ByteArrayInputStream(imageBytes), imageFileName));
            sendPhoto.setCaption(caption);

            execute(sendPhoto);
        } catch (Exception e) {
            logger.error("Ошибка при отправке изображения: {}", e.getMessage(), e);
            sendMessage(chatId, "Ошибка при генерации или отправке изображения.");
        }
    }

    private void sendLunarInfo(Long chatId) {
        LocalDateTime now = LocalDateTime.now();
        int lunarDay = calculateLunarDay(now);
        String moonPhase = determineMoonPhase(lunarDay);
        String zodiacSign = calculateLunarZodiacSign(now);

        String phaseIconPath = getMoonPhaseIconPath(moonPhase);
        String zodiacIconPath = getZodiacIconPath(zodiacSign);

        String caption = String.format("Сегодня %d лунный день\nФаза Луны: %s\nЛуна в знаке зодиака: %s", lunarDay, moonPhase, zodiacSign);

        try {
            byte[] imageBytes = ImageGenerator.generateLunarImage(phaseIconPath, zodiacIconPath, "stars", String.valueOf(lunarDay), zodiacSign);
            sendGeneratedImageWithCaption(chatId, imageBytes, "lunar_info.png", caption);
        } catch (Exception e) {
            logger.error("Ошибка при генерации или отправке изображения", e);
            sendMessage(chatId, "Ошибка при отправке информации о луне.");
        }
    }

    private String getMoonPhaseIconPath(String moonPhase) {
        return switch (moonPhase.toLowerCase()) {
            case "новолуние" -> "moon/phases/icons8-new-moon-64.png";
            case "растущий серп" -> "moon/phases/icons8-waxing-crescent-64.png";
            case "первая четверть" -> "moon/phases/icons8-first-quarter-64.png";
            case "растущая луна" -> "moon/phases/icons8-waxing-gibbous-64.png";
            case "полнолуние" -> "moon/phases/icons8-full-moon-64.png";
            case "убывающая луна" -> "moon/phases/icons8-waning-gibbous-64.png";
            case "последняя четверть" -> "moon/phases/icons8-last-quarter-64.png";
            case "убывающий серп" -> "moon/phases/icons8-waning-crescent-64.png";
            default -> null;
        };
    }

    private String getZodiacIconPath(String zodiacSign) {
        return switch (zodiacSign.toLowerCase()) {
            case "овен" -> "moon/zodiac/icons8-aries-64.png";
            case "телец" -> "moon/zodiac/icons8-taurus-64.png";
            case "близнецы" -> "moon/zodiac/icons8-gemini-64.png";
            case "рак" -> "moon/zodiac/icons8-cancer-64.png";
            case "лев" -> "moon/zodiac/icons8-leo-64.png";
            case "дева" -> "moon/zodiac/icons8-virgo-64.png";
            case "весы" -> "moon/zodiac/icons8-libra-64.png";
            case "скорпион" -> "moon/zodiac/icons8-scorpio-64.png";
            case "стрелец" -> "moon/zodiac/icons8-sagittarius-64.png";
            case "козерог" -> "moon/zodiac/icons8-capricorn-64.png";
            case "водолей" -> "moon/zodiac/icons8-aquarius-64.png";
            case "рыбы" -> "moon/zodiac/icons8-pisces-64.png";
            default -> null;
        };
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
                        {"Овен",
                        "Телец",
                        "Близнецы",
                        "Рак",
                        "Лев",
                        "Дева",
                        "Весы",
                        "Скорпион",
                        "Стрелец",
                        "Козерог",
                        "Водолей",
                        "Рыбы"};
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

        try {
            WeatherResponse weatherResponse = restTemplate.getForObject(url, WeatherResponse.class);

            if (weatherResponse != null) {
                String iconCode = weatherResponse.getWeather().get(0).getIcon();
                String weatherIconUrl = String.format("https://openweathermap.org/img/wn/%s@2x.png", iconCode);
                String backgroundType = determineWeatherBackground(city, weatherResponse.getWeather().get(0).getDescription());

                String caption = String.format(
                        "Погода в городе: %s\nТемпература: %.1f°C\nОщущается как: %.1f°C\nВлажность: %d%%\nСкорость ветра: %.1f м/с\n%s",
                        weatherResponse.getName(),
                        weatherResponse.getMain().getTemp(),
                        weatherResponse.getMain().getFeelsLike(),
                        weatherResponse.getMain().getHumidity(),
                        weatherResponse.getWind().getSpeed(),
                        weatherResponse.getWeather().get(0).getDescription()
                );

                byte[] imageBytes = ImageGenerator.generateWeatherImageFromUrl(
                        city,
                        weatherIconUrl,
                        weatherResponse.getMain().getTemp(),
                        backgroundType
                );

                sendGeneratedImageWithCaption(chatId, imageBytes, "weather_info.png", caption);
            } else {
                sendMessage(chatId, "Не удалось получить данные о погоде для города: " + city);
            }
        } catch (Exception e) {
            logger.error("Ошибка получения данных о погоде для города", e);
            sendMessage(chatId, "Ошибка получения данных о погоде для города: " + city);
        }
    }


    private String determineWeatherBackground(String city, String description) {
        LocalTime now = LocalTime.now();
        if (now.isAfter(LocalTime.of(6, 0)) && now.isBefore(LocalTime.of(18, 0))) {
            return "day";
        } else {
            return "night";
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
            String gLevel = determineStormLevel(kpIndex);
            String description = determineStormDescription(gLevel);

            String caption = String.format(
                    "Геомагнитная буря\nУровень: %s\nОписание: %s\nИндекс Kp: %.1f",
                    gLevel, description, kpIndex
            );

            byte[] imageBytes = ImageGenerator.generateMagneticStormImage(gLevel, description);

            sendGeneratedImageWithCaption(chatId, imageBytes, "storm_info.png", caption);
        } catch (Exception e) {
            logger.error("Ошибка при получении данных о магнитных бурях", e);
            sendMessage(chatId, "Ошибка при получении данных о магнитных бурях.");
        }
    }


    private String determineStormLevel(double kpIndex) {
        if (kpIndex < 4) return "G1";
        if (kpIndex < 5) return "G2";
        if (kpIndex < 6) return "G3";
        if (kpIndex < 7) return "G4";
        if (kpIndex < 9) return "G5";
        return "extreme";
    }

    private String determineStormDescription(String gLevel) {
        return switch (gLevel) {
            case "G1" -> "Слабая буря";
            case "G2" -> "Средняя буря";
            case "G3" -> "Сильная буря";
            case "G4" -> "Очень сильная буря";
            case "G5" -> "Экстремально сильная буря";
            default -> "Неизвестный уровень";
        };
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

    public void setWebhook(String webhookUrl) {
        try {
            SetWebhook setWebhook = SetWebhook.builder().url(webhookUrl).build();
            execute(setWebhook);
            logger.info("Webhook успешно установлен на URL: {}", webhookUrl);
        } catch (TelegramApiException e) {
            logger.error("Ошибка при установке Webhook: {}", e.getMessage(), e);
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

    @Override
    public String getBotPath() {
        return "webhook/telegram";
    }
}








