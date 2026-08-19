package com.example.captcha.core;

import com.example.captcha.util.SceneBackground;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 文字点选验证码生成：随机场景 + 目标汉字 + 干扰字，
 * 颜色从背景采样、旋转倾斜绘制，最后做像素级波浪形变与噪点干扰。
 */
public class ClickCaptcha {

    private static final int WIDTH = 340;
    private static final int HEIGHT = 190;
    private static final String[] CHAR_POOL = {
            "安", "全", "快", "捷", "智", "能", "验", "证",
            "风", "控", "点", "选", "文", "字", "极", "简"
    };
    private static final String[] FONTS = {
            "华文行楷", "STXingkai", "楷体", "KaiTi", "隶书", "LiSu", "宋体", "SimSun"
    };

    private final Random random = new Random();
    private BufferedImage image;
    private final List<Point> targets = new ArrayList<>();
    private final List<String> prompt = new ArrayList<>();

    private static class Chip {
        String ch;
        boolean target;
        double x;
        double y;
        int size;
        double rotation;
        double shear;
        String font;
    }

    public void run() {
        image = SceneBackground.create(WIDTH, HEIGHT);

        List<String> pool = new ArrayList<>(List.of(CHAR_POOL));
        Collections.shuffle(pool, random);
        List<String> targetChars = pool.subList(0, 3);
        prompt.addAll(targetChars);
        List<String> distractorPool = new ArrayList<>(pool.subList(3, pool.size()));
        Collections.shuffle(distractorPool, random);
        List<String> distractorChars = distractorPool.subList(0, 5);

        List<Chip> chips = new ArrayList<>();
        for (String ch : targetChars) {
            Chip chip = tryPlace(ch, true, chips, 300);
            if (chip == null) {
                chip = forcePlace(ch, true);
            }
            chips.add(chip);
            targets.add(new Point((int) chip.x, (int) chip.y));
        }
        for (String ch : distractorChars) {
            Chip chip = tryPlace(ch, false, chips, 160);
            if (chip != null) {
                chips.add(chip);
            }
        }

        for (Chip chip : chips) {
            drawChip(chip);
        }

        image = warp(image);
        drawNoise(image.createGraphics());
    }

    private Chip tryPlace(String ch, boolean target, List<Chip> placed, int maxAttempts) {
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            int size = 18 + random.nextInt(7);
            double x = rand(18 + size / 2.0, WIDTH - 18 - size / 2.0);
            double y = rand(24 + size / 2.0, HEIGHT - 12 - size / 2.0);
            boolean clear = true;
            for (Chip p : placed) {
                if (Math.hypot(p.x - x, p.y - y) <= (p.size + size) / 2.0 + 36) {
                    clear = false;
                    break;
                }
            }
            if (clear) {
                return chip(ch, target, x, y, size);
            }
        }
        return null;
    }

    private Chip forcePlace(String ch, boolean target) {
        int size = 18 + random.nextInt(7);
        return chip(ch, target,
                rand(18 + size / 2.0, WIDTH - 18 - size / 2.0),
                rand(24 + size / 2.0, HEIGHT - 12 - size / 2.0),
                size);
    }

    private Chip chip(String ch, boolean target, double x, double y, int size) {
        Chip chip = new Chip();
        chip.ch = ch;
        chip.target = target;
        chip.x = x;
        chip.y = y;
        chip.size = size;
        chip.rotation = rand(-18, 18);
        chip.shear = rand(-0.14, 0.14);
        chip.font = FONTS[random.nextInt(FONTS.length)];
        return chip;
    }

    private void drawChip(Chip chip) {
        int px = Math.max(0, Math.min(WIDTH - 1, (int) Math.round(chip.x)));
        int py = Math.max(0, Math.min(HEIGHT - 1, (int) Math.round(chip.y)));
        Color textColor = pickColor(image.getRGB(px, py));

        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.translate(chip.x, chip.y);
        g.rotate(Math.toRadians(chip.rotation));
        g.shear(chip.shear, 0);
        g.setFont(new Font(chip.font, Font.BOLD, chip.size));
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(chip.ch);
        g.setColor(textColor);
        g.drawString(chip.ch, -tw / 2f, (fm.getAscent() - fm.getDescent()) / 2f);
        g.dispose();
    }

    private Color pickColor(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int gr = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        float[] hsl = rgbToHsl(r, gr, b);
        float shift = hsl[2] > 0.55f
                ? -(0.27f + random.nextFloat() * 0.07f)
                : 0.27f + random.nextFloat() * 0.07f;
        float nl = clamp(hsl[2] + shift, 0.14f, 0.88f);
        float ns = clamp(hsl[1] + (random.nextFloat() - 0.5f) * 0.18f, 0.2f, 0.45f);
        float nh = (hsl[0] + (random.nextFloat() * 28 - 14) + 360) % 360;
        return Color.getHSBColor(nh / 360f, ns, nl);
    }

    private float[] rgbToHsl(int r, int g, int b) {
        float rf = r / 255f;
        float gf = g / 255f;
        float bf = b / 255f;
        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float l = (max + min) / 2;
        float s;
        float h;
        if (max == min) {
            s = 0;
            h = 0;
        } else {
            float d = max - min;
            s = l > 0.5f ? d / (2 - max - min) : d / (max + min);
            if (max == rf) {
                h = (gf - bf) / d + (gf < bf ? 6 : 0);
            } else if (max == gf) {
                h = (bf - rf) / d + 2;
            } else {
                h = (rf - gf) / d + 4;
            }
            h *= 60;
        }
        return new float[]{h, s, l};
    }

    private BufferedImage warp(BufferedImage src) {
        double amp = 1.6;
        double freq = 0.09;
        double phase = random.nextDouble() * Math.PI * 2;
        double phase2 = random.nextDouble() * Math.PI * 2;
        BufferedImage out = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int sx = clamp(x + (int) Math.round(Math.sin(y * freq + phase) * amp), 0, WIDTH - 1);
                int sy = clamp(y + (int) Math.round(Math.sin(x * freq * 0.7 + phase2) * amp), 0, HEIGHT - 1);
                out.setRGB(x, y, src.getRGB(sx, sy));
            }
        }
        return out;
    }

    private void drawNoise(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (int i = 0; i < 18; i++) {
            g.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256),
                    20 + random.nextInt(30)));
            g.setStroke(new java.awt.BasicStroke((float) rand(1, 2.2)));
            int x0 = random.nextInt(WIDTH);
            int y0 = random.nextInt(HEIGHT);
            java.awt.geom.Path2D path = new java.awt.geom.Path2D.Double();
            path.moveTo(x0, y0);
            path.curveTo(random.nextInt(WIDTH), random.nextInt(HEIGHT),
                    random.nextInt(WIDTH), random.nextInt(HEIGHT),
                    random.nextInt(WIDTH), random.nextInt(HEIGHT));
            g.draw(path);
        }
        for (int i = 0; i < 110; i++) {
            g.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256),
                    12 + random.nextInt(35)));
            int r = 1 + random.nextInt(3);
            g.fillOval(random.nextInt(WIDTH), random.nextInt(HEIGHT), r, r);
        }
    }

    private double rand(double min, double max) {
        return min + random.nextDouble() * (max - min);
    }

    private int random(int min, int max) {
        return min + random.nextInt(Math.max(1, max - min + 1));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public BufferedImage getImage() {
        return image;
    }

    public List<Point> getTargets() {
        return targets;
    }

    public List<String> getPrompt() {
        return prompt;
    }

    public int getWidth() {
        return WIDTH;
    }

    public int getHeight() {
        return HEIGHT;
    }
}
