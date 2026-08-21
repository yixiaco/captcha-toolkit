package com.captcha.toolkit.generator;

import com.captcha.toolkit.behavior.BehaviorValidator;
import com.captcha.toolkit.behavior.SlideCurveBehaviorValidator;
import com.captcha.toolkit.config.BehaviorConfig;
import com.captcha.toolkit.config.SlideCurveConfig;
import com.captcha.toolkit.i18n.CaptchaMessages;
import com.captcha.toolkit.i18n.MessageProvider;
import com.captcha.toolkit.i18n.ResourceBundleMessageProvider;
import com.captcha.toolkit.model.CaptchaAnswer;
import com.captcha.toolkit.model.CaptchaSession;
import com.captcha.toolkit.model.GeneratedCaptcha;
import com.captcha.toolkit.model.SlideCurveChallengeData;
import com.captcha.toolkit.model.VerifyResult;
import com.captcha.toolkit.render.BackgroundProvider;
import com.captcha.toolkit.render.SlideCurveRenderer;
import com.captcha.toolkit.type.CaptchaType;

import java.util.List;
import java.util.Optional;

/**
 * 滑动曲线验证码生成器：大图上曲线两端固定，用户拖动滑块让曲线摆动，
 * 对准图中唯一的真凹槽；答案以摆动量（0~1）提交。
 */
public class SlideCurveCaptchaGenerator
        extends AbstractCaptchaGenerator<SlideCurveChallengeData> {

    /** 滑动曲线配置 */
    private final SlideCurveConfig options;

    /** 背景图提供者 */
    private final BackgroundProvider backgroundProvider;

    /** 滑动曲线行为轨迹校验器 */
    private final BehaviorValidator behaviorValidator;

    /** 使用默认（关闭）行为校验构造生成器 */
    public SlideCurveCaptchaGenerator(SlideCurveConfig options,
                                      BackgroundProvider backgroundProvider) {
        this(options, backgroundProvider,
                new SlideCurveBehaviorValidator(new BehaviorConfig()),
                new ResourceBundleMessageProvider());
    }

    /**
     * @param options            滑动曲线配置
     * @param backgroundProvider 背景图提供者
     * @param behaviorValidator  行为轨迹校验器
     */
    public SlideCurveCaptchaGenerator(SlideCurveConfig options,
                                      BackgroundProvider backgroundProvider,
                                      BehaviorValidator behaviorValidator) {
        this(options, backgroundProvider, behaviorValidator,
                new ResourceBundleMessageProvider());
    }

    /**
     * @param options            滑动曲线配置
     * @param backgroundProvider 背景图提供者
     * @param behaviorValidator  行为轨迹校验器
     * @param messages           用户提示消息提供者
     */
    public SlideCurveCaptchaGenerator(SlideCurveConfig options,
                                      BackgroundProvider backgroundProvider,
                                      BehaviorValidator behaviorValidator,
                                      MessageProvider messages) {
        super(messages);
        this.options = options;
        this.backgroundProvider = backgroundProvider;
        this.behaviorValidator = behaviorValidator;
    }

    @Override
    public CaptchaType type() {
        return CaptchaType.SLIDE_CURVE;
    }

    @Override
    protected GeneratedCaptcha<SlideCurveChallengeData> doGenerate(GenerateRequest request) {
        SlideCurveRenderer renderer = new SlideCurveRenderer(options, backgroundProvider);
        renderer.run();

        // 摆动答案 0~1 放大 10000 倍存进会话 x，避免为单一类型扩展会话模型
        int scaledSwing = (int) Math.round(renderer.getAnswerSwing() * 10000);
        CaptchaSession session = CaptchaSession.slideCurve(
                request.getId(), renderer.getWidth(), renderer.getHeight(),
                scaledSwing, options.getExpireSeconds() * 1000);
        GeneratedCaptcha<SlideCurveChallengeData> result = new GeneratedCaptcha<>();
        result.setSession(session);
        result.setImage1(renderer.getArtwork());
        result.setWidth(renderer.getWidth());
        result.setHeight(renderer.getHeight());
        result.setData(new SlideCurveChallengeData(
                List.of(renderer.getLeftEnd(), renderer.getRightEnd()),
                renderer.getAmplitude(),
                renderer.getShape(),
                request.isDebug() ? renderer.getAnswerSwing() : null,
                request.isDebug() ? renderer.getFakeTargets().stream()
                        .map(f -> f.getPoints().getFirst())
                        .toList() : null));
        return result;
    }

    @Override
    protected VerifyResult doVerify(CaptchaSession session, CaptchaAnswer answer) {
        if (answer == null || answer.getXNorm() == null) {
            return VerifyResult.badRequest(CaptchaMessages.SLIDER_MISSING_X_NORM, messages);
        }
        Optional<String> behaviorError = behaviorValidator.validate(
                answer.getTd(), answer, session);
        if (behaviorError.isPresent()) {
            return VerifyResult.fail(behaviorError.get(), "BEHAVIOR", messages);
        }
        double expected = session.getX() / 10000.0;
        double tolerance = options.getTolerance();
        if (Math.abs(answer.getXNorm() - expected) <= tolerance) {
            return VerifyResult.ok(CaptchaMessages.VERIFY_OK, messages);
        }
        return VerifyResult.fail(CaptchaMessages.VERIFY_WRONG, "WRONG", messages);
    }

    @Override
    protected long minElapsedMs() {
        return options.getMinElapsedMs();
    }
}
