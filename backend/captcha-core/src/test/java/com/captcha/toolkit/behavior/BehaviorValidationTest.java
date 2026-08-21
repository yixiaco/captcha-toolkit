package com.captcha.toolkit.behavior;

import com.captcha.toolkit.CaptchaEngine;
import com.captcha.toolkit.CaptchaType;
import com.captcha.toolkit.config.CaptchaConfig;
import com.captcha.toolkit.image.CaptchaImageCodec;
import com.captcha.toolkit.image.DataUriImageCodec;
import com.captcha.toolkit.model.CaptchaAnswer;
import com.captcha.toolkit.model.CaptchaChallenge;
import com.captcha.toolkit.model.NormalizedPoint;
import com.captcha.toolkit.model.PointVo;
import com.captcha.toolkit.model.VerifyResult;
import com.captcha.toolkit.render.FallbackBackgroundProvider;
import com.captcha.toolkit.render.SceneBackgroundProvider;
import com.captcha.toolkit.store.InMemoryCaptchaSessionStore;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 行为校验引擎级测试：开启行为校验后，三种类型必须提交与答案一致的 td 轨迹。
 */
class BehaviorValidationTest {

    @Test
    void sliderPassesWithMatchingTrace() {
        CaptchaEngine engine = newEngine();
        CaptchaChallenge challenge = engine.create(CaptchaType.SLIDER,
                Map.of("shape", "classic"), true);
        int width = challenge.getWidth();
        double endX = challenge.getDebugX() / (double) width;

        CaptchaAnswer answer = CaptchaAnswer.slider(challenge.getDebugX() / (double) width);
        answer.setTd(sliderTrace(endX));

        VerifyResult result = engine.verify(challenge.getId(), answer);
        assertTrue(result.isSuccess(), result.getMessage());
    }

    @Test
    void sliderRejectsTraceWithoutRelease() {
        CaptchaEngine engine = newEngine();
        CaptchaChallenge challenge = engine.create(CaptchaType.SLIDER,
                Map.of("shape", "classic"), true);
        int width = challenge.getWidth();
        double endX = challenge.getDebugX() / (double) width;

        CaptchaAnswer answer = CaptchaAnswer.slider(challenge.getDebugX() / (double) width);
        answer.setTd(BehaviorTraceCodec.encode(new BehaviorTrace(1, width, challenge.getHeight(),
                1_000_000L, 1_002_000L, List.of(
                        new BehaviorPoint(0, 0.01, 0.5, BehaviorEventType.START),
                        new BehaviorPoint(500, 0.4, 0.5, BehaviorEventType.MOVE),
                        new BehaviorPoint(1000, endX, 0.5, BehaviorEventType.MOVE)))));

        VerifyResult result = engine.verify(challenge.getId(), answer);
        assertFalse(result.isSuccess());
        assertEquals("BEHAVIOR", result.getCode());
    }

    @Test
    void sliderRejectsMissingTraceWhenEnabled() {
        CaptchaEngine engine = newEngine();
        CaptchaChallenge challenge = engine.create(CaptchaType.SLIDER,
                Map.of("shape", "classic"), true);

        CaptchaAnswer answer = CaptchaAnswer.slider(
                challenge.getDebugX() / (double) challenge.getWidth());

        VerifyResult result = engine.verify(challenge.getId(), answer);
        assertFalse(result.isSuccess());
        assertEquals("BEHAVIOR", result.getCode());
    }

    @Test
    void clickPassesWithMatchingTrace() {
        CaptchaEngine engine = newEngine();
        CaptchaChallenge challenge = engine.create(CaptchaType.CLICK, Map.of(), true);
        List<PointVo> targets = serverTargets(challenge);
        List<NormalizedPoint> points = normalizedPoints(challenge);

        CaptchaAnswer answer = CaptchaAnswer.click(points);
        answer.setTd(clickTrace(targets, challenge.getWidth(), challenge.getHeight()));

        VerifyResult result = engine.verify(challenge.getId(), answer);
        assertTrue(result.isSuccess(), result.getMessage());
    }

    @Test
    void clickRejectsTraceWithWrongOrder() {
        CaptchaEngine engine = newEngine();
        CaptchaChallenge challenge = engine.create(CaptchaType.CLICK, Map.of(), true);
        List<PointVo> targets = serverTargets(challenge);
        List<PointVo> reversedTargets = new ArrayList<>(targets);
        Collections.swap(reversedTargets, 0, reversedTargets.size() - 1);
        List<NormalizedPoint> points = normalizedPoints(challenge);

        CaptchaAnswer answer = CaptchaAnswer.click(points);
        answer.setTd(clickTrace(reversedTargets, challenge.getWidth(), challenge.getHeight()));

        VerifyResult result = engine.verify(challenge.getId(), answer);
        assertFalse(result.isSuccess());
        assertEquals("BEHAVIOR", result.getCode());
    }

