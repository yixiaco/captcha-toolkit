package com.captcha.toolkit.render;

import com.captcha.toolkit.config.SlideCurveConfig;
import com.captcha.toolkit.exception.CaptchaException;
import com.captcha.toolkit.model.PointVo;
import com.captcha.toolkit.util.ImageUtil;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 滑动曲线渲染器：大图上绘制一条两端固定的摆动曲线（由前端按滑块位置实时绘制），
 * 并绘制多个固定凹槽；其中只有真凹槽在某个摆动量下与曲线完全重合。
 * 假凹槽与真曲线共用两端固定点，但形状/振幅不同，任何摆动都无法与之重合。
 *
 * <p>曲线形状族由“端点 + 归一化形状采样 + 振幅”决定：
 * 滑块位置 s 对应的曲线为 baseline(u) + (2s-1) * amplitude * shape(u)，
 * s 从 0 到 1 时曲线在两端之间来回摆动；真凹槽即摆动量为答案时的曲线。</p>
 */
public class SlideCurveRenderer {

    /** 滑动曲线配置 */
    private final SlideCurveConfig options;

    /** 背景图提供者 */
    private final BackgroundProvider backgroundProvider;

    /** 随机数源 */
    private final Random random = new Random();

    /** 图片宽度 */
    private int width;

    /** 图片高度 */
    private int height;

    /** 曲线左端固定点 */
    private PointVo leftEnd;

    /** 曲线右端固定点 */
    private PointVo rightEnd;

    /** 曲线振幅（像素） */
    private double amplitude;

    /** 归一化形状采样（首尾为 0，其余在 [-1, 1]） */
    private List<Double> shape;

    /** 真凹槽对应的摆动答案（0~1 滑块位置） */
    private double answerSwing;

    /** 大图（背景 + 凹槽 + 固定端点） */
    private BufferedImage artwork;

    /** 假凹槽列表 */
    private final List<FakeGroove> fakeTargets = new ArrayList<>();

    /**
     * 假凹槽：固定位置 + 独立曲线形状（与摆动曲线形状不同）。
     */
    public static class FakeGroove {

        /** 凹槽采样点（绝对像素坐标） */
        private final List<PointVo> points;

        /**
         * @param points 凹槽采样点
         */
        public FakeGroove(List<PointVo> points) {
            this.points = points;
        }

        /** 返回凹槽采样点 */
        public List<PointVo> getPoints() {
            return new ArrayList<>(points);
        }
    }

    /**
     * @param options            滑动曲线配置
     * @param backgroundProvider 背景图提供者
     */
    public SlideCurveRenderer(SlideCurveConfig options, BackgroundProvider backgroundProvider) {
        this.options = options;
        this.backgroundProvider = backgroundProvider;
    }

    /** 执行一次完整渲染：生成大图与真/假凹槽 */
    public void run() {
        width = options.getWidth();
        height = options.getHeight();
        BufferedImage raw = backgroundProvider.provide(width, height)
                .orElseThrow(() -> new CaptchaException(
                        "没有可用的背景图，请配置 captcha.background.sources 或开启 generate-fallback"));
        artwork = ImageUtil.cover(raw, width, height);

        int margin = 18;
        // 两端水平距离随机：左端靠左随机、右端在中后段随机，时近时远
        int leftX = random(margin, (int) Math.round(width * 0.2));
        int rightX = random((int) Math.round(width * 0.55), width - margin);
        // 两端 y 独立随机，并保证不在同一水平轴上（有明显高差），让曲线呈斜向摆动
        int y0 = random((int) (height * 0.2), (int) (height * 0.8));
        int minEndGap = Math.max(24, (int) Math.round(height * 0.15));
        int y1 = y0;
        for (int attempt = 0; attempt < 50; attempt++) {
            y1 = random((int) (height * 0.2), (int) (height * 0.8));
            if (Math.abs(y1 - y0) >= minEndGap) {
                break;
            }
        }
        leftEnd = new PointVo(leftX, y0);
        rightEnd = new PointVo(rightX, y1);
        // 振幅取配置范围，并限制在画布可用高度内，避免曲线摆出图片
        int verticalRoom = Math.max(10, Math.min(
                Math.min(y0, y1) - 8,
                height - 8 - Math.max(y0, y1)));
        amplitude = Math.min(
                rand(options.getAmplitudeMin(), options.getAmplitudeMax()),
                verticalRoom);
        shape = buildShape(options.getSampleCount());
        answerSwing = rand(options.getSwingMin(), options.getSwingMax());

        fakeTargets.clear();
        int fakeCount = Math.max(0, options.getFakeTargetCount());
        for (int i = 0; i < fakeCount; i++) {
            fakeTargets.add(buildFakeGroove());
        }

        Graphics2D g = artwork.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        // 先画假凹槽，再画真凹槽，保证真凹槽在最上层
        for (FakeGroove fake : fakeTargets) {
            drawGroove(g, fake.points, 160);
        }
        drawGroove(g, swingCurve(answerSwing), 200);
        drawAnchors(g);
        g.dispose();
    }

