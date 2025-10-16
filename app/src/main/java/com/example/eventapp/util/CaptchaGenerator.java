package com.example.eventapp.util;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.security.SecureRandom;
import java.util.Random;

public final class CaptchaGenerator {
    private static final String SYMBOLS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Random RANDOM = new SecureRandom();

    private CaptchaGenerator() {
    }

    public static CaptchaImage generate(int length, int width, int height) {
        String text = randomText(length);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, width, height);
        g2.setFont(new Font("SansSerif", Font.BOLD, height - 10));
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        for (int i = 0; i < 12; i++) {
            g2.setColor(randomColor(150, 255));
            double x1 = RANDOM.nextDouble(width);
            double y1 = RANDOM.nextDouble(height);
            double x2 = RANDOM.nextDouble(width);
            double y2 = RANDOM.nextDouble(height);
            g2.draw(new Line2D.Double(x1, y1, x2, y2));
        }

        int charWidth = width / (length + 2);
        for (int i = 0; i < text.length(); i++) {
            g2.setColor(randomColor(0, 150));
            double angle = Math.toRadians(RANDOM.nextInt(21) - 10);
            g2.rotate(angle, (i + 1) * charWidth, height / 2.0);
            g2.drawString(String.valueOf(text.charAt(i)), (i + 1) * charWidth, height - 10);
            g2.rotate(-angle, (i + 1) * charWidth, height / 2.0);
        }
        g2.dispose();
        return new CaptchaImage(text, image);
    }

    private static String randomText(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(SYMBOLS.charAt(RANDOM.nextInt(SYMBOLS.length())));
        }
        return builder.toString();
    }

    private static Color randomColor(int min, int max) {
        int r = min + RANDOM.nextInt(Math.max(1, max - min));
        int g = min + RANDOM.nextInt(Math.max(1, max - min));
        int b = min + RANDOM.nextInt(Math.max(1, max - min));
        return new Color(r, g, b);
    }

    public record CaptchaImage(String text, BufferedImage image) {
    }
}
