package com.captcha.toolkit.generator;

import com.captcha.toolkit.CaptchaType;
import com.captcha.toolkit.behavior.BehaviorValidator;
import com.captcha.toolkit.behavior.CurveBehaviorValidator;
import com.captcha.toolkit.config.BehaviorConfig;
import com.captcha.toolkit.config.CurveConfig;
import com.captcha.toolkit.exception.CaptchaException;
import com.captcha.toolkit.i18n.CaptchaMessages;
import com.captcha.toolkit.i18n.MessageProvider;
import com.captcha.toolkit.i18n.ResourceBundleMessageProvider;
import com.captcha.toolkit.model.CaptchaAnswer;
import com.captcha.toolkit.model.CaptchaSession;
import com.captcha.toolkit.model.GeneratedCaptcha;
import com.captcha.toolkit.model.NormalizedPoint;
import com.captcha.toolkit.model.PointVo;
import com.captcha.toolkit.model.VerifyResult;
import com.captcha.toolkit.render.BackgroundProvider;
import com.captcha.toolkit.util.ImageUtil;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * 曲线绘制验证码生成器。
 *
 * <p>在背景图上随机生成一条平滑引导曲线（起点/终点带标记），用户沿曲线完整绘制；
 * 服务端保存期望曲线采样点，校验绘制覆盖率与起终点是否一致。</p>
 */
public class CurveCaptchaGenerator extends AbstractCaptchaGenerator {

    /** 曲线配置 */
    private final CurveConfig options;

    /** 背景图提供者 */
    private final BackgroundProvider backgroundProvider;

    /** 曲线行为轨迹校验器 */
    private final BehaviorValidator behaviorValidator;

    /** 随机数源 */
    private final Random random = new Random();

    /** 使用默认（关闭）行为校验构造生成器 */
    public CurveCaptchaGenerator(CurveConfig options, BackgroundProvider backgroundProvider) {
        this(options, backgroundProvider,
                new CurveBehaviorValidator(new BehaviorConfig()),
                new ResourceBundleMessageProvider());
    }

    /**
     * @param options            曲线配置
     * @param backgroundProvider 背景图提供者
     * @param behaviorValidator  行为轨迹校验器
     */
    public CurveCaptchaGenerator(CurveConfig options,
                                 BackgroundProvider backgroundProvider,
                                 BehaviorValidator behaviorValidator) {
        this(options, backgroundProvider, behaviorValidator,
                new ResourceBundleMessageProvider());
    }

    /**
     * @param options            曲线配置
     * @param backgroundProvider 背景图提供者
     * @param behaviorValidator  行为轨迹校验器
     * @param messages           用户提示消息提供者
     */
    public CurveCaptchaGenerator(CurveConfig options,
                                 BackgroundProvider backgroundProvider,
                                 BehaviorValidator behaviorValidator,
                                 MessageProvider messages) {
        super(messages);
        this.options = options;
        this.backgroundProvider = backgroundProvider;
        this.behaviorValidator = behaviorValidator;
    }

    @Override
    public CaptchaType type() {
        return CaptchaType.CURVE;
    }

    @Override
    protected GeneratedCaptcha doGenerate(GenerateRequest request) {
        int width = options.getWidth();
        int height = options.getHeight();
        BufferedImage raw = backgroundProvider.provide(width, height)
                .orElseThrow(() -> new CaptchaException(
                        "没有可用的背景图，请配置 captcha.background.sources 或开启 generate-fallback"));
        BufferedImage image = ImageUtil.cover(raw, width, height);

        List<PointVo> curve = buildCurve(width, height);
        drawGuide(image, curve);

        CaptchaSession session = CaptchaSession.curve(
                request.getId(), width, height, curve,
                options.getExpireSeconds() * 1000);
        GeneratedCaptcha result = new GeneratedCaptcha();
        result.setSession(session);
        result.setImage1(image);
        result.setWidth(width);
        result.setHeight(height);
        result.setShape("curve");
        if (request.isDebug()) {
            result.setDebugCurve(new ArrayList<>(curve));
        }
        return result;
    }

