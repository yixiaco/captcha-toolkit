package com.captcha.toolkit.render;

import com.jhlabs.image.InvertAlphaFilter;
import com.jhlabs.image.ShadowFilter;
import com.captcha.toolkit.config.ScratchConfig;
import com.captcha.toolkit.model.ScratchPatternSpec;
import com.captcha.toolkit.shape.PuzzleShape;
import com.captcha.toolkit.shape.PuzzleShapeRegistry;
import com.captcha.toolkit.util.ImageUtil;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 刮刮乐渲染器。
 *
 * <p>在背景图上随机埋入多个图形：颜色从图案中心背景采样，
 * 再做低明度差、小幅色相偏移与半透明叠加，让机器视觉难以直接分割；
 * 图形位置只保存在会话里，不下发给前端。</p>
 */
public class ScratchCaptchaRenderer {

    /** 可埋入的图形（排除 classic 拼图块外观，避免与滑块混淆） */
    private static final List<String> SCRATCH_SHAPES = List.of(
            "leaf", "triangle", "circle", "diamond", "star", "heart", "moon", "hexagon",
            "bat", "elephant", "dolphin", "butterfly", "whale", "owl", "bird",
            "frog", "bear", "duck", "eagle", "fish", "pig",
            "airplane", "fire", "school");

    /** 刮刮乐渲染配置 */
    private final ScratchConfig options;

    /** 图形形状注册表 */
    private final PuzzleShapeRegistry registry;

    /** 随机数源 */
    private final Random random = new Random();

    /**
     * @param options 刮刮乐配置
     */
    public ScratchCaptchaRenderer(ScratchConfig options) {
        this(options, new PuzzleShapeRegistry());
    }

    /**
     * @param options  刮刮乐配置
     * @param registry 图形形状注册表（支持宿主自定义形状）
     */
    public ScratchCaptchaRenderer(ScratchConfig options, PuzzleShapeRegistry registry) {
        this.options = options;
        this.registry = registry;
    }

    /** 渲染结果：背景图 + 全部图案布局 */
    public record ScratchRenderResult(
            BufferedImage background,
            List<ScratchPatternSpec> patterns) {
    }

