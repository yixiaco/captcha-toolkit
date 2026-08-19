package com.captcha.toolkit;

import com.captcha.toolkit.generator.CaptchaGenerator;
import com.captcha.toolkit.generator.ClickCaptchaGenerator;
import com.captcha.toolkit.render.BackgroundProvider;
import com.captcha.toolkit.render.SceneBackgroundProvider;

/**
 * 文字点选验证码工厂。
 */
public class ClickCaptchaFactory implements CaptchaFactory {

    private final BackgroundProvider backgroundProvider;

    public ClickCaptchaFactory() {
        this(new SceneBackgroundProvider());
    }

    public ClickCaptchaFactory(BackgroundProvider backgroundProvider) {
        this.backgroundProvider = backgroundProvider;
    }

    @Override
    public CaptchaType type() {
        return CaptchaType.CLICK;
    }

    @Override
    public CaptchaGenerator create(CaptchaConfig config) {
        return new ClickCaptchaGenerator(config.getClick(), backgroundProvider);
    }
}
