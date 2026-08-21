package com.captcha.toolkit.factory;

import com.captcha.toolkit.behavior.AngleBehaviorValidator;
import com.captcha.toolkit.config.CaptchaConfig;
import com.captcha.toolkit.generator.AngleCaptchaGenerator;
import com.captcha.toolkit.generator.CaptchaGenerator;
import com.captcha.toolkit.render.BackgroundProvider;
import com.captcha.toolkit.render.FallbackBackgroundProvider;
import com.captcha.toolkit.render.SceneBackgroundProvider;
import com.captcha.toolkit.type.CaptchaType;

import java.util.List;

/**
 * 角度验证码工厂。
 */
public class AngleCaptchaFactory implements CaptchaFactory {

    /** 角度验证背景图提供者 */
    private final BackgroundProvider backgroundProvider;

    /** 使用程序生成背景 */
    public AngleCaptchaFactory() {
        this(new FallbackBackgroundProvider(List.of(new SceneBackgroundProvider())));
    }

    /**
     * @param backgroundProvider 背景图提供者
     */
    public AngleCaptchaFactory(BackgroundProvider backgroundProvider) {
        this.backgroundProvider = backgroundProvider;
    }

    @Override
    public CaptchaType type() {
        return CaptchaType.ANGLE;
    }

    @Override
    public CaptchaGenerator<?> create(CaptchaConfig config) {
        return new AngleCaptchaGenerator(config.getAngle(), backgroundProvider,
                new AngleBehaviorValidator(config.getBehavior()), config.getMessageProvider());
    }
}
