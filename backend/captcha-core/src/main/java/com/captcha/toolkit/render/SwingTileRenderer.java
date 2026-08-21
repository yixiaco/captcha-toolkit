package com.captcha.toolkit.render;

import com.captcha.toolkit.config.SwingTileConfig;
import com.captcha.toolkit.exception.CaptchaException;
import com.captcha.toolkit.model.PointVo;
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
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 滑块摆动图块渲染器：大图上绘制目标凹槽与多个假凹槽，并生成一块小图块；
 * 用户拖动滑块时，图块沿多阶贝塞尔曲线从起点运动到终点，
 * 方向按曲线摆动，终点方向与真凹槽完全一致。
 */
public class SwingTileRenderer {

    /** 滑块摆动图块配置 */
    private final SwingTileConfig options;

    /** 背景图提供者 */
    private final BackgroundProvider backgroundProvider;

    /** 拼图形状注册表 */
    private final PuzzleShapeRegistry shapeRegistry;

    /** 随机数源 */
    private final Random random = new Random();

    /** alpha 反转滤镜（用于生成凹槽内阴影） */
    private final InvertAlphaFilter alphaFilter = new InvertAlphaFilter();

    /** 图片宽度 */
    private int width;

    /** 图片高度 */
    private int height;

    /** 图块边长（像素） */
    private int pieceSize;

    /** 图块图片边长（含裁剪留白，前端按此尺寸显示） */
    private int pieceImageSize;

    /** 起点中心（像素） */
    private PointVo startPoint;

    /** 终点中心（像素，真凹槽位置） */
    private PointVo targetPoint;

    /** 路径终点（t=1 时图块到达的位置，不一定有凹槽） */
    private PointVo endPoint;

    /** 贝塞尔路径点（起点 + 控制点 + 终点） */
    private List<PointVo> path;

    /** 起始方向（度） */
    private double startRotation;

    /** 终点方向（度，与真凹槽一致） */
    private double endRotation;

    /** 真凹槽方向（度，路径上 answerT 处的图块方向） */
    private double targetRotation;

    /** 方向摆动幅度（度） */
    private double swingAmplitude;

    /** 真凹槽在路径上的位置（0~1 滑块位置） */
    private double answerT;

    /** 大图（背景 + 凹槽 + 引导路径） */
    private BufferedImage artwork;

    /** 小图（图块，透明背景） */
    private BufferedImage piece;

    /** 假凹槽列表 */
    private final List<FakeTarget> fakeTargets = new ArrayList<>();

    /**
     * 假凹槽：中心 + 独立方向。
     */
    public static class FakeTarget {

        /** 中心 x */
        private final int x;

        /** 中心 y */
        private final int y;

        /** 方向（度） */
        private final double rotation;

        /**
         * @param x        中心 x
         * @param y        中心 y
         * @param rotation 方向（度）
         */
        public FakeTarget(int x, int y, double rotation) {
            this.x = x;
            this.y = y;
            this.rotation = rotation;
        }

        /** 返回中心 x */
        public int getX() {
            return x;
        }

        /** 返回中心 y */
        public int getY() {
            return y;
        }

        /** 返回方向（度） */
        public double getRotation() {
            return rotation;
        }
    }

    /**
     * @param options            滑块摆动图块配置
     * @param backgroundProvider 背景图提供者
     * @param shapeRegistry      拼图形状注册表
     */
    public SwingTileRenderer(SwingTileConfig options,
                             BackgroundProvider backgroundProvider,
                             PuzzleShapeRegistry shapeRegistry) {
        this.options = options;
        this.backgroundProvider = backgroundProvider;
        this.shapeRegistry = shapeRegistry;
    }

