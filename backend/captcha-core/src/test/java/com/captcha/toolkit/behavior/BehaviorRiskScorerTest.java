package com.captcha.toolkit.behavior;

import com.captcha.toolkit.config.ClientBehaviorConfig;
import com.captcha.toolkit.config.BehaviorConfig;
import com.captcha.toolkit.i18n.CaptchaMessages;
import com.captcha.toolkit.model.CaptchaAnswer;
import com.captcha.toolkit.model.CaptchaSession;
import com.captcha.toolkit.model.NormalizedPoint;
import com.captcha.toolkit.model.PointVo;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 风险评分器单元测试：机器特征（匀速直线、机械点击）应得高分，
 * 拟人特征（变速、减速、抖动、时长波动）应得低分。
 */
class BehaviorRiskScorerTest {

    /** Web 画像（风险阈值 0.65） */
    private final ClientBehaviorConfig web = new ClientBehaviorConfig();

    @Test
    void uniformStraightDragScoresAboveWebThreshold() {
        BehaviorRiskResult result = new DragBehaviorRiskScorer().score(straightDrag(0.5), web);
        assertTrue(result.score() > 0.65, result.features().toString());
    }

    @Test
    void humanLikeDragScoresBelowWebThreshold() {
        BehaviorRiskResult result = new DragBehaviorRiskScorer().score(humanLikeDrag(0.5), web);
        assertTrue(result.score() < 0.4, result.features().toString());
    }

    @Test
    void mechanicalClickScoresAboveWebThreshold() {
        BehaviorRiskResult result = new ClickBehaviorRiskScorer().score(mechanicalClick(), web);
        assertTrue(result.score() > 0.65, result.features().toString());
    }

    @Test
    void variedClickScoresBelowWebThreshold() {
        BehaviorRiskResult result = new ClickBehaviorRiskScorer().score(variedClick(), web);
        assertTrue(result.score() < 0.4, result.features().toString());
    }

    @Test
    void sparseTouchDragStaysBelowTouchThreshold() {
        ClientBehaviorConfig touch = ClientBehaviorConfig.touchDefaults();
        BehaviorRiskResult result = new DragBehaviorRiskScorer().score(sparseDrag(0.5), touch);
        assertTrue(result.score() < touch.getRiskThreshold(), result.features().toString());
    }

    @Test
    void dragValidatorAcceptsScoreEqualToThresholdAndRejectsJustAbove() {
        BehaviorTrace trace = straightDrag(0.5);
        double score = new DragBehaviorRiskScorer().score(trace, web).score();

        BehaviorConfig config = new BehaviorConfig();
        config.setEnabled(true);
        config.setRiskEnabled(true);
        config.setRiskThreshold(score);
        SliderBehaviorValidator validator = new SliderBehaviorValidator(config);
        CaptchaAnswer answer = CaptchaAnswer.slider(0.5);
        answer.setTd(BehaviorTraceCodec.encode(trace));
        CaptchaSession session = CaptchaSession.slider(
                "risk-boundary", "classic", 170, 95, 340, 190, 300_000L);

        assertTrue(validator.validate(answer.getTd(), answer, session).isEmpty(),
                "综合分恰好等于阈值应通过（仅超过阈值才拒绝）");
        config.setRiskThreshold(Math.nextDown(score));
        assertEquals(CaptchaMessages.BEHAVIOR_RISK_TOO_HIGH,
                validator.validate(answer.getTd(), answer, session).orElse(""));
    }

    @Test
    void clickValidatorAcceptsScoreEqualToThresholdAndRejectsJustAbove() {
        BehaviorTrace trace = mechanicalClick();
        double score = new ClickBehaviorRiskScorer().score(trace, web).score();

        BehaviorConfig config = new BehaviorConfig();
        config.setEnabled(true);
        config.setRiskEnabled(true);
        config.setRiskThreshold(score);
        ClickBehaviorValidator validator = new ClickBehaviorValidator(config);
        CaptchaAnswer answer = CaptchaAnswer.click(List.of(
                new NormalizedPoint(0.3, 0.4),
                new NormalizedPoint(0.6, 0.7),
                new NormalizedPoint(0.5, 0.2)));
        answer.setTd(BehaviorTraceCodec.encode(trace));
        CaptchaSession session = CaptchaSession.click(
                "risk-boundary-click", 340, 190,
                List.of(new PointVo(102, 76), new PointVo(204, 133), new PointVo(170, 38)),
                List.of("测", "试", "验"), 300_000L);

        assertTrue(validator.validate(answer.getTd(), answer, session).isEmpty(),
                "综合分恰好等于阈值应通过（仅超过阈值才拒绝）");
        config.setRiskThreshold(Math.nextDown(score));
        assertEquals(CaptchaMessages.BEHAVIOR_RISK_TOO_HIGH,
                validator.validate(answer.getTd(), answer, session).orElse(""));
    }

