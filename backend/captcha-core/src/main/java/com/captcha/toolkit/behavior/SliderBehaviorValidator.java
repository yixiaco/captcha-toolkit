package com.captcha.toolkit.behavior;

import com.captcha.toolkit.config.BehaviorConfig;
import com.captcha.toolkit.config.ClientBehaviorConfig;
import com.captcha.toolkit.model.CaptchaAnswer;
import com.captcha.toolkit.model.CaptchaSession;

import java.util.List;
import java.util.Optional;

/**
 * 滑块行为校验：一条“按下 → 连续移动 → 松开”的拖拽轨迹，
 * 且松开点的归一化 x 必须与提交的滑块位移一致。
 */
public class SliderBehaviorValidator extends AbstractBehaviorValidator {

    /**
     * @param config 行为校验配置（含分端画像）
     */
    public SliderBehaviorValidator(BehaviorConfig config) {
        super(config);
    }

    /** 校验拖拽事件序列：按下开始、连续移动、松开结束，且不允许点击事件 */
    @Override
    protected Optional<String> validateEvents(BehaviorTrace trace) {
        List<BehaviorPoint> points = trace.points();
        if (points.getFirst().type() != BehaviorEventType.START) {
            return Optional.of("滑块轨迹应以按下开始");
        }
        if (points.getLast().type() != BehaviorEventType.UP) {
            return Optional.of("滑块轨迹应以松开结束");
        }
        boolean hasMove = false;
        for (BehaviorPoint point : points) {
            if (point.type() == BehaviorEventType.DOWN) {
                return Optional.of("滑块轨迹不允许出现点击事件");
            }
            hasMove |= point.type() == BehaviorEventType.MOVE;
        }
        if (!hasMove) {
            return Optional.of("滑块缺少移动轨迹");
        }
        return Optional.empty();
    }

    /** 校验轨迹终点归一化 x 与提交的滑块答案一致 */
    @Override
    protected Optional<String> validateAnswer(
            BehaviorTrace trace, CaptchaAnswer answer, CaptchaSession session,
            ClientBehaviorConfig profile) {
        if (answer == null || answer.getXNorm() == null) {
            return Optional.of("缺少滑块位移 xNorm");
        }
        double expectedX = answer.getXNorm();
        BehaviorPoint end = trace.points().getLast();
        if (Math.abs(end.x() - expectedX) > profile.getPointTolerance()) {
            return Optional.of("滑块终点与轨迹不一致");
        }
        return Optional.empty();
    }
}
