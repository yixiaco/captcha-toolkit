package com.captcha.toolkit.generator;

import com.captcha.toolkit.behavior.AngleBehaviorValidator;
import com.captcha.toolkit.behavior.BehaviorValidator;
import com.captcha.toolkit.config.AngleConfig;
import com.captcha.toolkit.config.BehaviorConfig;
import com.captcha.toolkit.exception.CaptchaException;
import com.captcha.toolkit.i18n.CaptchaMessages;
import com.captcha.toolkit.i18n.MessageProvider;
import com.captcha.toolkit.i18n.ResourceBundleMessageProvider;
import com.captcha.toolkit.model.AngleChallengeData;
import com.captcha.toolkit.model.CaptchaAnswer;
import com.captcha.toolkit.model.CaptchaSession;
import com.captcha.toolkit.model.GeneratedCaptcha;
import com.captcha.toolkit.model.VerifyResult;
import com.captcha.toolkit.render.AngleCaptchaRenderer;
import com.captcha.toolkit.render.BackgroundProvider;
import com.captcha.toolkit.type.CaptchaType;

import java.util.Optional;
import java.util.Random;

/**
 * 角度验证码生成器。
 *
 * <p>背景中心放一个圆形转盘，转盘上的方向箭头随机错位；
 * 用户拖动滑块把箭头转回顶部的固定凹口，答案角度只保存在服务端会话里。</p>
 */
public class AngleCaptchaGenerator extends AbstractCaptchaGenerator<AngleChallengeData> {

    /** 角度验证配置 */
    private final AngleConfig options;

    /** 背景图提供者 */
    private final BackgroundProvider backgroundProvider;

    /** 角度验证行为轨迹校验器 */
    private final BehaviorValidator behaviorValidator;

    /** 随机数源 */
    private final Random random = new Random();

    /** 使用默认（关闭）行为校验构造生成器 */
    public AngleCaptchaGenerator(AngleConfig options, BackgroundProvider backgroundProvider) {
        this(options, backgroundProvider,
                new AngleBehaviorValidator(new BehaviorConfig()),
                new ResourceBundleMessageProvider());
    }

    /**
     * @param options            角度验证配置
     * @param backgroundProvider 背景图提供者
     * @param behaviorValidator  行为轨迹校验器
     */
    public AngleCaptchaGenerator(AngleConfig options,
                                 BackgroundProvider backgroundProvider,
                                 BehaviorValidator behaviorValidator) {
        this(options, backgroundProvider, behaviorValidator,
                new ResourceBundleMessageProvider());
    }

    /**
     * @param options            角度验证配置
     * @param backgroundProvider 背景图提供者
     * @param behaviorValidator  行为轨迹校验器
     * @param messages           用户提示消息提供者
     */
    public AngleCaptchaGenerator(AngleConfig options,
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
        return CaptchaType.ANGLE;
    }

    @Override
    protected GeneratedCaptcha<AngleChallengeData> doGenerate(GenerateRequest request) {
        int w = options.getWidth();
        int h = options.getHeight();
        java.awt.image.BufferedImage raw = backgroundProvider.provide(w, h)
                .orElseThrow(() -> new CaptchaException(
                        "没有可用的背景图，请配置 captcha.background.sources 或开启 generate-fallback"));

        // 随机错位角度；用户需要转回的角度 = 360 - angle
        double angle = rand(options.getMinAngle(), options.getMaxAngle());
        double answer = normalize360(360 - angle);

        AngleCaptchaRenderer renderer = new AngleCaptchaRenderer(options);
        java.awt.image.BufferedImage disc = renderer.render(raw, angle);
        int discSize = renderer.discSize();

        CaptchaSession session = CaptchaSession.angle(request.getId(), w, h,
                answer, options.getExpireSeconds() * 1000);
        GeneratedCaptcha<AngleChallengeData> result = new GeneratedCaptcha<>();
        result.setSession(session);
        result.setImage1(null);
        result.setImage2(disc);
        result.setWidth(w);
        result.setHeight(h);
        result.setData(new AngleChallengeData(
                discSize, request.isDebug() ? answer : null));
        return result;
    }

    @Override
    protected VerifyResult doVerify(CaptchaSession session, CaptchaAnswer answer) {
        if (answer == null || answer.getAngle() == null) {
            return VerifyResult.badRequest(CaptchaMessages.ANGLE_MISSING_ANGLE, messages);
        }
        Optional<String> behaviorError = behaviorValidator.validate(
                answer.getTd(), answer, session);
        if (behaviorError.isPresent()) {
            return VerifyResult.fail(behaviorError.get(), "BEHAVIOR", messages);
        }
        double diff = normalize(answer.getAngle() - session.getRotation());
        if (Math.abs(diff) <= options.getTolerance()) {
            return VerifyResult.ok(CaptchaMessages.VERIFY_OK, messages);
        }
        return VerifyResult.fail(CaptchaMessages.VERIFY_WRONG, "WRONG", messages);
    }

    @Override
    protected long minElapsedMs() {
        return options.getMinElapsedMs();
    }

    /** 把角度归一化到 [-180, 180) */
    private static double normalize(double degrees) {
        double value = degrees % 360;
        if (value > 180) {
            value -= 360;
        } else if (value < -180) {
            value += 360;
        }
        return value;
    }

    /** 把角度归一化到 [0, 360)，作为下发答案与前端旋转角度的统一口径 */
    private static double normalize360(double degrees) {
        double value = degrees % 360;
        return value < 0 ? value + 360 : value;
    }

    /** 返回 [min, max] 区间内的随机浮点数 */
    private double rand(double min, double max) {
        return min + random.nextDouble() * (max - min);
    }
}