    @Test
    void rotatePassesWithDragTrace() {
        CaptchaEngine engine = newEngine();
        CaptchaChallenge challenge = engine.create(CaptchaType.ROTATE, Map.of(), true);

        CaptchaAnswer answer = CaptchaAnswer.rotate(challenge.getDebugAngle());
        answer.setTd(sliderTrace(0.5));

        VerifyResult result = engine.verify(challenge.getId(), answer);
        assertTrue(result.isSuccess(), result.getMessage());
    }

    @Test
    void h5ProfileAllowsSparseTouchTrace() {
        CaptchaEngine engine = newEngine();
        CaptchaChallenge challenge = engine.create(CaptchaType.SLIDER,
                Map.of("shape", "classic"), true);
        int width = challenge.getWidth();
        double endX = challenge.getDebugX() / (double) width;

        CaptchaAnswer answer = CaptchaAnswer.slider(challenge.getDebugX() / (double) width);
        answer.setClientType("h5");
        answer.setTd(sparseSliderTrace(endX));

        VerifyResult result = engine.verify(challenge.getId(), answer);
        assertTrue(result.isSuccess(), result.getMessage());
    }

    @Test
    void webProfileRejectsSparseTrace() {
        CaptchaEngine engine = newEngine();
        CaptchaChallenge challenge = engine.create(CaptchaType.SLIDER,
                Map.of("shape", "classic"), true);
        int width = challenge.getWidth();
        double endX = challenge.getDebugX() / (double) width;

        CaptchaAnswer answer = CaptchaAnswer.slider(challenge.getDebugX() / (double) width);
        answer.setClientType("web");
        answer.setTd(sparseSliderTrace(endX));

        VerifyResult result = engine.verify(challenge.getId(), answer);
        assertFalse(result.isSuccess());
        assertEquals("BEHAVIOR", result.getCode());
    }

    @Test
    void sliderRejectsUniformStraightTraceWhenRiskEnabled() {
        CaptchaEngine engine = newEngineWithRisk();
        CaptchaChallenge challenge = engine.create(CaptchaType.SLIDER,
                Map.of("shape", "classic"), true);
        int width = challenge.getWidth();
        double endX = challenge.getDebugX() / (double) width;

        CaptchaAnswer answer = CaptchaAnswer.slider(challenge.getDebugX() / (double) width);
        answer.setTd(sliderTrace(endX));

        VerifyResult result = engine.verify(challenge.getId(), answer);
        assertFalse(result.isSuccess());
        assertEquals("BEHAVIOR", result.getCode());
    }

    @Test
    void sliderPassesHumanLikeTraceWhenRiskEnabled() {
        CaptchaEngine engine = newEngineWithRisk();
        CaptchaChallenge challenge = engine.create(CaptchaType.SLIDER,
                Map.of("shape", "classic"), true);
        int width = challenge.getWidth();
        double endX = challenge.getDebugX() / (double) width;

        CaptchaAnswer answer = CaptchaAnswer.slider(challenge.getDebugX() / (double) width);
        answer.setTd(humanLikeSliderTrace(endX));

        VerifyResult result = engine.verify(challenge.getId(), answer);
        assertTrue(result.isSuccess(), result.getMessage());
    }

    @Test
    void clickRejectsMechanicalTraceWhenRiskEnabled() {
        CaptchaEngine engine = newEngineWithRisk();
        CaptchaChallenge challenge = engine.create(CaptchaType.CLICK, Map.of(), true);
        List<PointVo> targets = serverTargets(challenge);

        CaptchaAnswer answer = CaptchaAnswer.click(normalizedPoints(challenge));
        answer.setTd(mechanicalClickTrace(targets,
                challenge.getWidth(), challenge.getHeight()));

        VerifyResult result = engine.verify(challenge.getId(), answer);
        assertFalse(result.isSuccess());
        assertEquals("BEHAVIOR", result.getCode());
    }

