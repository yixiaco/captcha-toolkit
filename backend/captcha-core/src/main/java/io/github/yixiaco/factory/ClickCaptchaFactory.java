package io.github.yixiaco.factory;

import io.github.yixiaco.config.CaptchaConfig;
import io.github.yixiaco.behavior.ClickBehaviorValidator;
import io.github.yixiaco.generator.CaptchaGenerator;
import io.github.yixiaco.generator.ClickCaptchaGenerator;
import io.github.yixiaco.render.BackgroundProvider;
import io.github.yixiaco.render.SceneBackgroundProvider;
import io.github.yixiaco.type.CaptchaType;
import io.github.yixiaco.word.WordFactory;

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