    /**
     * 渲染刮刮乐背景图，并返回图案布局。
     *
     * @param raw 原始背景图
     * @return 背景图与图案布局
     */
    public ScratchRenderResult render(BufferedImage raw) {
        int w = options.getWidth();
        int h = options.getHeight();
        int scale = Math.max(1, options.getRenderScale());
        int hiW = w * scale;
        int hiH = h * scale;
        BufferedImage thumb = ImageUtil.cover(raw, hiW, hiH);

        double maxSizePx = w * options.getPatternSizeRatio();
        double minDist = maxSizePx + options.getPatternMinGap();
        List<ScratchPatternSpec> specs = new ArrayList<>();
        for (int i = 0; i < options.getPatternCount(); i++) {
            specs.add(placePattern(specs, maxSizePx, minDist));
        }

        BufferedImage out = new BufferedImage(hiW, hiH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        enableAntialias(g);
        g.drawImage(thumb, 0, 0, null);
        for (ScratchPatternSpec spec : specs) {
            drawPattern(g, thumb, spec, scale);
        }
        g.dispose();
        return new ScratchRenderResult(ImageUtil.scaleDown(out, w, h), specs);
    }

    /**
     * 渲染提示词图片：把需要刮出的图形横向排成一行（透明背景），
     * 前端直接显示这张图即可，无需知道图形名称。
     *
     * @param shapes 需要刮出的图形名称（按提示顺序）
     * @return 透明背景的提示词图片
     */
    public BufferedImage renderPromptImage(List<String> shapes) {
        int shapeSize = 44;
        int gap = 10;
        int padding = 8;
        int width = padding * 2 + shapes.size() * shapeSize + Math.max(0, shapes.size() - 1) * gap;
        int height = padding * 2 + shapeSize;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        enableAntialias(g);
        for (int i = 0; i < shapes.size(); i++) {
            PuzzleShape shape = registry.resolve(shapes.get(i));
            double x = padding + i * (shapeSize + gap);
            double y = padding;
            Path2D path = shape.create(x + 2, y + 2, shapeSize - 4);
            drawShapeWithInnerShadow(g, path, new Color(9, 88, 217), 1,
                    width, height, false);
        }
        g.dispose();
        return image;
    }

    /** 随机放置一个图案：与已有图案保持最小中心间距 */
    private ScratchPatternSpec placePattern(
            List<ScratchPatternSpec> existing, double maxSizePx, double minDist) {
        int w = options.getWidth();
        int h = options.getHeight();
        double sizeRatio = options.getPatternSizeMinRatio() + random.nextDouble()
                * (options.getPatternSizeRatio() - options.getPatternSizeMinRatio());
        double sizePx = w * sizeRatio;
        double half = sizePx / 2;
        String shape = SCRATCH_SHAPES.get(random.nextInt(SCRATCH_SHAPES.size()));
        for (int attempt = 0; attempt < 300; attempt++) {
            double cx = half + random.nextDouble() * Math.max(1, w - sizePx);
            double cy = half + random.nextDouble() * Math.max(1, h - sizePx);
            boolean tooClose = false;
            for (ScratchPatternSpec other : existing) {
                double dx = cx - other.x() * w;
                double dy = cy - other.y() * h;
                if (Math.hypot(dx, dy) < minDist) {
                    tooClose = true;
                    break;
                }
            }
            if (!tooClose) {
                return new ScratchPatternSpec(shape, cx / w, cy / h,
                        sizeRatio, random.nextDouble() * 40 - 20);
            }
        }
        // 兜底：按序号网格摆放，保证图案总数不缩水
        double cx = half + (existing.size() % 3) * (w - sizePx) / 2.0;
        double cy = half + (existing.size() / 3) * (h - sizePx) / 2.0;
        return new ScratchPatternSpec(shape, cx / w, cy / h,
                sizeRatio, 0);
    }

    /** 绘制单个图案：背景采样颜色 + 低对比填充 + 极淡描边 */
    private void drawPattern(Graphics2D g, BufferedImage thumb,
                             ScratchPatternSpec spec, int scale) {
        int w = options.getWidth();
        int h = options.getHeight();
        int hiW = w * scale;
        int hiH = h * scale;
        double sizePx = spec.size() * w * scale;
        double cx = spec.x() * w * scale;
        double cy = spec.y() * h * scale;
        PuzzleShape shape = registry.resolve(spec.shape());
        Path2D path = shape.create(cx - sizePx / 2, cy - sizePx / 2, sizePx);
        path.transform(AffineTransform.getRotateInstance(
                Math.toRadians(spec.rotation()), cx, cy));

        Color base = sampleColor(thumb, cx, cy, Math.max(2, scale * 2));
        Color fill = blendColor(base);
        drawShapeWithInnerShadow(g, path, fill, scale, hiW, hiH, true);
        g.setStroke(new BasicStroke(Math.max(1f, scale * 0.7f)));
        g.setColor(withAlpha(fill, 0.35f));
        g.draw(path);
    }

    /** 绘制带内阴影的图形：填充 + alpha 反转模糊阴影裁剪在图形内部 */
    private void drawShapeWithInnerShadow(
            Graphics2D g, Path2D path, Color fill, int scale,
            int canvasWidth, int canvasHeight, boolean whiteLayer) {
        // 内阴影蒙版：白色图形 → alpha 反转 + 模糊，裁剪在图形内形成凹陷立体感
        BufferedImage mask = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D mg = mask.createGraphics();
        mg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        mg.setClip(path);
        mg.setColor(Color.WHITE);
        mg.fill(path);
        mg.dispose();
        float shadowRadius = Math.max(1f, scale * 2f);
        ShadowFilter shadowFilter = new ShadowFilter(
                shadowRadius, 2 * scale, -1 * scale, 0.55f);
        BufferedImage innerShadow = shadowFilter.filter(
                new InvertAlphaFilter().filter(mask, null), null);

        g.setClip(path);
        g.setColor(fill);
        g.fill(path);
        if (whiteLayer) {
            // 白色透明层：与滑块拼图凹槽一致的浅色磨砂效果，让图形更清晰
            g.setColor(new Color(255, 255, 255,
                    (int) Math.round(255 * options.getHoleWhiteAlpha())));
            g.fill(path);
        }
        g.drawImage(innerShadow, 0, 0, null);
        g.setClip(null);
    }

    /** 采样图案中心附近背景的平均颜色 */
    private static Color sampleColor(BufferedImage image, double cx, double cy, int radius) {
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

    /** 基于背景色生成低对比图案色：轻微明度差 + 小幅色相偏移 + 半透明 */
    private Color blendColor(Color base) {
        float[] hsb = Color.RGBtoHSB(base.getRed(), base.getGreen(), base.getBlue(), null);
        double delta = options.getLightnessDeltaMin() + random.nextDouble()
                * (options.getLightnessDeltaMax() - options.getLightnessDeltaMin());
        float lightness = clamp01(hsb[2] + (float) (random.nextBoolean() ? delta : -delta));
        float hueShift = (float) ((random.nextDouble() * 2 - 1) * options.getHueShiftMax());
        float hue = (hsb[0] + hueShift / 360f + 1) % 1;
        float saturation = clamp01(hsb[1] + (float) (random.nextDouble() * 0.08 - 0.04));
        Color solid = new Color(Color.HSBtoRGB(hue, saturation, lightness));
        float alpha = (float) (options.getAlphaMin() + random.nextDouble()
                * (options.getAlphaMax() - options.getAlphaMin()));
        return withAlpha(solid, alpha);
    }

    /** 给颜色附加透明度 */
    private static Color withAlpha(Color color, float alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(),
                Math.round(alpha * 255));
    }

    /** 打开抗锯齿与高质量渲染 */
    private static void enableAntialias(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    /** 限制到 [0,1] */
    private static float clamp01(float value) {
        return Math.max(0, Math.min(1, value));
    }
}
