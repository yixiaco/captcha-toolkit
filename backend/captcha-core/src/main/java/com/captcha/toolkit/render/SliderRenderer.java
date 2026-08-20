package com.captcha.toolkit.render;

import com.captcha.toolkit.config.SliderConfig;
import com.captcha.toolkit.exception.CaptchaException;
import com.captcha.toolkit.shape.PuzzleShape;
import com.captcha.toolkit.shape.PuzzleShapeRegistry;
import com.captcha.toolkit.util.ImageUtil;
import com.jhlabs.image.InvertAlphaFilter;
import com.jhlabs.image.ShadowFilter;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 滑块渲染器：负责把“背景 + 缺口 + 拼图块 + 阴影”画出来。
 *
 * <p>使用 2 倍超采样 + 高质量缩小的抗锯齿方案：
 * 先在 renderScale 倍分辨率画布上绘制，再双线性缩小回目标尺寸，
 * 让弧形/斜边产生平滑的过渡像素。</p>
 */
public class SliderRenderer {

    private final SliderConfig options;
    private final BackgroundProvider backgroundProvider;
    private final PuzzleShapeRegistry shapeRegistry;
    private final Random random = new Random();
    private final InvertAlphaFilter alphaFilter = new InvertAlphaFilter();

    private int width;
    private int height;
    private int x;
    private int y;
    private int vwh;
    private String shapeName;
    private BufferedImage source;
    private BufferedImage artwork;
    private BufferedImage vacancy;
    private int pieceOffsetX;
    private final List<FakeTarget> fakeTargets = new ArrayList<>();

    /**
     * 假目标：位置 + 独立大小与旋转。
     * 允许与真目标/其他假目标在同一 y 轴，但同 y 时大小和旋转必须不同。
     */
    public static class FakeTarget {
        private final int x;
        private final int y;
        private final int size;
        private final double rotation;

        public FakeTarget(int x, int y, int size, double rotation) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.rotation = rotation;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getSize() {
            return size;
        }

        public double getRotation() {
            return rotation;
        }
    }

    public SliderRenderer(SliderConfig options,
                          BackgroundProvider backgroundProvider,
                          PuzzleShapeRegistry shapeRegistry) {
        this.options = options;
        this.backgroundProvider = backgroundProvider;
        this.shapeRegistry = shapeRegistry;
        this.width = options.getWidth();
        this.height = options.getHeight();
        this.shapeName = options.getDefaultShape();
    }

