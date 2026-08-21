package com.captcha.toolkit.behavior;

import com.captcha.toolkit.config.BehaviorConfig;
import com.captcha.toolkit.i18n.CaptchaMessages;
import com.captcha.toolkit.model.CaptchaAnswer;
import com.captcha.toolkit.model.CaptchaSession;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 旋转行为校验器边界测试：拖拽事件序列的合法边界。
 * （角度是否正确由生成器答案校验负责，本校验器不校验答案。）
 */
class RotateBehaviorValidatorTest {

    /** 开启行为校验的校验器 */
    private static RotateBehaviorValidator enabledValidator() {
        BehaviorConfig config = new BehaviorConfig();
        config.setEnabled(true);
        return new RotateBehaviorValidator(config);
    }

    /** 构造旋转会话 */
    private static CaptchaSession session() {
        return CaptchaSession.rotate("rotate-behavior", 340, 190, 90, 300_000L);
    }

    /** 构造一条终点 x = 0.5 的拖拽轨迹 */
    private static String dragTrace() {
        List<BehaviorPoint> points = List.of(
                new BehaviorPoint(0, 0.01, 0.5, BehaviorEventType.START),
                new BehaviorPoint(100, 0.2, 0.5, BehaviorEventType.MOVE),
                new BehaviorPoint(200, 0.35, 0.5, BehaviorEventType.MOVE),
                new BehaviorPoint(300, 0.5, 0.5, BehaviorEventType.UP));
        return BehaviorTraceCodec.encode(new BehaviorTrace(
                1, 340, 190, 1_000_000L, 1_000_300L, points));
    }

    @Test
    void passesCompleteDragTrace() {
        CaptchaAnswer answer = CaptchaAnswer.rotate(90.0);
        answer.setTd(dragTrace());
        assertTrue(enabledValidator().validate(answer.getTd(), answer, session()).isEmpty());
    }

    @Test
    void rejectsTraceNotStartingWithPress() {
        CaptchaAnswer answer = CaptchaAnswer.rotate(90.0);
        answer.setTd(dragTrace().replace("0,0.01,0.5,0", "0,0.01,0.5,1"));
        Optional<String> error = enabledValidator().validate(answer.getTd(), answer, session());
        assertEquals(CaptchaMessages.ROTATE_EXPECTED_START, error.orElse(""));
    }

    @Test
    void rejectsTraceNotEndingWithRelease() {
        CaptchaAnswer answer = CaptchaAnswer.rotate(90.0);
        answer.setTd(dragTrace().replace("300,0.5,0.5,2", "300,0.5,0.5,1"));
        Optional<String> error = enabledValidator().validate(answer.getTd(), answer, session());
        assertEquals(CaptchaMessages.ROTATE_EXPECTED_RELEASE, error.orElse(""));
    }

    @Test
    void rejectsClickEventInTrace() {
        List<BehaviorPoint> points = List.of(
                new BehaviorPoint(0, 0.01, 0.5, BehaviorEventType.START),
                new BehaviorPoint(100, 0.2, 0.5, BehaviorEventType.MOVE),
                new BehaviorPoint(200, 0.3, 0.5, BehaviorEventType.DOWN),
                new BehaviorPoint(300, 0.5, 0.5, BehaviorEventType.UP));
        CaptchaAnswer answer = CaptchaAnswer.rotate(90.0);
        answer.setTd(BehaviorTraceCodec.encode(new BehaviorTrace(
                1, 340, 190, 1_000_000L, 1_000_300L, points)));
        Optional<String> error = enabledValidator().validate(answer.getTd(), answer, session());
        assertEquals(CaptchaMessages.ROTATE_CLICK_NOT_ALLOWED, error.orElse(""));
    }

    @Test
    void rejectsTraceWithoutMovement() {
        List<BehaviorPoint> points = List.of(
                new BehaviorPoint(0, 0.01, 0.5, BehaviorEventType.START),
                new BehaviorPoint(100, 0.3, 0.5, BehaviorEventType.UP),
                new BehaviorPoint(200, 0.5, 0.5, BehaviorEventType.UP));
        CaptchaAnswer answer = CaptchaAnswer.rotate(90.0);
        answer.setTd(BehaviorTraceCodec.encode(new BehaviorTrace(
                1, 340, 190, 1_000_000L, 1_000_200L, points)));
        Optional<String> error = enabledValidator().validate(answer.getTd(), answer, session());
        assertEquals(CaptchaMessages.ROTATE_MISSING_MOVE, error.orElse(""));
    }
}
