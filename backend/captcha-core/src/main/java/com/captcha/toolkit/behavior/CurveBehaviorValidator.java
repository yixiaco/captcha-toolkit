package com.captcha.toolkit.behavior;

import com.captcha.toolkit.config.BehaviorConfig;
import com.captcha.toolkit.config.ClientBehaviorConfig;
import com.captcha.toolkit.i18n.CaptchaMessages;
import com.captcha.toolkit.model.CaptchaAnswer;
import com.captcha.toolkit.model.CaptchaSession;
import com.captcha.toolkit.model.NormalizedPoint;

import java.util.List;
import java.util.Optional;

/**
 * 曲线绘制行为校验：轨迹必须是“按下 → 连续移动 → 松开”的绘制过程，
 * 且轨迹首尾点必须与提交的绘制曲线首尾一致。
 *
 * <p>曲线与期望路径的几何匹配（覆盖率、容差）由生成器负责，
 * 这里只做事件序列与“轨迹-答案”一致性校验。</p>
 */
public class CurveBehaviorValidator extends AbstractBehaviorValidator {

    /**
     * @param config 行为校验配置（含分端画像）
     */
    public CurveBehaviorValidator(BehaviorConfig config) {
        super(config);
    }

    /** 校验绘制事件序列：按下开始、连续移动、松开结束，且不允许点击事件 */
    @Override
    protected Optional<String> validateEvents(BehaviorTrace trace) {
        List<BehaviorPoint> points = trace.points();
        if (points.getFirst().type() != BehaviorEventType.START) {
            return Optional.of(CaptchaMessages.CURVE_EXPECTED_START);
        }
        if (points.getLast().type() != BehaviorEventType.UP) {
            return Optional.of(CaptchaMessages.CURVE_EXPECTED_RELEASE);
        }
        boolean hasMove = false;
        for (BehaviorPoint point : points) {
            if (point.type() == BehaviorEventType.DOWN) {
                return Optional.of(CaptchaMessages.CURVE_CLICK_NOT_ALLOWED);
            }
            hasMove |= point.type() == BehaviorEventType.MOVE;
        }
        if (!hasMove) {
            return Optional.of(CaptchaMessages.CURVE_MISSING_MOVE);
        }
        return Optional.empty();
    }

    /** 校验绘制曲线非空，且轨迹首尾点与答案曲线首尾点一致 */
    @Override
    protected Optional<String> validateAnswer(
            BehaviorTrace trace, CaptchaAnswer answer, CaptchaSession session,
            ClientBehaviorConfig profile) {
        List<NormalizedPoint> curve = answer == null ? null : answer.getCurve();
        if (curve == null || curve.isEmpty()) {
            return Optional.of(CaptchaMessages.CURVE_MISSING_POINTS);
        }
        if (curve.size() < Math.max(2, profile.getMinPoints())) {
            return Optional.of(CaptchaMessages.CURVE_NOT_ENOUGH_POINTS);
        }
        double tolerance = profile.getPointTolerance() * 2;
        BehaviorPoint traceStart = trace.points().getFirst();
        BehaviorPoint traceEnd = trace.points().getLast();
        NormalizedPoint curveStart = curve.getFirst();
        NormalizedPoint curveEnd = curve.getLast();
        if (Math.hypot(traceStart.x() - curveStart.x(), traceStart.y() - curveStart.y())
                > tolerance) {
            return Optional.of(CaptchaMessages.CURVE_START_MISMATCH);
        }
        if (Math.hypot(traceEnd.x() - curveEnd.x(), traceEnd.y() - curveEnd.y())
                > tolerance) {
            return Optional.of(CaptchaMessages.CURVE_END_MISMATCH);
        }
        return Optional.empty();
    }
}
