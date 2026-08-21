package com.captcha.toolkit.behavior;

import com.captcha.toolkit.config.BehaviorConfig;
import com.captcha.toolkit.i18n.CaptchaMessages;
import com.captcha.toolkit.model.CaptchaAnswer;
import com.captcha.toolkit.model.CaptchaSession;
import com.captcha.toolkit.model.NormalizedPoint;
import com.captcha.toolkit.model.PointVo;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 点选行为校验器边界测试：按下/松开配对、点击次数/位置容差、点击时长上下限。
 */
class ClickBehaviorValidatorTest {

    /** 开启行为校验的校验器 */
    private static ClickBehaviorValidator enabledValidator() {
        BehaviorConfig config = new BehaviorConfig();
        config.setEnabled(true);
        return new ClickBehaviorValidator(config);
    }

    /** 构造点选会话 */
    private static CaptchaSession session() {
        return CaptchaSession.click("click-behavior", 340, 190,
                List.of(new PointVo(102, 76), new PointVo(204, 133)),
                List.of("测", "试"), 300_000L);
    }

    /** 默认答案：两次点击 */
    private static CaptchaAnswer answer() {
        return CaptchaAnswer.click(List.of(
                new NormalizedPoint(0.3, 0.4),
                new NormalizedPoint(0.6, 0.7)));
    }

    /** 构造点选轨迹：起点 → 按下/松开成对出现，点击之间带移动 */
    private static String clickTrace(
            List<NormalizedPoint> downs, long[] dwells) {
        List<BehaviorPoint> points = new ArrayList<>();
        int time = 0;
        double cursorX = downs.getFirst().x();
        double cursorY = downs.getFirst().y();
        points.add(new BehaviorPoint(time, cursorX, cursorY, BehaviorEventType.START));
        for (int i = 0; i < downs.size(); i++) {
            NormalizedPoint down = downs.get(i);
            for (int s = 1; s <= 3; s++) {
                time += 40;
                double ratio = s / 3.0;
                points.add(new BehaviorPoint(time,
                        cursorX + (down.x() - cursorX) * ratio,
                        cursorY + (down.y() - cursorY) * ratio,
                        BehaviorEventType.MOVE));
            }
            time += 20;
            points.add(new BehaviorPoint(time, down.x(), down.y(), BehaviorEventType.DOWN));
            time += dwells[i];
            points.add(new BehaviorPoint(time, down.x(), down.y(), BehaviorEventType.UP));
            cursorX = down.x();
            cursorY = down.y();
        }
        return encode(points);
    }

    /** 编码指定轨迹点 */
    private static String encode(List<BehaviorPoint> points) {
        return BehaviorTraceCodec.encode(new BehaviorTrace(
                1, 340, 190, 1_000_000L, 1_000_000L + points.getLast().timeMs(), points));
    }

    @Test
    void passesCompleteClickTrace() {
        CaptchaAnswer answer = answer();
        answer.setTd(clickTrace(List.of(
                new NormalizedPoint(0.3, 0.4),
                new NormalizedPoint(0.6, 0.7)), new long[]{80, 90}));
        assertTrue(enabledValidator().validate(answer.getTd(), answer, session()).isEmpty());
    }

    @Test
    void rejectsTraceNotStartingWithPress() {
        List<BehaviorPoint> points = clickPoints(
                List.of(new NormalizedPoint(0.3, 0.4)), new long[]{80});
        points.set(0, new BehaviorPoint(0, 0.3, 0.4, BehaviorEventType.MOVE));
        CaptchaAnswer answer = singleAnswer();
        answer.setTd(encode(points));
        Optional<String> error = enabledValidator().validate(answer.getTd(), answer, session());
        assertEquals(CaptchaMessages.CLICK_EXPECTED_START, error.orElse(""));
    }

    @Test
    void rejectsTraceNotEndingWithRelease() {
        List<BehaviorPoint> points = clickPoints(
                List.of(new NormalizedPoint(0.3, 0.4)), new long[]{80});
        BehaviorPoint last = points.getLast();
        points.set(points.size() - 1,
                new BehaviorPoint(last.timeMs(), last.x(), last.y(), BehaviorEventType.MOVE));
        CaptchaAnswer answer = singleAnswer();
        answer.setTd(encode(points));
        Optional<String> error = enabledValidator().validate(answer.getTd(), answer, session());
        assertEquals(CaptchaMessages.CLICK_EXPECTED_RELEASE, error.orElse(""));
    }

    @Test
    void rejectsConsecutiveDownsWithoutRelease() {
        List<BehaviorPoint> points = clickPoints(
                List.of(new NormalizedPoint(0.3, 0.4)), new long[]{80});
        // 在松开前再插入一次按下，形成“按下未松开又按下”，且最后一个事件仍是松开
        BehaviorPoint up = points.get(5);
        points.add(5, new BehaviorPoint(up.timeMs(), up.x(), up.y(), BehaviorEventType.DOWN));
        CaptchaAnswer answer = singleAnswer();
        answer.setTd(encode(points));
        Optional<String> error = enabledValidator().validate(answer.getTd(), answer, session());
        assertEquals(CaptchaMessages.CLICK_UNRELEASED, error.orElse(""));
    }

    @Test
    void rejectsUpWithoutDown() {
        List<BehaviorPoint> points = clickPoints(
                List.of(new NormalizedPoint(0.3, 0.4)), new long[]{80});
        BehaviorPoint down = points.get(4);
        points.set(4, new BehaviorPoint(down.timeMs(), down.x(), down.y(), BehaviorEventType.UP));
        CaptchaAnswer answer = singleAnswer();
        answer.setTd(encode(points));
        Optional<String> error = enabledValidator().validate(answer.getTd(), answer, session());
        assertEquals(CaptchaMessages.CLICK_ORDER_INVALID, error.orElse(""));
    }

