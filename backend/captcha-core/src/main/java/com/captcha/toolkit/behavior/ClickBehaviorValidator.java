package com.captcha.toolkit.behavior;

import com.captcha.toolkit.config.BehaviorConfig;
import com.captcha.toolkit.config.ClientBehaviorConfig;
import com.captcha.toolkit.i18n.CaptchaMessages;
import com.captcha.toolkit.model.CaptchaAnswer;
import com.captcha.toolkit.model.CaptchaSession;
import com.captcha.toolkit.model.NormalizedPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 文字点选行为校验：轨迹必须是“按下/松开”成对出现的多次点击，
 * 点击数量、顺序和归一化坐标必须与提交的 points 一致。
 */
public class ClickBehaviorValidator extends AbstractBehaviorValidator {

    /**
     * @param config 行为校验配置（含分端画像）
     */
    public ClickBehaviorValidator(BehaviorConfig config) {
        super(config);
    }

    /** 校验点选事件序列：按下/松开成对出现，顺序合法 */
    @Override
    protected Optional<String> validateEvents(BehaviorTrace trace) {
        List<BehaviorPoint> points = trace.points();
        if (points.getFirst().type() != BehaviorEventType.START) {
            return Optional.of(CaptchaMessages.CLICK_EXPECTED_START);
        }
        if (points.getLast().type() != BehaviorEventType.UP) {
            return Optional.of(CaptchaMessages.CLICK_EXPECTED_RELEASE);
        }
        boolean pendingDown = false;
        for (BehaviorPoint point : points) {
            if (point.type() == BehaviorEventType.DOWN) {
                if (pendingDown) {
                    return Optional.of(CaptchaMessages.CLICK_UNRELEASED);
                }
                pendingDown = true;
            } else if (point.type() == BehaviorEventType.UP) {
                if (!pendingDown) {
                    return Optional.of(CaptchaMessages.CLICK_ORDER_INVALID);
                }
                pendingDown = false;
            }
        }
        return Optional.empty();
    }

    /** 校验点击次数、顺序、位置与点击时长是否与提交答案一致 */
    @Override
    protected Optional<String> validateAnswer(
            BehaviorTrace trace, CaptchaAnswer answer, CaptchaSession session,
            ClientBehaviorConfig profile) {
        List<NormalizedPoint> points = answer == null ? null : answer.getPoints();
        if (points == null || points.isEmpty()) {
            return Optional.of(CaptchaMessages.CLICK_MISSING_POINTS);
        }
        List<BehaviorPoint> downs = new ArrayList<>();
        for (BehaviorPoint point : trace.points()) {
            if (point.type() == BehaviorEventType.DOWN) {
                downs.add(point);
            }
        }
        if (downs.size() != points.size()) {
            return Optional.of(CaptchaMessages.CLICK_COUNT_MISMATCH);
        }
        for (int i = 0; i < points.size(); i++) {
            NormalizedPoint submitted = points.get(i);
            BehaviorPoint down = downs.get(i);
            if (Math.hypot(down.x() - submitted.x(), down.y() - submitted.y())
                    > profile.getPointTolerance()) {
                return Optional.of(CaptchaMessages.CLICK_POSITION_MISMATCH);
            }
        }
        for (long[] pair : pressReleasePairs(trace.points())) {
            long dwell = pair[1] - pair[0];
            if (dwell < profile.getMinClickDurationMs()
                    || dwell > profile.getMaxClickDurationMs()) {
                return Optional.of(CaptchaMessages.CLICK_DURATION_INVALID);
            }
        }
        return Optional.empty();
    }

    /** 提取按下/松开时间对，供点击时长校验使用 */
    private List<long[]> pressReleasePairs(List<BehaviorPoint> points) {
        List<long[]> pairs = new ArrayList<>();
        BehaviorPoint down = null;
        for (BehaviorPoint point : points) {
            if (point.type() == BehaviorEventType.DOWN) {
                down = point;
            } else if (point.type() == BehaviorEventType.UP && down != null) {
                pairs.add(new long[]{down.timeMs(), point.timeMs()});
                down = null;
            }
        }
        return pairs;
    }
}