    /** 生成一个假凹槽：与真曲线共用两端固定点，但形状/振幅随机，摆动永远无法对准 */
    private FakeGroove buildFakeGroove() {
        int verticalRoom = Math.max(10, Math.min(
                Math.min(leftEnd.getY(), rightEnd.getY()) - 8,
                height - 8 - Math.max(leftEnd.getY(), rightEnd.getY())));
        for (int attempt = 0; attempt < 60; attempt++) {
            List<Double> fakeShape = buildShape(options.getSampleCount());
            // 形状与真曲线几乎成比例时，某个摆动量可能与之重合，必须换一个
            if (tooSimilarToReal(fakeShape)) {
                continue;
            }
            double fakeAmplitude = Math.min(
                    rand(options.getAmplitudeMin(), options.getAmplitudeMax()),
                    verticalRoom);
            // 摆动因子必须远离 0（曲线不能接近直线），否则会被真曲线 0.5 摆动量对准
            double fakeSwing = random.nextBoolean() ? rand(0, 0.3) : rand(0.7, 1);
            List<PointVo> points = swingCurve(
                    leftEnd, rightEnd, fakeShape, fakeAmplitude, fakeSwing);
            return new FakeGroove(points);
        }
        // 兜底：用最后一次随机形状
        List<Double> fallback = buildShape(options.getSampleCount());
        List<PointVo> points = swingCurve(leftEnd, rightEnd, fallback,
                Math.min(rand(options.getAmplitudeMin(), options.getAmplitudeMax()),
                        verticalRoom), rand(0, 1));
        return new FakeGroove(points);
    }

    /** 判断假形状是否与真形状几乎成比例（成比例时某个摆动量可能对准） */
    private boolean tooSimilarToReal(List<Double> fakeShape) {
        double dot = 0;
        double norm = 0;
        for (int i = 0; i < shape.size(); i++) {
            dot += fakeShape.get(i) * shape.get(i);
            norm += shape.get(i) * shape.get(i);
        }
        double k = norm > 0 ? dot / norm : 0;
        double maxResidual = 0;
        for (int i = 0; i < shape.size(); i++) {
            maxResidual = Math.max(maxResidual,
                    Math.abs(fakeShape.get(i) - k * shape.get(i)));
        }
        return maxResidual < 0.45;
    }

