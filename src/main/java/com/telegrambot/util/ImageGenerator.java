package com.telegrambot.util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import javax.imageio.ImageIO;
import java.net.URL;

public class ImageGenerator {

    public static byte[] generateLunarImage(String phaseIconPath, String zodiacIconPath, String backgroundType, String lunarDay, String zodiacSign) {
        int width = 800;
        int height = 600;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        if ("stars".equalsIgnoreCase(backgroundType)) {
            drawStarsBackground(g2d, width, height);
        }

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 52));
        g2d.drawString(lunarDay + " лунный день", 200, 100);
        g2d.drawString("Луна в знаке " + zodiacSign, 200, 500);

        try {
            InputStream phaseStream = ImageGenerator.class.getResourceAsStream("/" + phaseIconPath);
            if (phaseStream == null) {
                throw new IOException("Файл " + phaseIconPath + " не найден.");
            }
            BufferedImage phaseIcon = ImageIO.read(phaseStream);
            g2d.drawImage(phaseIcon, 300, 200, 200, 200, null);

            InputStream zodiacStream = ImageGenerator.class.getResourceAsStream("/" + zodiacIconPath);
            if (zodiacStream == null) {
                throw new IOException("Файл " + zodiacIconPath + " не найден.");
            }
            BufferedImage zodiacIcon = ImageIO.read(zodiacStream);
            g2d.drawImage(zodiacIcon, 400, 200, 100, 100, null);
        } catch (Exception e) {
            System.err.println("Ошибка загрузки иконок: " + e.getMessage());
        }

        g2d.dispose();

        return convertToBytes(image);
    }

    private static void drawStarsBackground(Graphics2D g2d, int width, int height) {
        g2d.setColor(new Color(10, 10, 50));
        g2d.fillRect(0, 0, width, height);

        g2d.setColor(Color.WHITE);
        Random random = new Random();
        for (int i = 0; i < 100; i++) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            int size = random.nextInt(3) + 1;
            g2d.fillOval(x, y, size, size);
        }
    }

    public static byte[] generateWeatherImageFromUrl(String cityName, String weatherIconUrl, double temperature, String backgroundType) {
        int width = 800;
        int height = 600;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        drawWeatherBackground(g2d, backgroundType, width, height);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 52));


        FontMetrics cityMetrics = g2d.getFontMetrics();
        int cityX = (width - cityMetrics.stringWidth(cityName)) / 2;
        int cityY = cityMetrics.getAscent() + 50;
        g2d.drawString(cityName, cityX, cityY);

        try {
            BufferedImage weatherIcon = ImageIO.read(new URL(weatherIconUrl));
            int iconWidth = 300;
            int iconHeight = 300;
            int iconX = (width - iconWidth) / 2;
            int iconY = (height - iconHeight) / 2;
            g2d.drawImage(weatherIcon, iconX, iconY, iconWidth, iconHeight, null);
        } catch (Exception e) {
            System.err.println("Ошибка загрузки иконки погоды: " + e.getMessage());
        }

        String temperatureText = String.format("%.1f°C", temperature);
        FontMetrics tempMetrics = g2d.getFontMetrics();
        int tempX = (width - tempMetrics.stringWidth(temperatureText)) / 2;
        int tempY = height - 50;
        g2d.drawString(temperatureText, tempX, tempY);

        g2d.dispose();

        return convertToBytes(image);

    }

    private static void drawWeatherBackground(Graphics2D g2d, String backgroundType, int width, int height) {
        if ("day".equalsIgnoreCase(backgroundType)) {
            g2d.setColor(new Color(135, 206, 250));
        } else if ("night".equalsIgnoreCase(backgroundType)) {
            g2d.setColor(new Color(25, 25, 112));
        } else {
            g2d.setColor(Color.GRAY);
        }
        g2d.fillRect(0, 0, width, height);
    }

    public static byte[] generateMagneticStormImage(String level, String description) {
        int width = 800;
        int height = 600;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, width, height);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 52));
        g2d.drawString(level + " — " + description, 150, 200);

        drawWaves(g2d, level, width, 300);

        g2d.dispose();

        return convertToBytes(image);
    }

    private static void drawWaves(Graphics2D g2d, String level, int width, int startY) {
        Color waveColor = switch (level) {
            case "G1" -> Color.GREEN;
            case "G2" -> Color.YELLOW;
            case "G3" -> Color.ORANGE;
            case "G4" -> Color.RED;
            case "G5" -> Color.MAGENTA;
            default -> Color.GRAY;
        };

        int waveCount = switch (level) {
            case "G1" -> 3;
            case "G2" -> 5;
            case "G3" -> 7;
            case "G4" -> 9;
            case "G5" -> 11;
            default -> 3;
        };

        int amplitude = 20;
        int waveSpacing = 40;

        g2d.setColor(waveColor);
        for (int i = 0; i < waveCount; i++) {
            int yOffset = startY + i * waveSpacing;
            drawSineWave(g2d, width, amplitude, yOffset);
            amplitude += 5;
        }
    }


    private static void drawSineWave(Graphics2D g2d, int width, int amplitude, int yOffset) {
        int waveLength = 200;
        int points = 500;

        int[] xPoints = new int[points];
        int[] yPoints = new int[points];

        for (int i = 0; i < points; i++) {
            xPoints[i] = i * width / points;
            yPoints[i] = (int) (yOffset + amplitude * Math.sin(2 * Math.PI * i / waveLength));
        }

        g2d.setStroke(new BasicStroke(3));
        g2d.drawPolyline(xPoints, yPoints, points);
    }



    private static byte[] convertToBytes(BufferedImage image) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Ошибка генерации изображения", e);
        }
    }
}