    /** 匀速直线拖拽：每一步速度完全相同，路径绝对平直 */
    private static BehaviorTrace straightDrag(double endX) {
        List<BehaviorPoint> points = new ArrayList<>();
        points.add(new BehaviorPoint(0, 0.01, 0.5, BehaviorEventType.START));
        for (int i = 1; i <= 8; i++) {
            double x = 0.01 + (endX - 0.01) * (i / 8.0);
            points.add(new BehaviorPoint(i * 100, x, 0.5, BehaviorEventType.MOVE));
        }
        points.add(new BehaviorPoint(1000, endX, 0.5, BehaviorEventType.UP));
        return new BehaviorTrace(1, 340, 190, 1_000_000L, 1_001_000L, points);
    }

    /** 拟人拖拽：起始停顿、先加速后减速、轻微纵向抖动、末端过冲回拉 */
    private static BehaviorTrace humanLikeDrag(double endX) {
        List<BehaviorPoint> points = new ArrayList<>();
        points.add(new BehaviorPoint(0, 0.01, 0.5, BehaviorEventType.START));
        int time = 180;
        double[] ratios = {0.02, 0.06, 0.13, 0.23, 0.36, 0.52, 0.68, 0.81, 0.9, 0.96, 1.0};
        for (int i = 1; i < ratios.length; i++) {
            time += 16 + (i % 3) * 3;
            double y = 0.5 + Math.sin(i * 0.7) * 0.008;
            points.add(new BehaviorPoint(time,
                    0.01 + (endX - 0.01) * ratios[i], y, BehaviorEventType.MOVE));
        }
        time += 30;
        points.add(new BehaviorPoint(time, endX + 0.015, 0.5, BehaviorEventType.MOVE));
        time += 24;
        points.add(new BehaviorPoint(time, endX, 0.5, BehaviorEventType.MOVE));
        time += 40;
        points.add(new BehaviorPoint(time, endX, 0.5, BehaviorEventType.UP));
        return new BehaviorTrace(1, 340, 190, 1_000_000L, 1_000_000L + time, points);
    }

    /** 机械点选：目标间匀速直线移动、每次按下时长与点击间隔完全一致 */
    private static BehaviorTrace mechanicalClick() {
        double[][] targets = {{0.3, 0.4}, {0.6, 0.7}, {0.5, 0.2}};
        List<BehaviorPoint> points = new ArrayList<>();
        int time = 0;
        double cursorX = targets[0][0];
        double cursorY = targets[0][1];
        points.add(new BehaviorPoint(time, cursorX, cursorY, BehaviorEventType.START));
        for (double[] target : targets) {
            for (int i = 1; i <= 5; i++) {
                time += 10;
                double ratio = i / 5.0;
                points.add(new BehaviorPoint(time,
                        cursorX + (target[0] - cursorX) * ratio,
                        cursorY + (target[1] - cursorY) * ratio,
                        BehaviorEventType.MOVE));
            }
            points.add(new BehaviorPoint(time, target[0], target[1], BehaviorEventType.DOWN));
            time += 80;
            points.add(new BehaviorPoint(time, target[0], target[1], BehaviorEventType.UP));
            cursorX = target[0];
            cursorY = target[1];
        }
        return new BehaviorTrace(1, 340, 190, 1_000_000L, 1_000_000L + time, points);
    }

    /** 拟人点选：减速接近、时长与间隔各有波动、带轻微坐标抖动 */
    private static BehaviorTrace variedClick() {
        double[][] targets = {{0.3, 0.4}, {0.6, 0.7}, {0.5, 0.2}};
        int[] steps = {5, 7, 9};
        int[] dwells = {90, 150, 60};
        List<BehaviorPoint> points = new ArrayList<>();
        int time = 0;
        double cursorX = targets[0][0];
        double cursorY = targets[0][1];
        points.add(new BehaviorPoint(time, cursorX, cursorY, BehaviorEventType.START));
        for (int i = 0; i < targets.length; i++) {
            double targetX = targets[i][0];
            double targetY = targets[i][1];
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
        return new BehaviorTrace(1, 340, 190, 1_000_000L, 1_000_000L + time, points);
    }

    /** 稀疏触摸拖拽：只有两次大跨度移动（H5 常见采样） */
    private static BehaviorTrace sparseDrag(double endX) {
        return new BehaviorTrace(1, 340, 190, 1_000_000L, 1_001_000L, List.of(
                new BehaviorPoint(0, 0.01, 0.5, BehaviorEventType.START),
                new BehaviorPoint(100, 0.7, 0.5, BehaviorEventType.MOVE),
                new BehaviorPoint(800, endX, 0.5, BehaviorEventType.MOVE),
                new BehaviorPoint(1000, endX, 0.5, BehaviorEventType.UP)));
    }
}
