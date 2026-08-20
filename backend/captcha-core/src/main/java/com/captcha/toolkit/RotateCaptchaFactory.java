package com.captcha.toolkit;

import com.captcha.toolkit.config.CaptchaConfig;
import com.captcha.toolkit.generator.CaptchaGenerator;
import com.captcha.toolkit.generator.RotateCaptchaGenerator;
import com.captcha.toolkit.render.BackgroundProvider;
import com.captcha.toolkit.render.FallbackBackgroundProvider;
import com.captcha.toolkit.render.SceneBackgroundProvider;

import java.util.List;

/**
 * 图片旋转验证码工厂。
 */
public class RotateCaptchaFactory implements CaptchaFactory {

    private final BackgroundProvider backgroundProvider;

    public RotateCaptchaFactory() {
        this(new FallbackBackgroundProvider(List.of(new SceneBackgroundProvider())));
    }

    public RotateCaptchaFactory(BackgroundProvider backgroundProvider) {
        this.backgroundProvider = backgroundProvider;
    }

    @Override
    public CaptchaType type() {
        return CaptchaType.ROTATE;
    }

    @Override
    public CaptchaGenerator create(CaptchaConfig config) {
        return new RotateCaptchaGenerator(config.getRotate(), backgroundProvider);
    }
}