    @Override
    protected VerifyResult doVerify(CaptchaSession session, CaptchaAnswer answer) {
        List<NormalizedPoint> drawn = answer == null ? null : answer.getCurve();
        if (drawn == null || drawn.isEmpty()) {
            return VerifyResult.badRequest(CaptchaMessages.CURVE_MISSING_POINTS, messages);
        }
        Optional<String> behaviorError = behaviorValidator.validate(
                answer.getTd(), answer, session);
        if (behaviorError.isPresent()) {
            return VerifyResult.fail(behaviorError.get(), "BEHAVIOR", messages);
        }
        if (drawn.size() < options.getMinDrawnPoints()) {
            return VerifyResult.fail(CaptchaMessages.CURVE_NOT_ENOUGH_POINTS, "WRONG", messages);
        }
        List<PointVo> expected = session.getCurve();
        if (expected == null || expected.size() < 2) {
            return VerifyResult.badRequest(CaptchaMessages.CURVE_MISSING_POINTS, messages);
        }
        int width = session.getWidth();
        int height = session.getHeight();
        double tolerance = options.getTolerance() / Math.max(1, width);
        double startEndTolerance = tolerance * 2;

        // 起点/终点必须落在期望曲线的首尾标记附近（方向一致性）
        NormalizedPoint first = drawn.getFirst();
        NormalizedPoint last = drawn.getLast();
        double startDist = Math.hypot(
                first.x() - expected.getFirst().getX() / (double) width,
                first.y() - expected.getFirst().getY() / (double) height);
        if (startDist > startEndTolerance) {
            return VerifyResult.fail(CaptchaMessages.CURVE_START_MISMATCH, "WRONG", messages);
        }
        double endDist = Math.hypot(
                last.x() - expected.getLast().getX() / (double) width,
                last.y() - expected.getLast().getY() / (double) height);
        if (endDist > startEndTolerance) {
            return VerifyResult.fail(CaptchaMessages.CURVE_END_MISMATCH, "WRONG", messages);
        }

        // 覆盖率：期望采样点中至少有一个绘制点落在容差内的比例
        int covered = 0;
        for (PointVo point : expected) {
            double ex = point.getX() / (double) width;
            double ey = point.getY() / (double) height;
            boolean hit = false;
            for (NormalizedPoint d : drawn) {
                if (Math.hypot(d.x() - ex, d.y() - ey) <= tolerance) {
                    hit = true;
                    break;
                }
            }
            if (hit) {
                covered++;
            }
        }
        double coverage = (double) covered / expected.size();
        if (coverage < options.getMinCoverage()) {
            return VerifyResult.fail(CaptchaMessages.CURVE_COVERAGE_TOO_LOW, "WRONG", messages);
        }
        return VerifyResult.ok(CaptchaMessages.VERIFY_OK, messages);
    }

    @Override
    protected long minElapsedMs() {
        return options.getMinElapsedMs();
    }