    /** 执行一次完整渲染：生成大图、图块与假凹槽 */
    public void run() {
        width = options.getWidth();
        height = options.getHeight();
        BufferedImage raw = backgroundProvider.provide(width, height)
                .orElseThrow(() -> new CaptchaException(
                        "没有可用的背景图，请配置 captcha.background.sources 或开启 generate-fallback"));
        int scale = Math.max(1, options.getRenderScale());
        int hiW = width * scale;
        int hiH = height * scale;
        BufferedImage thumb = ImageUtil.cover(raw, hiW, hiH);

        int margin = 16;
        int half = (pieceSize = Math.max(24,
                (int) Math.round(width * options.getPieceSizeRatio()))) / 2;
        pieceImageSize = pieceSize + options.getPiecePadding() * 2;
        int minX = margin + half;
        int maxX = width - margin - half;
        int minY = margin + half;
        int maxY = height - margin - half;

        // 路径终点（t=1 时图块到达的位置）靠右侧随机，方向随机小角度
        endPoint = new PointVo(random(minX, Math.max(minX, (int) (width * 0.9) - half)),
                random(minY, maxY));
        endRotation = rand(options.getEndRotationMin(), options.getEndRotationMax());

        // 起点靠左侧随机
        startPoint = new PointVo(random(minX, Math.max(minX, (int) (width * 0.2) - half)),
                random(minY, maxY));
        startRotation = endRotation + rand(-options.getStartRotationMax(),
                options.getStartRotationMax());
        swingAmplitude = options.getRotationSwingAmplitude();

        // 多阶贝塞尔路径：控制点数量可配置
        path = buildPath(startPoint, endPoint, minX, maxX, minY, maxY);

        // 真凹槽放在路径中随机位置（变速缓动后的贝塞尔点 + 对应方向）
        answerT = rand(options.getAnswerMin(), options.getAnswerMax());
        double answerU = ease(answerT);
        targetPoint = bezierPoint(path, answerU);
        targetRotation = rotationAt(answerU);

        fakeTargets.clear();
        int fakeCount = Math.max(0, options.getFakeTargetCount());
        for (int i = 0; i < fakeCount; i++) {
            FakeTarget fake = tryPlaceFake(minX, maxX, minY, maxY);
            if (fake == null) {
                break;
            }
            fakeTargets.add(fake);
        }

        // 与原滑块渲染一致：先在 renderScale 高清画布上画背景与凹槽，再整体缩小
        BufferedImage artworkHi = new BufferedImage(hiW, hiH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = artworkHi.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.drawImage(thumb, 0, 0, null);
        for (FakeTarget fake : fakeTargets) {
            drawHole(g, fake.x * scale, fake.y * scale, fake.rotation, 150, scale, hiW, hiH);
        }
        drawHole(g, targetPoint.getX() * scale, targetPoint.getY() * scale,
                targetRotation, 200, scale, hiW, hiH);
        g.dispose();
        artwork = ImageUtil.scaleDown(artworkHi, width, height);

        piece = buildPiece(thumb, scale);
    }

    /** 构建贝塞尔路径：起点 + N 个控制点 + 终点 */
    private List<PointVo> buildPath(PointVo start, PointVo end,
                                    int minX, int maxX, int minY, int maxY) {
        int count = Math.max(1, options.getControlPointCount());
        double spanX = Math.max(1, end.getX() - start.getX());
        double phase = random.nextDouble() * Math.PI * 2;
        double waves = rand(1, 2);
        double waveAmp = rand(height * 0.12, height * 0.28);
        List<PointVo> points = new ArrayList<>();
        points.add(start);
        for (int i = 1; i <= count; i++) {
            double u = i / (double) (count + 1);
            int x = (int) Math.round(start.getX() + (end.getX() - start.getX()) * u
                    + rand(-spanX * 0.08, spanX * 0.08));
            int y = (int) Math.round(start.getY() + (end.getY() - start.getY()) * u
                    + Math.sin(u * Math.PI * waves + phase) * waveAmp
                    + rand(-8, 8));
            points.add(new PointVo(clamp(x, minX, maxX), clamp(y, minY, maxY)));
        }
        points.add(end);
        return points;
    }

    /** 尝试放置一个假凹槽：与真凹槽及其他假凹槽保持中心间距 */
    private FakeTarget tryPlaceFake(int minX, int maxX, int minY, int maxY) {
        int minGap = pieceSize + Math.max(8, options.getFakeTargetMinGap() / 2);
        for (int attempt = 0; attempt < 400; attempt++) {
            int fx = random(minX, maxX);
            int fy = random(minY, maxY);
            // 假凹槽必须远离整条贝塞尔路径与真凹槽，保证任何滑块位置都对不上
            if (nearPath(fx, fy, minGap)) {
                continue;
            }
            boolean clear = true;
            for (FakeTarget existing : fakeTargets) {
                if (Math.hypot(existing.x - fx, existing.y - fy) < minGap) {
                    clear = false;
                    break;
                }
            }
            if (!clear) {
                continue;
            }
            return new FakeTarget(fx, fy, rand(-45, 45));
        }
        return null;
    }

    /** 判断坐标是否靠近贝塞尔路径（按缓动采样）或真凹槽 */
    private boolean nearPath(int x, int y, double minGap) {
        if (Math.hypot(x - targetPoint.getX(), y - targetPoint.getY()) < minGap) {
            return true;
        }
        for (int i = 0; i <= 40; i++) {
            PointVo p = bezierPoint(path, ease(i / 40.0));
            if (Math.hypot(x - p.getX(), y - p.getY()) < minGap) {
                return true;
            }
        }
        return false;
    }

    /**
     * 绘制一个凹槽（与原滑块一致的画法）：高清画布上，
     * 白色半透明镂空（SrcAtop 叠加在背景上）+ 裁剪在凹槽内的内阴影。
     */
    private void drawHole(Graphics2D g, int cx, int cy, double rotation, int alpha,
                          int scale, int hw, int hh) {
        PuzzleShape shape = shapeRegistry.resolve("classic");
        // 直接旋转路径几何（与原滑块一致），避免“旋转坐标系 + setClip”的变换歧义
        Path2D base = shape.create(cx - pieceSize * scale / 2.0,
                cy - pieceSize * scale / 2.0, pieceSize * scale);
        Path2D rotated = new Path2D.Double();
        rotated.append(base.getPathIterator(
                AffineTransform.getRotateInstance(Math.toRadians(rotation), cx, cy)), false);

        // 高清白色镂空蒙版（用于生成内阴影）
        BufferedImage mask = new BufferedImage(hw, hh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D mg = mask.createGraphics();
        mg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        mg.setClip(rotated);
        mg.setColor(Color.WHITE);
        mg.fill(rotated);
        mg.dispose();

        float radius = Math.max(1f, options.getShadowRadius()) * scale;
        ShadowFilter shadow = new ShadowFilter(radius, 2 * scale, -1 * scale,
                options.getShadowOpacity());
        BufferedImage innerShadow = shadow.filter(alphaFilter.filter(mask, null), null);

        // 白色半透明镂空直接叠加在已有背景的高清画布上（SrcAtop），再叠内阴影
        g.setClip(rotated);
        g.setComposite(AlphaComposite.SrcAtop);
        g.setColor(new Color(255, 255, 255, alpha));
        g.fill(rotated);
        g.drawImage(innerShadow, 0, 0, null);
        g.setComposite(AlphaComposite.SrcOver);
        g.setClip(null);
    }

    /** 构建图块：按终点方向从高清背景取纹理，旋转后仍与凹槽下背景完全一致 */
    private BufferedImage buildPiece(BufferedImage thumb, int scale) {
        int cropSize = pieceSize + options.getPiecePadding() * 2;
        int cropHi = cropSize * scale;

        // 形状蒙版（终点方向 0 度，居中）
        BufferedImage mask = new BufferedImage(cropHi, cropHi, BufferedImage.TYPE_INT_ARGB);
        Graphics2D mg = mask.createGraphics();
        mg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        mg.setColor(Color.WHITE);
        PuzzleShape shape = shapeRegistry.resolve("classic");
        mg.fill(shape.create((cropHi - pieceSize * scale) / 2.0,
                (cropHi - pieceSize * scale) / 2.0, pieceSize * scale));
        mg.dispose();

        // 纹理：逆旋转背景，使图块在终点方向下与凹槽下背景一致
        BufferedImage texture = new BufferedImage(cropHi, cropHi, BufferedImage.TYPE_INT_ARGB);
        Graphics2D tg = texture.createGraphics();
        tg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        AffineTransform at = new AffineTransform();
        at.translate(cropHi / 2.0, cropHi / 2.0);
        at.rotate(Math.toRadians(-targetRotation));
        at.translate(-targetPoint.getX() * scale, -targetPoint.getY() * scale);
        tg.transform(at);
        tg.drawImage(thumb, 0, 0, null);
        tg.dispose();

        BufferedImage pieceHi = new BufferedImage(cropHi, cropHi, BufferedImage.TYPE_INT_ARGB);
        Graphics2D pg = pieceHi.createGraphics();
        pg.drawImage(texture, 0, 0, null);
        pg.setComposite(AlphaComposite.DstIn);
        pg.drawImage(mask, 0, 0, null);
        pg.dispose();

        float radius = Math.max(1f, options.getShadowRadius()) * scale;
        ShadowFilter shadow = new ShadowFilter(radius, 2 * scale, 2 * scale, 0.4f);
        return ImageUtil.scaleDown(shadow.filter(pieceHi, null), cropSize, cropSize);
    }

    /** 变速缓动：smoothstep，起点/终点慢、中间快，保证 0→1 单调 */
    private static double ease(double t) {
        double x = Math.max(0, Math.min(1, t));
        return x * x * (3 - 2 * x);
    }

    /** de Casteljau：计算多阶贝塞尔曲线上的点 */
    private static PointVo bezierPoint(List<PointVo> points, double t) {
        List<double[]> current = new ArrayList<>();
        for (PointVo p : points) {
            current.add(new double[]{p.getX(), p.getY()});
        }
        while (current.size() > 1) {
            List<double[]> next = new ArrayList<>();
            for (int i = 0; i < current.size() - 1; i++) {
                next.add(new double[]{
                        current.get(i)[0] + (current.get(i + 1)[0] - current.get(i)[0]) * t,
                        current.get(i)[1] + (current.get(i + 1)[1] - current.get(i)[1]) * t});
            }
            current = next;
        }
        return new PointVo((int) Math.round(current.get(0)[0]),
                (int) Math.round(current.get(0)[1]));
    }

    /** 图块在路径参数 u 处的方向 */
    private double rotationAt(double u) {
        return endRotation + (startRotation - endRotation) * (1 - u)
                + swingAmplitude * Math.sin(Math.PI * u);
    }

    /** 返回 [min, max] 闭区间内的随机整数 */
    private int random(int min, int max) {
        return min + random.nextInt(Math.max(1, max - min + 1));
    }

    /** 返回 [min, max] 区间内的随机浮点数 */
    private double rand(double min, double max) {
        return min + random.nextDouble() * (max - min);
    }

    /** 把整数限制在 [min, max] */
    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /** 返回图片宽度 */
    public int getWidth() {
        return width;
    }

    /** 返回图片高度 */
    public int getHeight() {
        return height;
    }

    /** 返回图块边长 */
    public int getPieceSize() {
        return pieceSize;
    }

    /** 返回图块图片边长（含裁剪留白） */
    public int getPieceImageSize() {
        return pieceImageSize;
    }

    /** 返回起点中心 */
    public PointVo getStartPoint() {
        return startPoint;
    }

    /** 返回终点中心（真凹槽位置） */
    public PointVo getTargetPoint() {
        return targetPoint;
    }

    /** 返回真凹槽在路径上的位置（0~1 滑块位置） */
    public double getAnswerT() {
        return answerT;
    }

    /** 返回贝塞尔路径点（起点 + 控制点 + 终点） */
    public List<PointVo> getPath() {
        return new ArrayList<>(path);
    }

    /** 返回起始方向（度） */
    public double getStartRotation() {
        return startRotation;
    }

    /** 返回终点方向（度） */
    public double getEndRotation() {
        return endRotation;
    }

    /** 返回真凹槽方向（度） */
    public double getTargetRotation() {
        return targetRotation;
    }

    /** 返回方向摆动幅度（度） */
    public double getSwingAmplitude() {
        return swingAmplitude;
    }

    /** 返回大图 */
    public BufferedImage getArtwork() {
        return artwork;
    }

    /** 返回小图（图块） */
    public BufferedImage getPiece() {
        return piece;
    }

    /** 返回假凹槽列表（只读副本） */
    public List<FakeTarget> getFakeTargets() {
        return new ArrayList<>(fakeTargets);
    }
}
