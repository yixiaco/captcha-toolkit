package com.example.captcha.util;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * 默认背景图生成：随机卡通风景（天空 / 太阳 / 云 / 山 / 草地 / 树）。
 * 当滑块缺少底图素材时使用。
 */
public final class SceneBackground {

    private static final Random RANDOM = new Random();

    private SceneBackground() {
    }

    public static BufferedImage create(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        draw(image.createGraphics(), width, height);
        return image;
    }

    public static void draw(Graphics2D g, int width, int height) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int hue = RANDOM.nextInt(360);
        g.setPaint(new GradientPaint(0, 0, Color.getHSBColor(hue / 360f, 0.65f, 0.78f),
                0, height, Color.getHSBColor(((hue + 55) % 360) / 360f, 0.65f, 0.94f)));
        g.fillRect(0, 0, width, height);

        // 太阳
        int sunX = (int) (width * (0.18 + RANDOM.nextDouble() * 0.64));
        int sunY = (int) (height * (0.1 + RANDOM.nextDouble() * 0.22));
        int sunR = 13 + RANDOM.nextInt(12);
        g.setColor(new Color(255, 244, 179));
        g.fillOval(sunX - sunR, sunY - sunR, sunR * 2, sunR * 2);

        // 云
        g.setColor(new Color(255, 255, 255, 150));
        for (int i = 0; i < 4; i++) {
            int cx = (int) (width * (0.08 + RANDOM.nextDouble() * 0.84));
            int cy = (int) (height * (0.08 + RANDOM.nextDouble() * 0.34));
            int r = 9 + RANDOM.nextInt(8);
            g.fillOval(cx - r, cy - r, r * 2, r * 2);
            g.fillOval(cx + r - (int) (r * 0.7), cy - (int) (r * 0.35) - (int) (r * 0.7), (int) (r * 1.4), (int) (r * 1.4));
            g.fillOval(cx - r - (int) (r * 0.5), cy - (int) (r * 0.3), (int) (r * 1.2), (int) (r * 1.2));
        }

        // 远山与近丘
        ridge(g, width, height, hue + 140, 0.38f, 0.62f, height * 0.13, 5);
        ridge(g, width, height, hue + 152, 0.42f, 0.52f, height * 0.09, 7);

        // 草地
        g.setPaint(new GradientPaint(0, height * 0.72f,
                Color.getHSBColor(((hue + 160) % 360) / 360f, 0.46f, 0.58f),
                0, height, Color.getHSBColor(((hue + 170) % 360) / 360f, 0.4f, 0.44f)));
        g.fillRect(0, (int) (height * 0.72), width, (int) (height * 0.28));

        // 树
        for (int i = 0; i < 9; i++) {
            int tx = (int) (width * (0.04 + RANDOM.nextDouble() * 0.92));
            int baseY = (int) (height * (0.76 + RANDOM.nextDouble() * 0.2));
            int size = 7 + RANDOM.nextInt(8);
            g.setColor(new Color(92, 64, 38, 220));
            g.fillRect(tx - size / 8, baseY - size * 7 / 10, size / 4, size * 9 / 10);
            g.setColor(Color.getHSBColor(((hue + 150) % 360) / 360f, 0.48f, 0.42f));
            g.fillOval(tx - size / 2, baseY - size - size / 2, size, size);
            g.fillOval(tx - size, baseY - size * 3 / 4, size, size);
            g.fillOval(tx, baseY - size * 3 / 4, size, size);
        }
    }

    private static void ridge(Graphics2D g, int width, int height, int hue, float sat,
                              float bright, double amp, int steps) {
        double phase = RANDOM.nextDouble() * Math.PI * 2;
        int baseY = (int) (height * 0.78);
        Polygon polygon = new Polygon();
        polygon.addPoint(0, height);
        polygon.addPoint(0, baseY);
        for (int x = 0; x <= width; x += width / steps) {
            int y = (int) (baseY + Math.sin(x * 0.02 + phase) * amp + (RANDOM.nextDouble() * 6 - 3));
            polygon.addPoint(x, y);
        }
        polygon.addPoint(width, height);
        g.setColor(Color.getHSBColor(((hue % 360)) / 360f, sat, bright));
        g.fillPolygon(polygon);
    }
}