    @Test
    void clickPassesVariedTraceWhenRiskEnabled() {
        CaptchaEngine engine = newEngineWithRisk();
        CaptchaChallenge challenge = engine.create(CaptchaType.CLICK, Map.of(), true);
        List<PointVo> targets = serverTargets(challenge);

        CaptchaAnswer answer = CaptchaAnswer.click(normalizedPoints(challenge));
        answer.setTd(humanLikeClickTrace(targets,
                challenge.getWidth(), challenge.getHeight()));

        VerifyResult result = engine.verify(challenge.getId(), answer);
        assertTrue(result.isSuccess(), result.getMessage());
    }

    @Test
    void h5SparseTracePassesRiskScoring() {
        CaptchaEngine engine = newEngineWithRisk();
        CaptchaChallenge challenge = engine.create(CaptchaType.SLIDER,
                Map.of("shape", "classic"), true);
        int width = challenge.getWidth();
        double endX = challenge.getDebugX() / (double) width;

        CaptchaAnswer answer = CaptchaAnswer.slider(challenge.getDebugX() / (double) width);
        answer.setClientType("h5");
        answer.setTd(sparseSliderTrace(endX));

        VerifyResult result = engine.verify(challenge.getId(), answer);
        assertTrue(result.isSuccess(), result.getMessage());
    }

    @Test
    void curvePassesWithMatchingTrace() {
        CaptchaEngine engine = newEngine();
        CaptchaChallenge challenge = engine.create(CaptchaType.CURVE, Map.of(), true);
        List<PointVo> curve = challenge.getDebugCurve();

        CaptchaAnswer answer = CaptchaAnswer.curve(
                curve.stream()
                        .map(p -> new NormalizedPoint(
                                p.getX() / (double) challenge.getWidth(),
                                p.getY() / (double) challenge.getHeight()))
                        .toList());
        answer.setTd(curveTrace(curve, challenge.getWidth(), challenge.getHeight()));

        VerifyResult result = engine.verify(challenge.getId(), answer);
        assertTrue(result.isSuccess(), result.getMessage());
    }

    @Test
    void curveRejectsMissingTraceWhenEnabled() {
        CaptchaEngine engine = newEngine();
        CaptchaChallenge challenge = engine.create(CaptchaType.CURVE, Map.of(), true);

        CaptchaAnswer answer = CaptchaAnswer.curve(
                challenge.getDebugCurve().stream()
                        .map(p -> new NormalizedPoint(
                                p.getX() / (double) challenge.getWidth(),
                                p.getY() / (double) challenge.getHeight()))
                        .toList());

        VerifyResult result = engine.verify(challenge.getId(), answer);
        assertFalse(result.isSuccess());
        assertEquals("BEHAVIOR", result.getCode());
    }

    private CaptchaEngine newEngine() {
        return newEngine(false);
    }

    /** 开启第二层风险评分的引擎 */
    private CaptchaEngine newEngineWithRisk() {
        return newEngine(true);
    }

    private CaptchaEngine newEngine(boolean riskEnabled) {
        CaptchaConfig config = new CaptchaConfig();
        config.setDebugEnabled(true);
        config.getBehavior().setEnabled(true);
        config.getBehavior().setRiskEnabled(riskEnabled);
        config.getSlider().setMinElapsedMs(0);
        config.getClick().setMinElapsedMs(0);
        config.getRotate().setMinElapsedMs(0);
        config.getCurve().setMinElapsedMs(0);
        FallbackBackgroundProvider background = new FallbackBackgroundProvider(
                List.of(new SceneBackgroundProvider()));
        return CaptchaEngine.of(config, new InMemoryCaptchaSessionStore(),
                new DataUriImageCodec(), List.of(), background);
    }

    private static List<NormalizedPoint> normalizedPoints(CaptchaChallenge challenge) {
        return challenge.getDebugTargets().stream()
                .map(p -> new NormalizedPoint(
                        p.getX() / (double) challenge.getWidth(),
                        p.getY() / (double) challenge.getHeight()))
                .toList();
    }

    private static List<PointVo> serverTargets(CaptchaChallenge challenge) {
        return challenge.getDebugTargets().stream()
                .map(p -> new PointVo(p.getX(), p.getY()))
                .toList();
    }

    /** 生成一条 1 秒的滑块拖拽轨迹，终点归一化 x = endX */
    private static String sliderTrace(double endX) {
        List<BehaviorPoint> points = new ArrayList<>();
        points.add(new BehaviorPoint(0, 0.01, 0.5, BehaviorEventType.START));
        // 逐步插值到终点，避免答案靠左/靠右时最后一步跳跃超过阈值
        for (int i = 1; i <= 8; i++) {
            double x = 0.01 + (endX - 0.01) * (i / 8.0);
            points.add(new BehaviorPoint(i * 100, x, 0.5, BehaviorEventType.MOVE));
        }
        points.add(new BehaviorPoint(1000, endX, 0.5, BehaviorEventType.UP));
        return BehaviorTraceCodec.encode(new BehaviorTrace(
                1, 340, 190, 1_000_000L, 1_001_000L, points));
    }

