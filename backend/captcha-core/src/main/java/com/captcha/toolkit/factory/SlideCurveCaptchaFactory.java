package com.captcha.toolkit.factory;

import com.captcha.toolkit.behavior.SlideCurveBehaviorValidator;
import com.captcha.toolkit.config.CaptchaConfig;
import com.captcha.toolkit.generator.CaptchaGenerator;
import com.captcha.toolkit.generator.SlideCurveCaptchaGenerator;
import com.captcha.toolkit.render.BackgroundProvider;
import com.captcha.toolkit.render.FallbackBackgroundProvider;
import com.captcha.toolkit.render.SceneBackgroundProvider;
import com.captcha.toolkit.type.CaptchaType;

import java.util.List;

/**
 * 滑动曲线验证码工厂。
 */
public class SlideCurveCaptchaFactory implements CaptchaFactory {

    /** 滑动曲线背景图提供者 */
    private final BackgroundProvider backgroundProvider;

    /** 使用程序生成背景 */
    public SlideCurveCaptchaFactory() {
        this(new FallbackBackgroundProvider(List.of(new SceneBackgroundProvider())));
    }

    /**
     * @param backgroundProvider 背景图提供者
     */
    public SlideCurveCaptchaFactory(BackgroundProvider backgroundProvider) {
        this.backgroundProvider = backgroundProvider;
    }

    @Override
    public CaptchaType type() {
        return CaptchaType.SLIDE_CURVE;
    }

    @Override
    public CaptchaGenerator<?> create(CaptchaConfig config) {
        return new SlideCurveCaptchaGenerator(config.getSlideCurve(), backgroundProvider,
                new SlideCurveBehaviorValidator(config.getBehavior()),
                config.getMessageProvider());
    }
}
