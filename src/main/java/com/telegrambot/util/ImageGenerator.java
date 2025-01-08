package com.telegrambot.util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import javax.imageio.ImageIO;

public class ImageGenerator {

    public static byte[] generateImage(String title, String subtitle, String details, String iconUrl) {
        int width = 800;
        int height = 600;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Заливаем фон
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, width, height);

        // Загружаем иконку
        try {
            BufferedImage icon = ImageIO.read(new URL(iconUrl));
            g2d.drawImage(icon, 50, 50, 100, 100, null);
        } catch (Exception e) {
            System.err.println("Ошибка загрузки иконки: " + e.getMessage());
        }

        g2d.setFont(new Font("Arial", Font.BOLD, 32));
        g2d.setColor(Color.WHITE);
        g2d.drawString(title, 200, 100);

        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        g2d.drawString(subtitle, 200, 150);

        g2d.setFont(new Font("Arial", Font.PLAIN, 18));
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.drawString(details, 200, 200);

        g2d.dispose();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка генерации изображения", e);
        }
    }
}


