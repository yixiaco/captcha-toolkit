package io.github.yixiaco.render;

import io.github.yixiaco.config.ScratchConfig;
import io.github.yixiaco.model.ScratchPatternSpec;
import io.github.yixiaco.shape.ShapeGeometry;
import io.github.yixiaco.shape.ShapePart;
import io.github.yixiaco.shape.PuzzleShape;
import io.github.yixiaco.shape.PuzzleShapeRegistry;
import io.github.yixiaco.util.ImageUtil;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
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

    /** 刮刮乐渲染配置 */
    private final ScratchConfig options;

    /** 图形形状注册表 */
    private final PuzzleShapeRegistry registry;

    /** 可埋入的图形名称（排除 classic 拼图块外观，避免与滑块混淆） */
    private final List<String> scratchShapes;

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
        this.scratchShapes = registry.names().stream()
                .filter(name -> !"classic".equals(name))
                .toList();
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
        PatternPainter.enableAntialias(g);
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
        return ShapePromptRenderer.render(registry, shapes);
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
        String shape = scratchShapes.get(random.nextInt(scratchShapes.size()));
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
        ShapeGeometry geometry = shape.geometry(
                cx - sizePx / 2, cy - sizePx / 2, sizePx);
        geometry = geometry.rotated(spec.rotation(), cx, cy);

        Color base = PatternPainter.sampleColor(
                thumb, cx, cy, Math.max(2, scale * 2));
        Color fill = PatternPainter.blendColor(base,
                options.getLightnessDeltaMin(), options.getLightnessDeltaMax(),
                options.getHueShiftMax(), options.getAlphaMin(), options.getAlphaMax(),
                random);
        PatternPainter.drawShapeWithInnerShadow(g, geometry, fill, scale,
                hiW, hiH, options.getHoleWhiteAlpha());
        // 纯填充图形补一圈淡描边形成微弱边缘；描边图形已有自身线宽，不再叠加
        g.setStroke(new BasicStroke(Math.max(1f, scale * 0.7f)));
        g.setColor(PatternPainter.withAlpha(fill, 0.35f));
        for (ShapePart part : geometry.parts()) {
            if (part.filled() && !part.stroked()) {
                g.draw(part.path());
            }
        }
    }
}