    /** 生成归一化形状采样：两端为 0，中间由两个不同频率的正弦叠加并归一化到 [-1, 1] */
    private List<Double> buildShape(int count) {
        int n = Math.max(8, count);
        List<Double> result = null;
        for (int attempt = 0; attempt < 40; attempt++) {
            double phase1 = random.nextDouble() * Math.PI * 2;
            double phase2 = random.nextDouble() * Math.PI * 2;
            double[] raw = new double[n];
            double maxAbs = 0;
            for (int i = 0; i < n; i++) {
                double u = i / (double) (n - 1);
                double value = Math.sin(Math.PI * u + phase1) * 0.65
                        + Math.sin(Math.PI * 2 * u + phase2) * 0.35;
                if (i == 0 || i == n - 1) {
                    value = 0;
                }
                raw[i] = value;
                maxAbs = Math.max(maxAbs, Math.abs(value));
            }
            // 太“平”的形状（接近直线）会被 0.5 摆动量对准，重新生成
            if (maxAbs < 0.4) {
                continue;
            }
            List<Double> normalized = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                normalized.add(raw[i] / maxAbs);
            }
            result = normalized;
            break;
        }
        if (result == null) {
            result = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                result.add(Math.sin(Math.PI * i / (double) (n - 1)));
            }
        }
        return result;
    }

    /** 计算真曲线在指定摆动量下的采样点（绝对像素坐标） */
    private List<PointVo> swingCurve(double swing) {
        return swingCurve(leftEnd, rightEnd, shape, amplitude, swing);
    }

    /** 计算任意端点/形状/振幅/摆动量下的曲线采样点 */
    private List<PointVo> swingCurve(PointVo start, PointVo end,
                                     List<Double> shape, double amplitude, double swing) {
        int n = shape.size();
        double factor = (swing * 2 - 1) * amplitude;
        List<PointVo> points = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double u = i / (double) (n - 1);
            double x = start.getX() + (end.getX() - start.getX()) * u;
            double y = start.getY() + (end.getY() - start.getY()) * u
                    + factor * shape.get(i);
            points.add(new PointVo((int) Math.round(x), (int) Math.round(y)));
        }
        return points;
    }

    /** 绘制凹槽：深色曲线槽 */
    private void drawGroove(Graphics2D g, List<PointVo> points, int alpha) {
        Path2D path = new Path2D.Double();
        path.moveTo(points.getFirst().getX(), points.getFirst().getY());
        for (int i = 1; i < points.size(); i++) {
            PointVo p = points.get(i);
            path.lineTo(p.getX(), p.getY());
        }
        g.setColor(new Color(15, 20, 30, alpha));
        g.setStroke(new BasicStroke(options.getGrooveStrokeWidth(),
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(path);
    }

    /** 绘制曲线两端的固定点标记 */
    private void drawAnchors(Graphics2D g) {
        int radius = 6;
        for (PointVo anchor : List.of(leftEnd, rightEnd)) {
            g.setColor(new Color(15, 20, 30, 180));
            g.fillOval(anchor.getX() - radius - 1, anchor.getY() - radius - 1,
                    (radius + 1) * 2, (radius + 1) * 2);
            g.setColor(Color.WHITE);
            g.fillOval(anchor.getX() - radius, anchor.getY() - radius,
                    radius * 2, radius * 2);
            g.setColor(new Color(59, 124, 255, 220));
            g.fillOval(anchor.getX() - radius / 2, anchor.getY() - radius / 2,
                    radius, radius);
        }
    }

    /** 返回 [min, max] 闭区间内的随机整数 */
    private int random(int min, int max) {
        return min + random.nextInt(Math.max(1, max - min + 1));
    }

    /** 返回 [min, max] 区间内的随机浮点数 */
    private double rand(double min, double max) {
        return min + random.nextDouble() * (max - min);
    }

    /** 返回图片宽度 */
    public int getWidth() {
        return width;
    }

    /** 返回图片高度 */
    public int getHeight() {
        return height;
    }

    /** 返回曲线左端固定点 */
    public PointVo getLeftEnd() {
        return leftEnd;
    }

    /** 返回曲线右端固定点 */
    public PointVo getRightEnd() {
        return rightEnd;
    }

    /** 返回曲线振幅（像素） */
    public double getAmplitude() {
        return amplitude;
    }

    /** 返回归一化形状采样 */
    public List<Double> getShape() {
        return new ArrayList<>(shape);
    }

    /** 返回真凹槽对应的摆动答案（0~1） */
    public double getAnswerSwing() {
        return answerSwing;
    }

    /** 返回大图（背景 + 凹槽 + 固定端点） */
    public BufferedImage getArtwork() {
        return artwork;
    }

    /** 返回假凹槽列表（只读副本） */
    public List<FakeGroove> getFakeTargets() {
        return new ArrayList<>(fakeTargets);
    }
}