    public void run() {
        source = backgroundProvider.provide(width, height)
                .orElseThrow(() -> new CaptchaException("没有可用的背景图，请配置 captcha.background.sources 或开启 generate-fallback"));
        // 拼图块边长按宽度等比缩放（参考 puzzle_captcha：280 宽用 30）
        vwh = Math.max(24, (int) Math.round(width * options.getPieceSizeRatio()));
        x = random(options.getMargin(), width - vwh - options.getMargin());
        y = random(options.getMargin(), height - vwh - options.getMargin());

        fakeTargets.clear();
        int fakeCount = Math.max(0, options.getFakeTargetCount());
        for (int i = 0; i < fakeCount; i++) {
            FakeTarget fake = tryPlaceFake();
            if (fake == null) {
                break;
            }
            fakeTargets.add(fake);
        }

        int renderScale = Math.max(1, options.getRenderScale());
        int renderWidth = width * renderScale;
        int renderHeight = height * renderScale;
        int renderVwh = vwh * renderScale;
        PuzzleShape shape = shapeRegistry.resolve(shapeName);
        Path2D path = shape.create(x * renderScale, y * renderScale, renderVwh);
        BufferedImage thumbnail = ImageUtil.cover(source, renderWidth, renderHeight);

        // 小图（高清画布）：拼图块 = 原图按路径裁剪
        BufferedImage pieceFull = transparent(renderWidth, renderHeight);
        Graphics2D vg = pieceFull.createGraphics();
        vg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        vg.setClip(path);
        vg.drawImage(thumbnail, 0, 0, null);
        vg.dispose();

        // 大图（高清画布）：原图 + 白色半透明缺口蒙版 + 内阴影
        BufferedImage artworkFull = transparent(renderWidth, renderHeight);
        Graphics2D g = artworkFull.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(thumbnail, 0, 0, null);
        g.setClip(path);
        g.setComposite(AlphaComposite.SrcAtop);
        g.setColor(new Color(255, 255, 255, clampAlpha(options.getHoleAlpha())));
        g.fill(path);

        float radius = Math.max(1f, options.getShadowRadius()) * renderScale;
        ShadowFilter shadowFilter = new ShadowFilter(radius,
                options.getShadowOffsetX() * renderScale,
                options.getShadowOffsetY() * renderScale,
                options.getShadowOpacity());
        BufferedImage innerShadow = shadowFilter.filter(alphaFilter.filter(pieceFull, null), null);
        g.drawImage(innerShadow, 0, 0, null);

        // 假目标：画成和真目标一样的白色缺口 + 内阴影，但小图里没有对应拼图块；
        // 每个假目标有独立大小与旋转（同 y 轴时保证与真目标/彼此不同）
        for (FakeTarget fake : fakeTargets) {
            int fakeRenderSize = Math.max(8, fake.size * renderScale);
            Path2D fakePath = shape.create(fake.x * renderScale, fake.y * renderScale,
                    fakeRenderSize);
            double centerX = (fake.x + fake.size / 2.0) * renderScale;
            double centerY = (fake.y + fake.size / 2.0) * renderScale;
            fakePath.transform(AffineTransform.getRotateInstance(
                    Math.toRadians(fake.rotation), centerX, centerY));
            BufferedImage fakeHoleFull = transparent(renderWidth, renderHeight);
            Graphics2D fg = fakeHoleFull.createGraphics();
            fg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            fg.setClip(fakePath);
            fg.setColor(Color.WHITE);
            fg.fill(fakePath);
            fg.dispose();
            BufferedImage fakeShadow = shadowFilter.filter(alphaFilter.filter(fakeHoleFull, null), null);
            g.setClip(fakePath);
            g.setComposite(AlphaComposite.SrcAtop);
            g.setColor(new Color(255, 255, 255, clampAlpha(options.getHoleAlpha())));
            g.fill(fakePath);
            g.drawImage(fakeShadow, 0, 0, null);
        }
        g.dispose();

        // 高清大图缩小回目标尺寸，边缘自动平滑
        artwork = ImageUtil.scaleDown(artworkFull, width, height);

        // 小图裁剪成竖条（拼图块 + 投影），并记录内部左侧留白
        Rectangle2D bounds = path.getBounds2D();
        int pad = Math.max(1, options.getPiecePadding());
        int cropX = Math.max(0, (x - pad) * renderScale);
        int cropW = (int) Math.ceil(bounds.getWidth() / renderScale) + pad * 2;
        cropW = Math.min(width - cropX / renderScale, cropW);
        int cropWHigh = cropW * renderScale;
        BufferedImage strip = pieceFull.getSubimage(cropX, 0, cropWHigh, renderHeight);
        BufferedImage stripShadowed = shadowFilter.filter(strip, null);
        vacancy = ImageUtil.scaleDown(stripShadowed, cropW, height);
        pieceOffsetX = x - cropX / renderScale;
    }

    private BufferedImage transparent(int w, int h) {
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    }

    private int random(int min, int max) {
        return min + random.nextInt(Math.max(1, max - min + 1));
    }

    /**
     * 放置一个假目标：
     * <ul>
     *   <li>允许与真目标/其他假目标在同一 y 轴；</li>
     *   <li>但同 y 轴时，大小和旋转必须与对方不同；</li>
     *   <li>任意两个目标中心距离不小于两者边长一半 + 最小间距，避免重叠。</li>
     * </ul>
     */
    private FakeTarget tryPlaceFake() {
        for (int attempt = 0; attempt < 400; attempt++) {
            int size = Math.max(16, (int) Math.round(vwh * rand(0.72, 1.28)));
            double rotation = rand(-28, 28);
            int fx = random(options.getMargin(), width - size - options.getMargin());
            int fy = random(options.getMargin(), height - size - options.getMargin());

            // 与真目标同 y 轴：大小、旋转都必须不同
            if (fy == y && (size == vwh || Math.abs(rotation) < 0.5)) {
                continue;
            }

            boolean clear = true;
            for (FakeTarget existing : fakeTargets) {
                double minDist = (existing.size + size) / 2.0
                        + Math.max(4, options.getFakeTargetMinGap());
                if (Math.hypot(existing.x - fx, existing.y - fy) < minDist) {
                    clear = false;
                    break;
                }
                // 与其他假目标同 y 轴：大小、旋转都必须不同
                if (existing.y == fy
                        && (existing.size == size
                        || Math.abs(existing.rotation - rotation) < 0.5)) {
                    clear = false;
                    break;
                }
            }
            if (clear) {
                return new FakeTarget(fx, fy, size, rotation);
            }
        }
        return null;
    }

    private double rand(double min, double max) {
        return min + random.nextDouble() * (max - min);
    }

    private static int clampAlpha(int alpha) {
        return Math.max(0, Math.min(255, alpha));
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getPieceSize() {
        return vwh;
    }

    public String getShape() {
        return shapeName;
    }

    public void setShape(String shapeName) {
        this.shapeName = shapeName;
    }

    public BufferedImage getArtwork() {
        return artwork;
    }

    public BufferedImage getVacancy() {
        return vacancy;
    }

    public int getPieceOffsetX() {
        return pieceOffsetX;
    }

    public List<FakeTarget> getFakeTargets() {
        return new ArrayList<>(fakeTargets);
    }
}