    @Test
    void rejectsMissingPoints() {
        CaptchaAnswer answer = CaptchaAnswer.click(List.of());
        answer.setTd(clickTrace(List.of(
                new NormalizedPoint(0.3, 0.4),
                new NormalizedPoint(0.6, 0.7)), new long[]{80, 90}));
        Optional<String> error = enabledValidator().validate(answer.getTd(), answer, session());
        assertEquals(CaptchaMessages.CLICK_MISSING_POINTS, error.orElse(""));
    }

    @Test
    void rejectsCountMismatch() {
        CaptchaAnswer answer = answer();
        answer.setTd(clickTrace(List.of(
                new NormalizedPoint(0.3, 0.4)), new long[]{80}));
        Optional<String> error = enabledValidator().validate(answer.getTd(), answer, session());
        assertEquals(CaptchaMessages.CLICK_COUNT_MISMATCH, error.orElse(""));
    }

    @Test
    void acceptsDownExactlyAtTolerance() {
        // 容差设为可精确表示的 0.125：按下点与答案相差恰好 0.125 应通过
        CaptchaAnswer answer = CaptchaAnswer.click(List.of(
                new NormalizedPoint(0.5, 0.4)));
        answer.setTd(clickTrace(List.of(
                new NormalizedPoint(0.625, 0.4)), new long[]{80}));
        assertTrue(toleranceValidator(0.125)
                .validate(answer.getTd(), answer, session()).isEmpty());
    }

    @Test
    void rejectsDownJustBeyondTolerance() {
        CaptchaAnswer answer = CaptchaAnswer.click(List.of(
                new NormalizedPoint(0.5, 0.4)));
        answer.setTd(clickTrace(List.of(
                new NormalizedPoint(Math.nextUp(0.625), 0.4)), new long[]{80}));
        Optional<String> error = toleranceValidator(0.125)
                .validate(answer.getTd(), answer, session());
        assertEquals(CaptchaMessages.CLICK_POSITION_MISMATCH, error.orElse(""));
    }

    @Test
    void acceptsDwellAtMinimum() {
        CaptchaAnswer answer = singleAnswer();
        answer.setTd(clickTrace(List.of(
                new NormalizedPoint(0.3, 0.4)), new long[]{30}));
        assertTrue(enabledValidator().validate(answer.getTd(), answer, session()).isEmpty());
    }

    @Test
    void rejectsDwellBelowMinimum() {
        CaptchaAnswer answer = singleAnswer();
        answer.setTd(clickTrace(List.of(
                new NormalizedPoint(0.3, 0.4)), new long[]{29}));
        Optional<String> error = enabledValidator().validate(answer.getTd(), answer, session());
        assertEquals(CaptchaMessages.CLICK_DURATION_INVALID, error.orElse(""));
    }

    @Test
    void acceptsDwellAtMaximum() {
        CaptchaAnswer answer = singleAnswer();
        answer.setTd(clickTrace(List.of(
                new NormalizedPoint(0.3, 0.4)), new long[]{5_000}));
        assertTrue(enabledValidator().validate(answer.getTd(), answer, session()).isEmpty());
    }

    @Test
    void rejectsDwellAboveMaximum() {
        CaptchaAnswer answer = singleAnswer();
        answer.setTd(clickTrace(List.of(
                new NormalizedPoint(0.3, 0.4)), new long[]{5_001}));
        Optional<String> error = enabledValidator().validate(answer.getTd(), answer, session());
        assertEquals(CaptchaMessages.CLICK_DURATION_INVALID, error.orElse(""));
    }

    /** 单次点击答案（(0.3, 0.4)） */
    private static CaptchaAnswer singleAnswer() {
        return CaptchaAnswer.click(List.of(new NormalizedPoint(0.3, 0.4)));
    }

    /** 开启行为校验并指定位置容差的校验器 */
    private static ClickBehaviorValidator toleranceValidator(double tolerance) {
        BehaviorConfig config = new BehaviorConfig();
        config.setEnabled(true);
        config.setPointTolerance(tolerance);
        return new ClickBehaviorValidator(config);
    }

    /** 构造点选轨迹点列表 */
    private static List<BehaviorPoint> clickPoints(
            List<NormalizedPoint> downs, long[] dwells) {
        List<BehaviorPoint> points = new ArrayList<>();
        int time = 0;
        double cursorX = downs.getFirst().x();
        double cursorY = downs.getFirst().y();
        points.add(new BehaviorPoint(time, cursorX, cursorY, BehaviorEventType.START));
        for (int i = 0; i < downs.size(); i++) {
            NormalizedPoint down = downs.get(i);
            for (int s = 1; s <= 3; s++) {
                time += 40;
                double ratio = s / 3.0;
                points.add(new BehaviorPoint(time,
                        cursorX + (down.x() - cursorX) * ratio,
                        cursorY + (down.y() - cursorY) * ratio,
                        BehaviorEventType.MOVE));
            }
            time += 20;
            points.add(new BehaviorPoint(time, down.x(), down.y(), BehaviorEventType.DOWN));
            time += dwells[i];
            points.add(new BehaviorPoint(time, down.x(), down.y(), BehaviorEventType.UP));
            cursorX = down.x();
            cursorY = down.y();
        }
        return points;
    }
}
