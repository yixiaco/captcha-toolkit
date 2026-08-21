package com.captcha.toolkit.behavior;

import com.captcha.toolkit.config.BehaviorConfig;
import com.captcha.toolkit.i18n.CaptchaMessages;
import com.captcha.toolkit.model.CaptchaAnswer;
import com.captcha.toolkit.model.CaptchaSession;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 刮刮乐行为校验器测试：单次“按下 → 移动 → 松开”拖拽事件序列与
 * “轨迹终点-滑块位置”容差边界。
 */
class ScratchBehaviorValidatorTest {

    /** 开启行为校验的校验器 */
    private static ScratchBehaviorValidator enabledValidator() {
        BehaviorConfig config = new BehaviorConfig();
        config.setEnabled(true);
        return new ScratchBehaviorValidator(config);
    }

    /** 构造刮刮乐会话（行为校验不读取图案布局） */
    private static CaptchaSession session() {
        return CaptchaSession.scratch("scratch-behavior", 340, 190, 5000,
                List.of(), List.of(), 300_000L);
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
        CaptchaAnswer answer = CaptchaAnswer.slider(0.5);
        answer.setTd(dragTrace(0.5));
        assertTrue(enabledValidator().validate(answer.getTd(), answer, session()).isEmpty());
    }

    @Test
    void rejectsTraceNotStartingWithPress() {
        CaptchaAnswer answer = CaptchaAnswer.slider(0.5);
        answer.setTd(dragTrace(0.5).replace("0,0.01,0.5,0", "0,0.01,0.5,1"));
        Optional<String> error = enabledValidator().validate(answer.getTd(), answer, session());
        assertEquals(CaptchaMessages.SCRATCH_EXPECTED_START, error.orElse(""));
    }

    @Test
    void rejectsTraceNotEndingWithRelease() {
        CaptchaAnswer answer = CaptchaAnswer.slider(0.5);
        answer.setTd(dragTrace(0.5).replace("1000,0.5,0.5,2", "1000,0.5,0.5,1"));
        Optional<String> error = enabledValidator().validate(answer.getTd(), answer, session());
        assertEquals(CaptchaMessages.SCRATCH_EXPECTED_RELEASE, error.orElse(""));
    }

    @Test
    void rejectsClickEventInTrace() {
        List<BehaviorPoint> points = List.of(
                new BehaviorPoint(0, 0.01, 0.5, BehaviorEventType.START),
                new BehaviorPoint(100, 0.2, 0.5, BehaviorEventType.MOVE),
                new BehaviorPoint(200, 0.3, 0.5, BehaviorEventType.DOWN),
                new BehaviorPoint(300, 0.5, 0.5, BehaviorEventType.UP));
        CaptchaAnswer answer = CaptchaAnswer.slider(0.5);
        answer.setTd(BehaviorTraceCodec.encode(new BehaviorTrace(
                1, 340, 190, 1_000_000L, 1_000_300L, points)));
        Optional<String> error = enabledValidator().validate(answer.getTd(), answer, session());
        assertEquals(CaptchaMessages.SCRATCH_CLICK_NOT_ALLOWED, error.orElse(""));
    }

    @Test
    void rejectsTraceWithoutMovement() {
        List<BehaviorPoint> points = List.of(
                new BehaviorPoint(0, 0.01, 0.5, BehaviorEventType.START),
                new BehaviorPoint(100, 0.3, 0.5, BehaviorEventType.UP),
                new BehaviorPoint(200, 0.5, 0.5, BehaviorEventType.UP));
        CaptchaAnswer answer = CaptchaAnswer.slider(0.5);
        answer.setTd(BehaviorTraceCodec.encode(new BehaviorTrace(
                1, 340, 190, 1_000_000L, 1_000_200L, points)));
        Optional<String> error = enabledValidator().validate(answer.getTd(), answer, session());
        assertEquals(CaptchaMessages.SCRATCH_MISSING_MOVE, error.orElse(""));
    }

    @Test
    void rejectsMissingXNorm() {
        CaptchaAnswer answer = CaptchaAnswer.slider(null);
        answer.setTd(dragTrace(0.5));
        Optional<String> error = enabledValidator().validate(answer.getTd(), answer, session());
        assertEquals(CaptchaMessages.SCRATCH_MISSING_X_NORM, error.orElse(""));
    }

    @Test
    void rejectsEndMismatch() {
        CaptchaAnswer answer = CaptchaAnswer.slider(0.4);
        answer.setTd(dragTrace(0.6));
        Optional<String> error = enabledValidator().validate(answer.getTd(), answer, session());
        assertEquals(CaptchaMessages.SCRATCH_END_MISMATCH, error.orElse(""));
    }

    @Test
    void acceptsEndExactlyAtTolerance() {
        // 容差设为可精确表示的 0.125：轨迹终点与答案相差恰好 0.125 应通过
        BehaviorConfig config = new BehaviorConfig();
        config.setEnabled(true);
        config.setPointTolerance(0.125);
        ScratchBehaviorValidator validator = new ScratchBehaviorValidator(config);
        CaptchaAnswer answer = CaptchaAnswer.slider(0.5);
        answer.setTd(dragTrace(0.625));
        assertTrue(validator.validate(answer.getTd(), answer, session()).isEmpty());
    }

    @Test
    void rejectsEndJustBeyondTolerance() {
        BehaviorConfig config = new BehaviorConfig();
        config.setEnabled(true);
        config.setPointTolerance(0.125);
        ScratchBehaviorValidator validator = new ScratchBehaviorValidator(config);
        CaptchaAnswer answer = CaptchaAnswer.slider(0.5);
        answer.setTd(dragTrace(Math.nextUp(0.625)));
        Optional<String> error = validator.validate(answer.getTd(), answer, session());
        assertEquals(CaptchaMessages.SCRATCH_END_MISMATCH, error.orElse(""));
    }
}
