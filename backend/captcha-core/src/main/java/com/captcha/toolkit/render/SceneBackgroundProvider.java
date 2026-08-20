package com.captcha.toolkit.render;

import com.captcha.toolkit.util.ImageUtil;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.Optional;
import java.util.Random;

/**
 * 程序生成随机风景背景：无底图素材时的兜底方案。
 *
 * <p>先按 2 倍分辨率超采样绘制，再高质量缩小回目标尺寸，
 * 让太阳/云/山/树等曲线边缘产生平滑过渡，避免 1x 直出的锯齿感。</p>
 */
public class SceneBackgroundProvider implements BackgroundProvider {

    /** 超采样倍数：先画高清再缩小，等效于全局抗锯齿 */
    private static final int RENDER_SCALE = 2;

    private final Random random = new Random();

    @Override
    public Optional<BufferedImage> provide(int width, int height) {
        int hiWidth = width * RENDER_SCALE;
        int hiHeight = height * RENDER_SCALE;
        BufferedImage hi = new BufferedImage(hiWidth, hiHeight, BufferedImage.TYPE_INT_RGB);
        draw(hi.createGraphics(), hiWidth, hiHeight);
        return Optional.of(ImageUtil.scaleDown(hi, width, height));
    }

    public void draw(Graphics2D g, int width, int height) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int hue = random.nextInt(360);
        // 天空渐变
        g.setPaint(new GradientPaint(0, 0, Color.getHSBColor(hue / 360f, 0.65f, 0.78f),
                0, height, Color.getHSBColor(((hue + 55) % 360) / 360f, 0.65f, 0.94f)));
        g.fillRect(0, 0, width, height);

        drawSun(g, width, height);
        drawClouds(g, width, height);

        // 远山与近丘（渐变填充 + 更大尺寸 + 多段起伏）
        ridge(g, width, height, hue + 140, 0.38f, 0.75f, 0.55f, height * 0.17, 9);
        ridge(g, width, height, hue + 152, 0.42f, 0.66f, 0.50f, height * 0.12, 13);

        // 草地
        g.setPaint(new GradientPaint(0, height * 0.72f,
                Color.getHSBColor(((hue + 160) % 360) / 360f, 0.46f, 0.58f),
                0, height, Color.getHSBColor(((hue + 170) % 360) / 360f, 0.4f, 0.44f)));
        g.fillRect(0, (int) (height * 0.72), width, (int) (height * 0.28));