    /** 生成一条采样稀疏的触摸轨迹：一次大跨度移动，H5 可接受、Web 会被拒绝 */
    private static String sparseSliderTrace(double endX) {
        List<BehaviorPoint> points = List.of(
                new BehaviorPoint(0, 0.01, 0.5, BehaviorEventType.START),
                new BehaviorPoint(100, 0.7, 0.5, BehaviorEventType.MOVE),
                new BehaviorPoint(800, endX, 0.5, BehaviorEventType.MOVE),
                new BehaviorPoint(1000, endX, 0.5, BehaviorEventType.UP));
        return BehaviorTraceCodec.encode(new BehaviorTrace(
                1, 340, 190, 1_000_000L, 1_001_000L, points));
    }

    /** 生成与点选答案匹配的轨迹：每个目标一次按下/松开，点击之间带移动 */
    private static String clickTrace(List<PointVo> targets, int width, int height) {
        List<BehaviorPoint> points = new ArrayList<>();
        int time = 0;
        double cursorX = targets.getFirst().getX() / (double) width;
        double cursorY = targets.getFirst().getY() / (double) height;
        points.add(new BehaviorPoint(time, cursorX, cursorY, BehaviorEventType.START));

        for (PointVo target : targets) {
            double targetX = target.getX() / (double) width;
            double targetY = target.getY() / (double) height;
            move(points, time, cursorX, cursorY, targetX, targetY, 5);
            time = points.getLast().timeMs();
            points.add(new BehaviorPoint(time, targetX, targetY, BehaviorEventType.DOWN));
            points.add(new BehaviorPoint(time + 80, targetX, targetY, BehaviorEventType.UP));
            time += 80;
            cursorX = targetX;
            cursorY = targetY;
        }
        return BehaviorTraceCodec.encode(new BehaviorTrace(
                1, width, height, 1_000_000L, 1_000_000L + time, points));
    }

    private static void move(List<BehaviorPoint> points, int startTime,
                             double x0, double y0, double x1, double y1, int steps) {
        for (int i = 1; i <= steps; i++) {
            double ratio = i / (double) steps;
            points.add(new BehaviorPoint(startTime + i * 10,
                    x0 + (x1 - x0) * ratio,
                    y0 + (y1 - y0) * ratio,
                    BehaviorEventType.MOVE));
        }
    }

    /** 生成一条“机械”点选轨迹：每段匀速移动、按下时长与相邻点击间隔完全一致 */
    private static String mechanicalClickTrace(List<PointVo> targets, int width, int height) {
        List<BehaviorPoint> points = new ArrayList<>();
        int time = 0;
        int nextDown = 0;
        double cursorX = targets.getFirst().getX() / (double) width;
        double cursorY = targets.getFirst().getY() / (double) height;
        points.add(new BehaviorPoint(time, cursorX, cursorY, BehaviorEventType.START));

        for (int i = 0; i < targets.size(); i++) {
            PointVo target = targets.get(i);
            double targetX = target.getX() / (double) width;
            double targetY = target.getY() / (double) height;
            double distance = Math.hypot(targetX - cursorX, targetY - cursorY);
            int steps = 6;
            int moveDuration = 0;
            int[] dts = new int[steps];
            // 时间按距离等比分配，保证每段移动速度完全一致
            for (int s = 0; s < steps; s++) {
                dts[s] = (int) Math.max(1, Math.round(distance * 100 / steps));
                moveDuration += dts[s];
            }
            if (i == 0) {
                time = 0;
            } else {
                // 等待到固定的按下时刻，使相邻点击间隔一致
                time = nextDown - moveDuration;
                // 等待期间原地停留，避免把“等待”算成一段慢速移动
                points.add(new BehaviorPoint(
                        time, cursorX, cursorY, BehaviorEventType.MOVE));
            }
            for (int s = 0; s < steps; s++) {
                time += dts[s];
                double ratio = (s + 1) / (double) steps;
                points.add(new BehaviorPoint(time,
                        cursorX + (targetX - cursorX) * ratio,
                        cursorY + (targetY - cursorY) * ratio,
                        BehaviorEventType.MOVE));
            }
            points.add(new BehaviorPoint(time, targetX, targetY, BehaviorEventType.DOWN));
            if (i == 0) {
                nextDown = time + 300;
            } else {
                nextDown += 300;
            }
            time += 80;
            points.add(new BehaviorPoint(time, targetX, targetY, BehaviorEventType.UP));
            cursorX = targetX;
            cursorY = targetY;
        }
        return BehaviorTraceCodec.encode(new BehaviorTrace(
                1, width, height, 1_000_000L, 1_000_000L + time, points));
    }

