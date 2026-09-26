package io.github.yixiaco.behavior;

import io.github.yixiaco.config.BehaviorConfig;
import io.github.yixiaco.config.ClientBehaviorConfig;
import io.github.yixiaco.i18n.CaptchaMessages;
import io.github.yixiaco.model.CaptchaAnswer;
import io.github.yixiaco.model.CaptchaSession;

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
        if (points.get(0).type() != BehaviorEventType.START) {
            return Optional.of(CaptchaMessages.ROTATE_EXPECTED_START);
        }
        if (points.get(points.size() - 1).type() != BehaviorEventType.UP) {
            return Optional.of(CaptchaMessages.ROTATE_EXPECTED_RELEASE);
        }
        boolean hasMove = false;
        for (BehaviorPoint point : points) {
            if (point.type() == BehaviorEventType.DOWN) {
                return Optional.of(CaptchaMessages.ROTATE_CLICK_NOT_ALLOWED);
            }
            hasMove |= point.type() == BehaviorEventType.MOVE;
        }
        if (!hasMove) {
            return Optional.of(CaptchaMessages.ROTATE_MISSING_MOVE);
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
