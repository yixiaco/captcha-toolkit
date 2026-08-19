package com.captcha.toolkit;

import com.captcha.toolkit.generator.CaptchaGenerator;
import com.captcha.toolkit.generator.ClickCaptchaGenerator;
import com.captcha.toolkit.render.BackgroundProvider;
import com.captcha.toolkit.render.SceneBackgroundProvider;
import com.captcha.toolkit.word.WordFactory;

/**
 * 文字点选验证码工厂。
 */
public class ClickCaptchaFactory implements CaptchaFactory {

    private final BackgroundProvider backgroundProvider;
    private final WordFactory wordFactory;

    public ClickCaptchaFactory() {
        this(new SceneBackgroundProvider(), null);
    }

    public ClickCaptchaFactory(BackgroundProvider backgroundProvider) {
        this(backgroundProvider, null);
    }

    public ClickCaptchaFactory(BackgroundProvider backgroundProvider, WordFactory wordFactory) {
        this.backgroundProvider = backgroundProvider;
        this.wordFactory = wordFactory;
    }

    @Override
    public CaptchaType type() {
        return CaptchaType.CLICK;
    }

    @Override
    public CaptchaGenerator create(CaptchaConfig config) {
        return new ClickCaptchaGenerator(config.getClick(), backgroundProvider, wordFactory);
    }
}
