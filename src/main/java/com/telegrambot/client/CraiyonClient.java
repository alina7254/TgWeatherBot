package com.telegrambot.client;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class CraiyonClient {

    private static final String CRAIYON_URL = "https://www.craiyon.com";

    public static String generateImage(String prompt) throws Exception {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--remote-debugging-port=9222");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-web-security");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--headless");
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.6778.204 Safari/537.36");


        WebDriver driver = new ChromeDriver(options);

        try {
            System.out.println("Открываем сайт...");
            driver.get(CRAIYON_URL);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
            WebElement inputField = driver.findElement(By.cssSelector("textarea[placeholder='What do you want to generate?']"));

            System.out.println("Поле ввода найдено!");

            inputField.sendKeys(prompt);
            System.out.println("Текст введен, нажимаем кнопку...");

            WebElement drawButton = driver.findElement(By.cssSelector("button[aria-label='Generate']"));
            drawButton.click();

            WebElement imageElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".generated-images img")));
            System.out.println("Изображение сгенерировано!");

            String imageUrl = imageElement.getAttribute("src");
            if (imageUrl == null || imageUrl.isEmpty()) {
                throw new RuntimeException("Не удалось получить изображение.");
            }

            return imageUrl;

        } catch (Exception e) {
            throw new RuntimeException("Ошибка генерации изображения через Selenium: " + e.getMessage(), e);
        } finally {
            driver.quit();
        }
    }
}





