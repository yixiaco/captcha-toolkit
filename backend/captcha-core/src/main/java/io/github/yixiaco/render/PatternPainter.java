package io.github.yixiaco.render;

import io.github.yixiaco.shape.ShapeGeometry;
import com.jhlabs.image.InvertAlphaFilter;
import com.jhlabs.image.ShadowFilter;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * 图案绘制工具：供刮刮乐、图形点选等“把图形融入背景”的验证码复用。
 *
 * <p>统一处理背景采样、低对比配色、内阴影与白色透明层，
 * 避免各渲染器重复实现同一套融合逻辑。</p>
 */
public final class PatternPainter {

    private PatternPainter() {
    }

    /**
     * 绘制带内阴影的图形：填充/描边 + alpha 反转模糊阴影裁剪在图形内部。
     *
     * @param g             目标画布
     * @param geometry      形状几何模型
     * @param fill          填充/描边颜色
     * @param scale         超采样倍数
     * @param canvasWidth   画布宽度
     * @param canvasHeight  画布高度
     * @param whiteAlpha    白色透明层透明度（0~1，0 表示不画）
     */
    public static void drawShapeWithInnerShadow(
            Graphics2D g, ShapeGeometry geometry, Color fill, int scale,
            int canvasWidth, int canvasHeight, double whiteAlpha) {
        // 内阴影蒙版：白色图形 → alpha 反转 + 模糊，再裁剪回图形内形成凹陷立体感
        BufferedImage mask = new BufferedImage(
                canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D mg = mask.createGraphics();
        enableAntialias(mg);
        ShapeRenderer.draw(mg, geometry, Color.WHITE);
        mg.dispose();
        float shadowRadius = Math.max(1f, scale * 2f);
        ShadowFilter shadowFilter = new ShadowFilter(
                shadowRadius, 2 * scale, -1 * scale, 0.55f);
        BufferedImage innerShadow = shadowFilter.filter(
                new InvertAlphaFilter().filter(mask, null), null);
        // 阴影只保留在图形区域内部（多路径/描边图形同样成立）
        Graphics2D sg = innerShadow.createGraphics();
        sg.setComposite(AlphaComposite.DstIn);
        sg.drawImage(mask, 0, 0, null);
        sg.dispose();

        ShapeRenderer.draw(g, geometry, fill);
        if (whiteAlpha > 0) {
            // 白色透明层：与滑块拼图凹槽一致的浅色磨砂效果，让图形更清晰
            ShapeRenderer.draw(g, geometry, withAlpha(Color.WHITE, (float) whiteAlpha));
        }
        g.drawImage(innerShadow, 0, 0, null);
    }

    /** 采样图片局部区域的平均颜色 */
    public static Color sampleColor(
            BufferedImage image, double cx, double cy, int radius) {
        int r = 0;
        int g = 0;
        int b = 0;
        int count = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                int x = (int) Math.round(cx) + dx;
                int y = (int) Math.round(cy) + dy;
                if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight()) {
                    continue;
                }
                int rgb = image.getRGB(x, y);
                r += (rgb >> 16) & 0xFF;
                g += (rgb >> 8) & 0xFF;
                b += rgb & 0xFF;
                count++;
            }
        }
        if (count == 0) {
            return new Color(120, 130, 140);
        }
        return new Color(r / count, g / count, b / count);
    }

    /**
     * 基于背景色生成低对比图案色：轻微明度差 + 小幅色相偏移 + 半透明。
     */
    public static Color blendColor(
            Color base,
            double lightnessDeltaMin, double lightnessDeltaMax,
            double hueShiftMax, double alphaMin, double alphaMax,
            Random random) {
        float[] hsb = Color.RGBtoHSB(
                base.getRed(), base.getGreen(), base.getBlue(), null);
        double delta = lightnessDeltaMin + random.nextDouble()
                * (lightnessDeltaMax - lightnessDeltaMin);
        float lightness = clamp01(hsb[2]
                + (float) (random.nextBoolean() ? delta : -delta));
        float hueShift = (float) ((random.nextDouble() * 2 - 1) * hueShiftMax);
        float hue = (hsb[0] + hueShift / 360f + 1) % 1;
        float saturation = clamp01(hsb[1] + (float) (random.nextDouble() * 0.08 - 0.04));
        Color solid = new Color(Color.HSBtoRGB(hue, saturation, lightness));
        float alpha = (float) (alphaMin + random.nextDouble() * (alphaMax - alphaMin));
        return withAlpha(solid, alpha);
    }

    /** 给颜色附加透明度 */
    public static Color withAlpha(Color color, float alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(),
                Math.round(alpha * 255));
    }

    /** 打开抗锯齿与高质量渲染 */
    public static void enableAntialias(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
    }

    /** 限制到 [0,1] */
    private static float clamp01(float value) {
        return Math.max(0, Math.min(1, value));
    }
}
