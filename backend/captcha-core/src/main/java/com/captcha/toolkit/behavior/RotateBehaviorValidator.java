package com.captcha.toolkit.behavior;

import com.captcha.toolkit.config.BehaviorConfig;
import com.captcha.toolkit.config.ClientBehaviorConfig;
import com.captcha.toolkit.model.CaptchaAnswer;
import com.captcha.toolkit.model.CaptchaSession;

import java.util.List;
import java.util.Optional;

/**
 * 图片旋转行为校验：与滑块同属拖拽交互，只校验“按下 → 移动 → 松开”事件序列；
 * 角度是否正确仍由生成器的答案校验负责。
 */
public class RotateBehaviorValidator extends AbstractBehaviorValidator {

    /**
     * @param config 行为校验配置（含分端画像）
     */
    public RotateBehaviorValidator(BehaviorConfig config) {
        super(config);
    }

    /** 校验旋转拖拽事件序列：按下开始、连续移动、松开结束 */
    @Override
    protected Optional<String> validateEvents(BehaviorTrace trace) {
        List<BehaviorPoint> points = trace.points();
        if (points.getFirst().type() != BehaviorEventType.START) {
            return Optional.of("旋转轨迹应以按下开始");
        }
        if (points.getLast().type() != BehaviorEventType.UP) {
            return Optional.of("旋转轨迹应以松开结束");
        }
        boolean hasMove = false;
        for (BehaviorPoint point : points) {
            if (point.type() == BehaviorEventType.DOWN) {
                return Optional.of("旋转轨迹不允许出现点击事件");
            }
            hasMove |= point.type() == BehaviorEventType.MOVE;
        }
        if (!hasMove) {
            return Optional.of("旋转缺少移动轨迹");
        }
        return Optional.empty();
    }

    /** 角度是否正确由生成器答案校验负责，此处无需额外检查 */
    @Override
    protected Optional<String> validateAnswer(
            BehaviorTrace trace, CaptchaAnswer answer, CaptchaSession session,
            ClientBehaviorConfig profile) {
        return Optional.empty();
    }
}
