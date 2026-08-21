package com.captcha.toolkit.behavior;

import com.captcha.toolkit.config.BehaviorConfig;
import com.captcha.toolkit.model.CaptchaAnswer;
import com.captcha.toolkit.model.CaptchaSession;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 滑块摆动图块行为校验器测试：拖拽事件序列与“轨迹终点-滑块位置”一致性。
 */
class SwingTileBehaviorValidatorTest {

    /** 开启行为校验的校验器 */
    private static SwingTileBehaviorValidator enabledValidator() {
        BehaviorConfig config = new BehaviorConfig();
        config.setEnabled(true);
        return new SwingTileBehaviorValidator(config);
    }

    /** 构造滑块摆动图块会话 */
    private static CaptchaSession session() {
        return CaptchaSession.swingTile("swing-tile-behavior", 340, 190, 5000, 300_000L);
    }

    /** 构造一条终点 x = endX 的拖拽轨迹 */
    private static String dragTrace(double endX) {
        List<BehaviorPoint> points = new ArrayList<>();
        points.add(new BehaviorPoint(0, 0.01, 0.5, BehaviorEventType.START));
        for (int i = 1; i <= 8; i++) {
            double x = 0.01 + (endX - 0.01) * (i / 8.0);
            points.add(new BehaviorPoint(i * 100, x, 0.5, BehaviorEventType.MOVE));
        }
        points.add(new BehaviorPoint(1000, endX, 0.5, BehaviorEventType.UP));
        return BehaviorTraceCodec.encode(new BehaviorTrace(
                1, 340, 190, 1_000_000L, 1_001_000L, points));
    }

    @Test
    void passesCompleteDragTrace() {
        CaptchaAnswer answer = CaptchaAnswer.slider(1.0);
        answer.setTd(dragTrace(1.0));
        assertTrue(enabledValidator().validate(answer.getTd(), answer, session()).isEmpty());
    }

    @Test
    void rejectsTraceNotStartingWithPress() {
        CaptchaAnswer answer = CaptchaAnswer.slider(1.0);
        answer.setTd(dragTrace(1.0).replace("0,0.01,0.5,0", "0,0.01,0.5,1"));
        Optional<String> error = enabledValidator().validate(answer.getTd(), answer, session());
        assertEquals("slider.expected-start", error.orElse(""));
    }

    @Test
    void rejectsClickEventInTrace() {
        List<BehaviorPoint> points = List.of(
                new BehaviorPoint(0, 0.01, 0.5, BehaviorEventType.START),
                new BehaviorPoint(100, 0.3, 0.5, BehaviorEventType.MOVE),
                new BehaviorPoint(200, 0.5, 0.5, BehaviorEventType.DOWN),
                new BehaviorPoint(300, 1.0, 0.5, BehaviorEventType.UP));
        CaptchaAnswer answer = CaptchaAnswer.slider(1.0);
        answer.setTd(BehaviorTraceCodec.encode(new BehaviorTrace(
                1, 340, 190, 1_000_000L, 1_000_300L, points)));
        Optional<String> error = enabledValidator().validate(answer.getTd(), answer, session());
        assertEquals("slider.click-not-allowed", error.orElse(""));
    }

    @Test
    void rejectsEndMismatch() {
        CaptchaAnswer answer = CaptchaAnswer.slider(0.9);
        answer.setTd(dragTrace(1.0));
        Optional<String> error = enabledValidator().validate(answer.getTd(), answer, session());
        assertEquals("slider.end-mismatch", error.orElse(""));
    }

    @Test
    void acceptsEndExactlyAtTolerance() {
        // 容差设为可精确表示的 0.125：轨迹终点与答案相差恰好 0.125 应通过
        CaptchaAnswer answer = CaptchaAnswer.slider(0.5);
        answer.setTd(dragTrace(0.625));
        assertTrue(toleranceValidator(0.125)
                .validate(answer.getTd(), answer, session()).isEmpty());
    }

    @Test
    void rejectsEndJustBeyondTolerance() {
        CaptchaAnswer answer = CaptchaAnswer.slider(0.5);
        answer.setTd(dragTrace(Math.nextUp(0.625)));
        Optional<String> error = toleranceValidator(0.125)
                .validate(answer.getTd(), answer, session());
        assertEquals("slider.end-mismatch", error.orElse(""));
    }

    @Test
    void rejectsMissingXNorm() {
        CaptchaAnswer answer = CaptchaAnswer.slider(null);
        answer.setTd(dragTrace(1.0));
        Optional<String> error = enabledValidator().validate(answer.getTd(), answer, session());
        assertEquals("slider.missing-x-norm", error.orElse(""));
    }

    /** 开启行为校验并指定终点容差的校验器 */
    private static SwingTileBehaviorValidator toleranceValidator(double tolerance) {
        BehaviorConfig config = new BehaviorConfig();
        config.setEnabled(true);
        config.setPointTolerance(tolerance);
        return new SwingTileBehaviorValidator(config);
    }
}