    /** 生成一条“拟人”滑块轨迹：起始停顿、变速、末端减速、轻微摆动与过冲修正 */
    private static String humanLikeSliderTrace(double endX) {
        List<BehaviorPoint> points = new ArrayList<>();
        points.add(new BehaviorPoint(0, 0.01, 0.5, BehaviorEventType.START));
        int time = 180;
        double[] ratios = {0.02, 0.06, 0.13, 0.23, 0.36, 0.52, 0.68, 0.81, 0.9, 0.96, 1.0};
        for (int i = 1; i < ratios.length; i++) {
            time += 16 + (i % 3) * 3;
            points.add(new BehaviorPoint(time,
                    0.01 + (endX - 0.01) * ratios[i],
                    0.5 + Math.sin(i * 0.7) * 0.008,
                    BehaviorEventType.MOVE));
        }
        time += 30;
        points.add(new BehaviorPoint(time, endX + 0.015, 0.5, BehaviorEventType.MOVE));
        time += 24;
        points.add(new BehaviorPoint(time, endX, 0.5, BehaviorEventType.MOVE));
        time += 40;
        points.add(new BehaviorPoint(time, endX, 0.5, BehaviorEventType.UP));
        return BehaviorTraceCodec.encode(new BehaviorTrace(
                1, 340, 190, 1_000_000L, 1_000_000L + time, points));
    }

    /** 生成一条“拟人”点选轨迹：点击间减速接近、时长与间隔有波动 */
    private static String humanLikeClickTrace(List<PointVo> targets, int width, int height) {
        List<BehaviorPoint> points = new ArrayList<>();
        int time = 0;
        double cursorX = targets.getFirst().getX() / (double) width;
        double cursorY = targets.getFirst().getY() / (double) height;
        points.add(new BehaviorPoint(time, cursorX, cursorY, BehaviorEventType.START));
        int[] steps = {5, 7, 9};
        int[] dwells = {90, 150, 60};
        for (int i = 0; i < targets.size(); i++) {
            PointVo target = targets.get(i);
            double targetX = target.getX() / (double) width;
            double targetY = target.getY() / (double) height;
            int stepCount = steps[i % steps.length];
            for (int s = 1; s <= stepCount; s++) {
                time += 12 + (s % 3) * 4;
                double eased = Math.pow(s / (double) stepCount, 2)
                        * (3 - 2 * (s / (double) stepCount));
                points.add(new BehaviorPoint(time,
                        cursorX + (targetX - cursorX) * eased + Math.sin(s) * 0.004,
                        cursorY + (targetY - cursorY) * eased + Math.cos(s) * 0.004,
                        BehaviorEventType.MOVE));
            }
            time += 20;
            points.add(new BehaviorPoint(time, targetX, targetY, BehaviorEventType.DOWN));
            time += dwells[i % dwells.length];
            points.add(new BehaviorPoint(time, targetX, targetY, BehaviorEventType.UP));
            cursorX = targetX;
            cursorY = targetY;
        }
        return BehaviorTraceCodec.encode(new BehaviorTrace(
                1, width, height, 1_000_000L, 1_000_000L + time, points));
    }

    /** 生成一条沿期望曲线逐点移动的绘制轨迹：起点按下、中间移动、终点松开 */
    private static String curveTrace(List<PointVo> curve, int width, int height) {
        List<BehaviorPoint> points = new ArrayList<>();
        for (int i = 0; i < curve.size(); i++) {
            double x = curve.get(i).getX() / (double) width;
            double y = curve.get(i).getY() / (double) height;
            BehaviorEventType type = i == 0 ? BehaviorEventType.START
                    : i == curve.size() - 1 ? BehaviorEventType.UP
                    : BehaviorEventType.MOVE;
            points.add(new BehaviorPoint(i * 30, x, y, type));
        }
        return BehaviorTraceCodec.encode(new BehaviorTrace(
                1, width, height, 1_000_000L,
                1_000_000L + (curve.size() - 1) * 30L, points));
    }
}
