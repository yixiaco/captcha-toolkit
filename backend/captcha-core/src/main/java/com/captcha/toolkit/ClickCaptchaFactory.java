package com.captcha.toolkit;

import com.captcha.toolkit.config.CaptchaConfig;
import com.captcha.toolkit.behavior.ClickBehaviorValidator;
import com.captcha.toolkit.generator.CaptchaGenerator;
import com.captcha.toolkit.generator.ClickCaptchaGenerator;
import com.captcha.toolkit.render.BackgroundProvider;
import com.captcha.toolkit.render.SceneBackgroundProvider;
import com.captcha.toolkit.word.WordFactory;

/**
 * 文字点选验证码工厂。
 */
public class ClickCaptchaFactory implements CaptchaFactory {

    /** 点选背景图提供者 */
    private final BackgroundProvider backgroundProvider;

    /** 点选目标词组工厂 */
    private final WordFactory wordFactory;

    /** 使用程序生成背景与默认词组来源 */
    public ClickCaptchaFactory() {
        this(new SceneBackgroundProvider(), null);
    }

    /**
     * @param backgroundProvider 背景图提供者
     */
    public ClickCaptchaFactory(BackgroundProvider backgroundProvider) {
        this(backgroundProvider, null);
    }

    /**
     * @param backgroundProvider 背景图提供者
     * @param wordFactory        目标词组工厂
     */
    public ClickCaptchaFactory(BackgroundProvider backgroundProvider, WordFactory wordFactory) {
        this.backgroundProvider = backgroundProvider;
        this.wordFactory = wordFactory;
    }

    @Override
    public CaptchaType type() {
        return CaptchaType.CLICK;
    }

    @Override
    public CaptchaGenerator<?> create(CaptchaConfig config) {
        return new ClickCaptchaGenerator(config.getClick(), backgroundProvider, wordFactory,
                new ClickBehaviorValidator(config.getBehavior()), config.getMessageProvider());
    }
}
