package com.captcha.toolkit.factory;

import com.captcha.toolkit.behavior.CurveBehaviorValidator;
import com.captcha.toolkit.config.CaptchaConfig;
import com.captcha.toolkit.generator.CaptchaGenerator;
import com.captcha.toolkit.generator.CurveCaptchaGenerator;
import com.captcha.toolkit.render.BackgroundProvider;
import com.captcha.toolkit.render.FallbackBackgroundProvider;
import com.captcha.toolkit.render.SceneBackgroundProvider;
import com.captcha.toolkit.type.CaptchaType;

import java.util.List;

/**
 * 曲线绘制验证码工厂。
 */
public class CurveCaptchaFactory implements CaptchaFactory {

    /** 曲线背景图提供者 */
    private final BackgroundProvider backgroundProvider;

    /** 使用程序生成背景 */
    public CurveCaptchaFactory() {
        this(new FallbackBackgroundProvider(List.of(new SceneBackgroundProvider())));
    }

    /**
     * @param backgroundProvider 背景图提供者
     */
    public CurveCaptchaFactory(BackgroundProvider backgroundProvider) {
        this.backgroundProvider = backgroundProvider;
    }

    @Override
    public CaptchaType type() {
        return CaptchaType.CURVE;
    }

    @Override
    public CaptchaGenerator<?> create(CaptchaConfig config) {
        return new CurveCaptchaGenerator(config.getCurve(), backgroundProvider,
                new CurveBehaviorValidator(config.getBehavior()), config.getMessageProvider());
    }
}
