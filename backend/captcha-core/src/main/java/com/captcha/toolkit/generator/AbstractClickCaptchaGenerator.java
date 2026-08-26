package com.captcha.toolkit.generator;

import com.captcha.toolkit.behavior.BehaviorValidator;
import com.captcha.toolkit.i18n.CaptchaMessages;
import com.captcha.toolkit.i18n.MessageProvider;
import com.captcha.toolkit.model.CaptchaAnswer;
import com.captcha.toolkit.model.CaptchaSession;
import com.captcha.toolkit.model.ClickChallengeData;
import com.captcha.toolkit.model.NormalizedPoint;
import com.captcha.toolkit.model.PointVo;
import com.captcha.toolkit.model.VerifyResult;

import java.util.List;
import java.util.Optional;

/**
 * 点选类验证码公共校验逻辑（文字点选 / 图形点选共用）。
 *
 * <p>校验规则：点击数量必须与目标一致、行为轨迹通过、每个点击坐标
 * 按服务端像素容差与答案顺序匹配，全部通过后发放一次性票据。</p>
 */
public abstract class AbstractClickCaptchaGenerator<T extends ClickChallengeData>
        extends AbstractCaptchaGenerator<T> {

    /** 点选行为轨迹校验器 */
    private final BehaviorValidator behaviorValidator;

    /**
     * @param messages          用户提示消息提供者
     * @param behaviorValidator 点选行为轨迹校验器
     */
    protected AbstractClickCaptchaGenerator(MessageProvider messages,
                                            BehaviorValidator behaviorValidator) {
        super(messages);
        this.behaviorValidator = behaviorValidator;
    }

    @Override
    protected VerifyResult doVerify(CaptchaSession session, CaptchaAnswer answer) {
        List<NormalizedPoint> points = answer == null ? null : answer.getPoints();
        if (points == null || points.size() != session.getTargets().size()) {
            return VerifyResult.badRequest(CaptchaMessages.VERIFY_BAD_PARAM, messages);
        }
        Optional<String> behaviorError = behaviorValidator.validate(
                answer.getTd(), answer, session);
        if (behaviorError.isPresent()) {
            return VerifyResult.fail(behaviorError.get(), "BEHAVIOR", messages);
        }
        // 点选答案是归一化坐标；先换算回服务端像素再做距离校验，保持容差语义不变
        for (int i = 0; i < points.size(); i++) {
            NormalizedPoint actual = points.get(i);
            PointVo expected = session.getTargets().get(i);
            double actualX = actual.x() * session.getWidth();
            double actualY = actual.y() * session.getHeight();
            if (Math.hypot(actualX - expected.getX(), actualY - expected.getY())
                    > tolerance()) {
                return VerifyResult.fail(CaptchaMessages.CLICK_WRONG, "WRONG", messages);
            }
        }
        return VerifyResult.ok(CaptchaMessages.VERIFY_OK, messages);
    }

    /** 点选坐标容差（服务端像素） */
    protected abstract double tolerance();
}
