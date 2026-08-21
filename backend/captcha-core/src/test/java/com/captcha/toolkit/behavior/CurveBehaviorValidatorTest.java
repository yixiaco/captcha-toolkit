package com.captcha.toolkit.behavior;

import com.captcha.toolkit.config.BehaviorConfig;
import com.captcha.toolkit.model.CaptchaAnswer;
import com.captcha.toolkit.model.CaptchaSession;
import com.captcha.toolkit.model.NormalizedPoint;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 曲线行为校验器测试：事件序列规则与“轨迹-答案”首尾一致性。
 */
class CurveBehaviorValidatorTest {

    /** 开启行为校验的校验器 */
    private static CurveBehaviorValidator enabledValidator() {
        BehaviorConfig config = new BehaviorConfig();
        config.setEnabled(true);
        return new CurveBehaviorValidator(config);
    }

    /** 构造一个曲线会话（行为校验不读取会话曲线，只读答案） */
    private static CaptchaSession session() {
        return CaptchaSession.curve("curve-behavior", 340, 190, List.of(), 300_000L);
    }

    /** 构造答案：首尾分别固定为 (0.1, 0.1) / (0.9, 0.9)，中间插值 */
    private static CaptchaAnswer answer() {
        List<NormalizedPoint> curve = new ArrayList<>();
        for (int i = 0; i <= 8; i++) {
            double ratio = i / 8.0;
            curve.add(new NormalizedPoint(0.1 + 0.8 * ratio, 0.1 + 0.8 * ratio));
        }
        return CaptchaAnswer.curve(curve);
    }

    /** 构造一条从 (0.1,0.1) 到 (0.9,0.9) 的完整绘制轨迹 */
    private static String validTrace() {
        List<BehaviorPoint> points = new ArrayList<>();
        for (int i = 0; i <= 8; i++) {
            double ratio = i / 8.0;
            BehaviorEventType type = i == 0 ? BehaviorEventType.START
                    : i == 8 ? BehaviorEventType.UP : BehaviorEventType.MOVE;
            points.add(new BehaviorPoint(i * 100, 0.1 + 0.8 * ratio,
                    0.1 + 0.8 * ratio, type));
        }
        return BehaviorTraceCodec.encode(new BehaviorTrace(
                1, 340, 190, 1_000_000L, 1_000_800L, points));
    }

    /** 构造一条事件序列为 first → middle → last 的轨迹（坐标不关心） */
    private static String trace(BehaviorEventType first, BehaviorEventType middle,
                                BehaviorEventType last) {
        List<BehaviorPoint> points = List.of(
                new BehaviorPoint(0, 0.1, 0.1, first),
                new BehaviorPoint(100, 0.25, 0.25, middle),
                new BehaviorPoint(200, 0.4, 0.4, last));
        return BehaviorTraceCodec.encode(new BehaviorTrace(
                1, 340, 190, 1_000_000L, 1_000_200L, points));
    }

    @Test
    void passesCompleteDrawTrace() {
        CaptchaAnswer answer = answer();
        answer.setTd(validTrace());
        assertTrue(enabledValidator().validate(answer.getTd(), answer, session()).isEmpty());
    }

    @Test
    void rejectsTraceNotStartingWithPress() {
        CaptchaAnswer answer = answer();
        answer.setTd(trace(BehaviorEventType.MOVE, BehaviorEventType.MOVE,
                BehaviorEventType.UP));
        Optional<String> error = enabledValidator().validate(answer.getTd(), answer, session());
        assertEquals("curve.expected-start", error.orElse(""));
    }

    @Test
    void rejectsTraceNotEndingWithRelease() {
        CaptchaAnswer answer = answer();
        answer.setTd(trace(BehaviorEventType.START, BehaviorEventType.MOVE,
                BehaviorEventType.MOVE));
        Optional<String> error = enabledValidator().validate(answer.getTd(), answer, session());
        assertEquals("curve.expected-release", error.orElse(""));
    }

    @Test
    void rejectsClickEventInTrace() {
        List<BehaviorPoint> points = List.of(
                new BehaviorPoint(0, 0.1, 0.1, BehaviorEventType.START),
                new BehaviorPoint(100, 0.25, 0.25, BehaviorEventType.MOVE),
                new BehaviorPoint(200, 0.4, 0.4, BehaviorEventType.DOWN),
                new BehaviorPoint(300, 0.55, 0.55, BehaviorEventType.UP));
        CaptchaAnswer answer = answer();
        answer.setTd(BehaviorTraceCodec.encode(new BehaviorTrace(
                1, 340, 190, 1_000_000L, 1_000_300L, points)));
        Optional<String> error = enabledValidator().validate(answer.getTd(), answer, session());
        assertEquals("curve.click-not-allowed", error.orElse(""));
    }

    @Test
    void rejectsTraceWithoutMovement() {
        List<BehaviorPoint> points = List.of(
                new BehaviorPoint(0, 0.1, 0.1, BehaviorEventType.START),
                new BehaviorPoint(100, 0.25, 0.25, BehaviorEventType.UP),
                new BehaviorPoint(200, 0.4, 0.4, BehaviorEventType.UP));
        CaptchaAnswer answer = answer();
        answer.setTd(BehaviorTraceCodec.encode(new BehaviorTrace(
                1, 340, 190, 1_000_000L, 1_000_200L, points)));
        Optional<String> error = enabledValidator().validate(answer.getTd(), answer, session());
        assertEquals("curve.missing-move", error.orElse(""));
    }

    @Test
    void rejectsAnswerStartMismatch() {
        CaptchaAnswer answer = answer();
        answer.setTd(validTrace());
        // 答案起点与轨迹起点相差超过容差
        List<NormalizedPoint> curve = new ArrayList<>(answer.getCurve());
        curve.set(0, new NormalizedPoint(0.5, 0.5));
        answer.setCurve(curve);

        Optional<String> error = enabledValidator().validate(answer.getTd(), answer, session());
        assertEquals("curve.start-mismatch", error.orElse(""));
    }

    @Test
    void rejectsAnswerEndMismatch() {
        CaptchaAnswer answer = answer();
        answer.setTd(validTrace());
        List<NormalizedPoint> curve = new ArrayList<>(answer.getCurve());
        curve.set(curve.size() - 1, new NormalizedPoint(0.5, 0.5));
        answer.setCurve(curve);

        Optional<String> error = enabledValidator().validate(answer.getTd(), answer, session());
        assertEquals("curve.end-mismatch", error.orElse(""));
    }

    @Test
    void rejectsEmptyAnswerCurve() {
        CaptchaAnswer answer = CaptchaAnswer.curve(List.of());
        answer.setTd(validTrace());
        Optional<String> error = enabledValidator().validate(answer.getTd(), answer, session());
        assertEquals("curve.missing-points", error.orElse(""));
    }
}