    /** 生成随机控制点，构建平滑曲线并均匀采样 */
    private List<PointVo> buildCurve(int width, int height) {
        int count = Math.max(2, options.getControlPointCount());
        double marginX = width * 0.12;
        double marginY = height * 0.12;
        double minGap = Math.min(width, height) * 0.12;
        List<PointVo> controls = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            for (int attempt = 0; attempt < 30; attempt++) {
                int x = (int) Math.round(marginX + random.nextDouble() * (width - marginX * 2));
                int y = (int) Math.round(marginY + random.nextDouble() * (height - marginY * 2));
                if (controls.isEmpty()
                        || Math.hypot(x - controls.getLast().getX(),
                                y - controls.getLast().getY()) >= minGap) {
                    controls.add(new PointVo(x, y));
                    break;
                }
            }
            if (controls.size() <= i) {
                controls.add(new PointVo(
                        (int) Math.round(marginX + random.nextDouble() * (width - marginX * 2)),
                        (int) Math.round(marginY + random.nextDouble() * (height - marginY * 2))));
            }
        }
        return resample(flatten(smoothPath(controls)), options.getPointCount());
    }

    /** 用 Catmull-Rom 风格的三次贝塞尔段构造经过所有控制点的平滑路径 */
    private Path2D smoothPath(List<PointVo> points) {
        Path2D path = new Path2D.Double();
        path.moveTo(points.getFirst().getX(), points.getFirst().getY());
        for (int i = 0; i < points.size() - 1; i++) {
            PointVo p0 = points.get(Math.max(0, i - 1));
            PointVo p1 = points.get(i);
            PointVo p2 = points.get(i + 1);
            PointVo p3 = points.get(Math.min(points.size() - 1, i + 2));
            double c1x = p1.getX() + (p2.getX() - p0.getX()) / 6.0;
            double c1y = p1.getY() + (p2.getY() - p0.getY()) / 6.0;
            double c2x = p2.getX() - (p3.getX() - p1.getX()) / 6.0;
            double c2y = p2.getY() - (p3.getY() - p1.getY()) / 6.0;
            path.curveTo(c1x, c1y, c2x, c2y, p2.getX(), p2.getY());
        }
        return path;
    }

    /** 把曲线路径按平直度展开为折线顶点 */
    private List<double[]> flatten(Path2D path) {
        List<double[]> vertices = new ArrayList<>();
        PathIterator iterator = path.getPathIterator(null, 0.5);
        double[] segment = new double[6];
        double lastX = 0;
        double lastY = 0;
        while (!iterator.isDone()) {
            int type = iterator.currentSegment(segment);
            if (type == PathIterator.SEG_MOVETO || type == PathIterator.SEG_LINETO) {
                lastX = segment[0];
                lastY = segment[1];
                vertices.add(new double[]{lastX, lastY});
            }
            iterator.next();
        }
        return vertices;
    }

    /** 按弧长把折线重采样为指定数量的等距点 */
    private List<PointVo> resample(List<double[]> vertices, int pointCount) {
        int count = Math.max(2, pointCount);
        double total = 0;
        double[] lengths = new double[vertices.size()];
        for (int i = 1; i < vertices.size(); i++) {
            total += Math.hypot(
                    vertices.get(i)[0] - vertices.get(i - 1)[0],
                    vertices.get(i)[1] - vertices.get(i - 1)[1]);
            lengths[i] = total;
        }
        List<PointVo> result = new ArrayList<>();
        if (total <= 0) {
            for (int i = 0; i < count; i++) {
                result.add(new PointVo(
                        (int) Math.round(vertices.get(0)[0]),
                        (int) Math.round(vertices.get(0)[1])));
            }
            return result;
        }
        int cursor = 0;
        for (int i = 0; i < count; i++) {
            double target = total * i / (count - 1);
            // 保留至少一个后继顶点用于插值，避免最后一个采样点越界
            while (cursor < lengths.length - 2 && lengths[cursor + 1] < target) {
                cursor++;
            }
            int next = Math.min(cursor + 1, lengths.length - 1);
            double start = lengths[cursor];
            double end = lengths[next];
            double ratio = end > start ? (target - start) / (end - start) : 0;
            result.add(new PointVo(
                    (int) Math.round(vertices.get(cursor)[0]
                            + (vertices.get(next)[0] - vertices.get(cursor)[0]) * ratio),
                    (int) Math.round(vertices.get(cursor)[1]
                            + (vertices.get(next)[1] - vertices.get(cursor)[1]) * ratio)));
        }
        return result;
    }

    /** 在背景图上绘制引导曲线与起终点标记 */
    private void drawGuide(BufferedImage image, List<PointVo> curve) {
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        Path2D path = new Path2D.Double();
        path.moveTo(curve.getFirst().getX(), curve.getFirst().getY());
        for (int i = 1; i < curve.size(); i++) {
            path.lineTo(curve.get(i).getX(), curve.get(i).getY());
        }
        // 深色底边保证在任何背景上都可见
        g.setColor(new Color(0, 0, 0, 90));
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(path);
        // 亮色虚线作为“待描绘”的引导路径
        g.setColor(new Color(255, 255, 255, 220));
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                10f, new float[]{10f, 8f}, 0f));
        g.draw(path);

        drawMarker(g, curve.getFirst(), new Color(46, 204, 113));
        drawMarker(g, curve.getLast(), new Color(255, 82, 82));
        g.dispose();
    }

    /** 绘制起/终点圆形标记 */
    private void drawMarker(Graphics2D g, PointVo point, Color color) {
        int radius = 8;
        g.setColor(new Color(0, 0, 0, 100));
        g.fillOval(point.getX() - radius - 1, point.getY() - radius - 1,
                (radius + 1) * 2, (radius + 1) * 2);
        g.setColor(color);
        g.fillOval(point.getX() - radius, point.getY() - radius, radius * 2, radius * 2);
        g.setColor(Color.WHITE);
        g.fillOval(point.getX() - 3, point.getY() - 3, 6, 6);
    }
}
