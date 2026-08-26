package com.captcha.toolkit.render;

import com.captcha.toolkit.config.ClickShapeConfig;
import com.captcha.toolkit.model.ScratchPatternSpec;
import com.captcha.toolkit.shape.PuzzleShape;
import com.captcha.toolkit.shape.PuzzleShapeRegistry;
import com.captcha.toolkit.shape.ShapeGeometry;
import com.captcha.toolkit.shape.ShapePart;
import com.captcha.toolkit.util.ImageUtil;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 图形点选渲染器：把指定图形列表按顺序埋入背景图，目标图形保持点击顺序。
 *
 * <p>图形颜色从背景采样并做低对比调整（与刮刮乐同一套融合逻辑），
 * 机器视觉难以直接分割；图形坐标只保存在会话里，不下发给前端。</p>
 */
public class ClickShapeCaptchaRenderer {

    /** 图形点选配置 */
    private final ClickShapeConfig options;

    /** 图形形状注册表 */
    private final PuzzleShapeRegistry registry;

    /** 随机数源 */
    private final Random random = new Random();

    /** 渲染结果：背景图 + 图形布局（顺序与传入的 shapes 一致） */
    public record RenderResult(
            BufferedImage background,
            List<ScratchPatternSpec> patterns) {
    }

    /**
     * @param options 图形点选配置
     */
    public ClickShapeCaptchaRenderer(ClickShapeConfig options) {
        this(options, new PuzzleShapeRegistry());
    }

    /**
     * @param options  图形点选配置
     * @param registry 图形形状注册表（支持宿主自定义形状）
     */
    public ClickShapeCaptchaRenderer(ClickShapeConfig options,
                                     PuzzleShapeRegistry registry) {
        this.options = options;
        this.registry = registry;
    }

    /**
     * 渲染图形点选背景图，并返回图形布局。
     *
     * @param raw    原始背景图
     * @param shapes 全部图形名称（目标在前、按点击顺序，干扰在后）
     * @return 背景图与图形布局
     */
    public RenderResult render(BufferedImage raw, List<String> shapes) {
        int w = options.getWidth();
        int h = options.getHeight();
        int scale = Math.max(1, options.getRenderScale());
        int hiW = w * scale;
        int hiH = h * scale;
        BufferedImage thumb = ImageUtil.cover(raw, hiW, hiH);

        double maxSizePx = w * options.getPatternSizeRatio();
        double minDist = maxSizePx + options.getPatternMinGap();
        List<ScratchPatternSpec> specs = new ArrayList<>(shapes.size());
        for (String shape : shapes) {
            specs.add(place(shape, specs, maxSizePx, minDist));
        }

        BufferedImage out = new BufferedImage(hiW, hiH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        PatternPainter.enableAntialias(g);
        g.drawImage(thumb, 0, 0, null);
        for (ScratchPatternSpec spec : specs) {
            drawPattern(g, thumb, spec, scale);
        }
        g.dispose();
        return new RenderResult(ImageUtil.scaleDown(out, w, h), specs);
    }

    /** 随机放置一个图形：与已有图形保持最小中心间距 */
    private ScratchPatternSpec place(
            String shape, List<ScratchPatternSpec> existing,
            double maxSizePx, double minDist) {
        int w = options.getWidth();
        int h = options.getHeight();
        double sizeRatio = options.getPatternSizeMinRatio() + random.nextDouble()
                * (options.getPatternSizeRatio() - options.getPatternSizeMinRatio());
        double sizePx = w * sizeRatio;
        double half = sizePx / 2;
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
                        sizeRatio, random.nextDouble() * options.getRotationMax() * 2
                                - options.getRotationMax());
            }
        }
        // 兜底：按序号网格摆放，保证图形总数不缩水
        double cx = half + (existing.size() % 3) * (w - sizePx) / 2.0;
        double cy = half + (existing.size() / 3) * (h - sizePx) / 2.0;
        return new ScratchPatternSpec(shape, cx / w, cy / h, sizeRatio, 0);
    }

    /** 绘制单个图形：背景采样颜色 + 低对比填充 + 极淡描边 */
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
