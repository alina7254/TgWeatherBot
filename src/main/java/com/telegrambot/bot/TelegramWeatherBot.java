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
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@EnableScheduling
public class TelegramWeatherBot extends TelegramLongPollingBot {

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

    private void sendGeneratedImageWithCaption(Long chatId, String title, String subtitle, String details, String iconUrl) {
        try {
            // Генерация изображения с использованием универсального ImageGenerator
            byte[] imageBytes = ImageGenerator.generateImage(title, subtitle, details, iconUrl);

            // Подготовка сообщения с изображением
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(chatId.toString());
            sendPhoto.setPhoto(new InputFile(new ByteArrayInputStream(imageBytes), "generated.png"));
            sendPhoto.setCaption(String.format("%s\n%s\n%s", title, subtitle, details));

            // Отправка изображения
            execute(sendPhoto);
        } catch (Exception e) {
            logger.error("Ошибка при отправке изображения: {}", e.getMessage(), e);
            // Отправка текстового сообщения в случае ошибки
            sendMessage(chatId, "Ошибка при генерации или отправке изображения. Вот текстовые данные:\n" +
                    String.format("%s\n%s\n%s", title, subtitle, details));
        }
    }


    private void sendLunarInfo(Long chatId) {
        LocalDateTime now = LocalDateTime.now();
        int lunarDay = calculateLunarDay(now);
        String moonPhase = determineMoonPhase(lunarDay);
        String zodiacSign = calculateLunarZodiacSign(now);

        String title = String.format("Сегодня %d лунный день", lunarDay);
        String subtitle = String.format("Фаза Луны: %s", moonPhase);
        String details = String.format("Луна в знаке зодиака: %s", zodiacSign);

        String iconUrl = getMoonPhaseIconUrl(moonPhase);

        try {
            sendGeneratedImageWithCaption(chatId, title, subtitle, details, iconUrl);
        } catch (Exception e) {
            logger.error("Ошибка при генерации изображения", e);
            sendMessage(chatId, title + "\n" + subtitle + "\n" + details);
        }
    }

    private String getMoonPhaseIconUrl(String moonPhase) {
        Map<String, String> moonPhaseIcons = Map.of(
                "Новолуние", "https://openweathermap.org/img/wn/01n.png",
                "Растущий серп", "https://cdn-icons-png.flaticon.com/512/4149/4149709.png",
                "Первая четверть", "https://cdn-icons-png.flaticon.com/512/869/869869.png",
                "Растущая Луна", "https://cdn-icons-png.flaticon.com/512/4149/4149712.png",
                "Полнолуние", "https://openweathermap.org/img/wn/01d.png",
                "Убывающая Луна", "https://cdn-icons-png.flaticon.com/512/4149/4149710.png",
                "Последняя четверть", "https://cdn-icons-png.flaticon.com/512/869/869872.png",
                "Убывающий серп", "https://cdn-icons-png.flaticon.com/512/4149/4149711.png"
        );

        return moonPhaseIcons.getOrDefault(moonPhase, "https://cdn-icons-png.flaticon.com/512/4149/4149720.png");
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
                String title = "Погода в городе " + weatherResponse.getName();
                String subtitle = String.format("Температура: %.1f°C, Ощущается как: %.1f°C",
                        weatherResponse.getMain().getTemp(), weatherResponse.getMain().getFeelsLike());
                String details = String.format("Влажность: %d%%, Скорость ветра: %.1f м/с, %s",
                        weatherResponse.getMain().getHumidity(),
                        weatherResponse.getWind().getSpeed(),
                        weatherResponse.getWeather().get(0).getDescription());

                String iconCode = weatherResponse.getWeather().get(0).getIcon();
                String iconUrl = String.format("https://openweathermap.org/img/wn/%s@2x.png", iconCode);

                sendGeneratedImageWithCaption(chatId, title, subtitle, details, iconUrl);
            } else {
                sendMessage(chatId, "Не удалось получить данные о погоде для города: " + city);
            }
        } catch (Exception e) {
            logger.error("Ошибка получения данных о погоде для города", e);
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

            String title = String.format("Уровень геомагнитной бури: %s", gLevel);
            String subtitle = String.format("Индекс Kp: %.1f", kpIndex);
            String details = String.format("Описание: %s", description);

            String iconUrl = getMagneticStormIconUrl(gLevel);

            sendGeneratedImageWithCaption(chatId, title, subtitle, details, iconUrl);
        } catch (Exception e) {
            logger.error("Ошибка при получении данных о магнитных бурях", e);
            sendMessage(chatId, "Ошибка при получении данных о магнитных бурях.");
        }
    }

    private String getMagneticStormIconUrl(String gLevel) {
        Map<String, String> stormIcons = Map.of(
                "G1", "https://cdn-icons-png.flaticon.com/512/6331/6331940.png",
                "G2", "https://cdn-icons-png.flaticon.com/512/6331/6331942.png",
                "G3", "https://cdn-icons-png.flaticon.com/512/6331/6331943.png",
                "G4", "https://cdn-icons-png.flaticon.com/512/6331/6331945.png",
                "G5", "https://cdn-icons-png.flaticon.com/512/6331/6331947.png"
        );

        return stormIcons.getOrDefault(gLevel, "https://cdn-icons-png.flaticon.com/512/6331/6331939.png");
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