        // 树
        for (int i = 0; i < 7; i++) {
            int tx = (int) (width * (0.04 + random.nextDouble() * 0.92));
            int baseY = (int) (height * (0.76 + random.nextDouble() * 0.2));
            int size = 10 + random.nextInt(8);
            drawTree(g, tx, baseY, size, hue);
        }
    }

    /**
     * 太阳：柔光光晕（径向渐变）+ 明亮核心，不再是一块硬边圆。
     */
    private void drawSun(Graphics2D g, int width, int height) {
        int cx = (int) (width * (0.18 + random.nextDouble() * 0.64));
        int cy = (int) (height * (0.1 + random.nextDouble() * 0.22));
        int r = 13 + random.nextInt(12);

        // 外发光：两层光晕，中心亮黄 → 透明
        float[] glowFractions = {0f, 0.4f, 1f};
        Color[] glowColors = {
                new Color(255, 244, 179, 170),
                new Color(255, 244, 179, 70),
                new Color(255, 244, 179, 0)
        };
        g.setPaint(new RadialGradientPaint(cx, cy, r * 3f, glowFractions, glowColors));
        g.fillOval(cx - r * 3, cy - r * 3, r * 6, r * 6);

        // 核心：中心偏左上更亮，模拟光源
        float[] coreFractions = {0f, 0.7f, 1f};
        Color[] coreColors = {
                new Color(255, 252, 226),
                new Color(255, 240, 180),
                new Color(255, 215, 110)
        };
        g.setPaint(new RadialGradientPaint(cx - r * 0.25f, cy - r * 0.25f,
                r * 1.1f, coreFractions, coreColors));
        g.fillOval(cx - r, cy - r, r * 2, r * 2);
    }

    /**
     * 云：多圆叠加 + 圆角底座，做出蓬松柔边。
     */
    private void drawClouds(Graphics2D g, int width, int height) {
        for (int i = 0; i < 4; i++) {
            int cx = (int) (width * (0.08 + random.nextDouble() * 0.84));
            int cy = (int) (height * (0.08 + random.nextDouble() * 0.34));
            int r = 9 + random.nextInt(8);

            // 底部圆角底座（柔化边缘）
            g.setColor(new Color(255, 255, 255, 90));
            g.fill(new RoundRectangle2D.Double(
                    cx - r * 1.8, cy - r * 0.6, r * 3.6, r * 1.4, r, r));

            // 蓬松云团：低透明度叠加 + 高透明度核心
            g.setColor(new Color(255, 255, 255, 80));
            g.fillOval((int) (cx - r * 1.4), (int) (cy - r * 0.9), (int) (r * 2.8), (int) (r * 2.0));
            g.fillOval((int) (cx - r * 0.7), (int) (cy - r * 1.3), (int) (r * 2.4), (int) (r * 2.2));
            g.fillOval((int) (cx + r * 0.2), (int) (cy - r * 0.9), (int) (r * 2.2), (int) (r * 1.8));

            g.setColor(new Color(255, 255, 255, 170));
            g.fillOval((int) (cx - r * 0.9), (int) (cy - r * 0.5), (int) (r * 1.8), (int) (r * 1.4));
            g.fillOval((int) (cx + r * 0.1), (int) (cy - r * 0.6), (int) (r * 1.4), (int) (r * 1.2));
        }
    }

    /**
     * 山丘：渐变填充，顶部轻微起伏。
     */
    private void ridge(Graphics2D g, int width, int height, int hue, float sat,
                       float brightTop, float brightBottom, double amp, int steps) {
        double phase = random.nextDouble() * Math.PI * 2;
        double phase2 = random.nextDouble() * Math.PI * 2;
        int baseY = (int) (height * 0.72);
        Polygon polygon = new Polygon();
        polygon.addPoint(0, height);
        polygon.addPoint(0, baseY);
        for (int x = 0; x <= width; x += width / steps) {
            // 双频正弦叠加 + 少量随机抖动，形成大小不一的起伏
            double wave = Math.sin(x * 0.02 + phase) * amp
                    + Math.sin(x * 0.05 + phase2) * amp * 0.45
                    + (random.nextDouble() * 6 - 3);
            int y = (int) (baseY + wave);
            polygon.addPoint(x, y);
        }
        polygon.addPoint(width, height);
        g.setPaint(new GradientPaint(0, baseY,
                Color.getHSBColor(((hue % 360)) / 360f, sat, brightTop),
                0, height, Color.getHSBColor(((hue % 360)) / 360f, sat, brightBottom)));
        g.fillPolygon(polygon);
    }

    /**
     * 树：圆角渐变树干 + 径向渐变树冠（中心亮、边缘深）。
     */
    private void drawTree(Graphics2D g, int tx, int baseY, int size, int hue) {
        // 树干
        int trunkW = Math.max(2, size / 4);
        int trunkH = size * 9 / 10;
        g.setPaint(new GradientPaint(tx - trunkW / 2f, 0,
                new Color(120, 82, 46), tx + trunkW / 2f, 0, new Color(78, 52, 30)));
        g.fill(new RoundRectangle2D.Double(
                tx - trunkW / 2.0, baseY - trunkH, trunkW, trunkH, trunkW, trunkW));

        // 树冠：主圆 + 左右侧圆，形成清晰的“伞形树”，渐变中心上移让底部更深
        int canopyTop = baseY - size - size / 2;
        int mainCy = canopyTop + (int) (size * 0.55);
        drawCanopy(g, tx, mainCy, (int) (size * 1.1), hue);
        drawCanopy(g, tx - (int) (size * 0.52), mainCy + (int) (size * 0.28),
                (int) (size * 0.82), hue);
        drawCanopy(g, tx + (int) (size * 0.52), mainCy + (int) (size * 0.28),
                (int) (size * 0.82), hue);
    }

    private void drawCanopy(Graphics2D g, int cx, int cy, int size, int hue) {
        float[] fractions = {0f, 0.65f, 1f};
        Color[] colors = {
                Color.getHSBColor(((hue + 154) % 360) / 360f, 0.55f, 0.68f),
                Color.getHSBColor(((hue + 148) % 360) / 360f, 0.52f, 0.48f),
                Color.getHSBColor(((hue + 142) % 360) / 360f, 0.55f, 0.32f)
        };
        // 渐变中心偏上，树冠顶部亮、底部暗，更像立体树冠
        g.setPaint(new RadialGradientPaint(cx, cy - size * 0.2f, size * 0.9f,
                fractions, colors));
        g.fillOval(cx - size / 2, cy - size / 2, size, size);
    }
}
