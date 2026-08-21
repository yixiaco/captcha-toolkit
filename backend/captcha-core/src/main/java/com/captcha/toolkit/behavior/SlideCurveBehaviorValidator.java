package com.captcha.toolkit.behavior;

import com.captcha.toolkit.config.BehaviorConfig;
import com.captcha.toolkit.config.ClientBehaviorConfig;
import com.captcha.toolkit.i18n.CaptchaMessages;
import com.captcha.toolkit.model.CaptchaAnswer;
import com.captcha.toolkit.model.CaptchaSession;

import java.util.List;
import java.util.Optional;

/**
 * 滑动曲线行为校验：与滑块拖拽一致，轨迹必须是
 * “按下 → 连续移动 → 松开”的拖拽序列，且松开点的归一化 x
 * 必须与提交的曲线位移答案一致。
 */
public class SlideCurveBehaviorValidator extends AbstractBehaviorValidator {

    /**
     * @param config 行为校验配置（含分端画像）
     */
    public SlideCurveBehaviorValidator(BehaviorConfig config) {
        super(config);
    }

    /** 校验拖拽事件序列：按下开始、连续移动、松开结束，且不允许点击事件 */
    @Override
    protected Optional<String> validateEvents(BehaviorTrace trace) {
        List<BehaviorPoint> points = trace.points();
        if (points.getFirst().type() != BehaviorEventType.START) {
            return Optional.of(CaptchaMessages.SLIDER_EXPECTED_START);
        }
        if (points.getLast().type() != BehaviorEventType.UP) {
            return Optional.of(CaptchaMessages.SLIDER_EXPECTED_RELEASE);
        }
        boolean hasMove = false;
        for (BehaviorPoint point : points) {
            if (point.type() == BehaviorEventType.DOWN) {
                return Optional.of(CaptchaMessages.SLIDER_CLICK_NOT_ALLOWED);
            }
            hasMove |= point.type() == BehaviorEventType.MOVE;
        }
        if (!hasMove) {
            return Optional.of(CaptchaMessages.SLIDER_MISSING_MOVE);
        }
        return Optional.empty();
    }

    /** 校验轨迹终点归一化 x 与提交的曲线位移答案一致 */
    @Override
    protected Optional<String> validateAnswer(
            BehaviorTrace trace, CaptchaAnswer answer, CaptchaSession session,
            ClientBehaviorConfig profile) {
        if (answer == null || answer.getXNorm() == null) {
            return Optional.of(CaptchaMessages.SLIDER_MISSING_X_NORM);
        }
        double expectedX = answer.getXNorm();
        BehaviorPoint end = trace.points().getLast();
        if (Math.abs(end.x() - expectedX) > profile.getPointTolerance()) {
            return Optional.of(CaptchaMessages.SLIDER_END_MISMATCH);
        }
        return Optional.empty();
    }
}
