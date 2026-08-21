package com.captcha.toolkit.factory;

import com.captcha.toolkit.behavior.ScratchBehaviorValidator;
import com.captcha.toolkit.config.CaptchaConfig;
import com.captcha.toolkit.generator.CaptchaGenerator;
import com.captcha.toolkit.generator.ScratchCaptchaGenerator;
import com.captcha.toolkit.render.BackgroundProvider;
import com.captcha.toolkit.render.FallbackBackgroundProvider;
import com.captcha.toolkit.render.SceneBackgroundProvider;
import com.captcha.toolkit.type.CaptchaType;

import java.util.List;

/**
 * 刮刮乐验证码工厂。
 */
public class ScratchCaptchaFactory implements CaptchaFactory {

    /** 刮刮乐背景图提供者 */
    private final BackgroundProvider backgroundProvider;

    /** 使用程序生成背景 */
    public ScratchCaptchaFactory() {
        this(new FallbackBackgroundProvider(List.of(new SceneBackgroundProvider())));
    }

    /**
     * @param backgroundProvider 背景图提供者
     */
    public ScratchCaptchaFactory(BackgroundProvider backgroundProvider) {
        this.backgroundProvider = backgroundProvider;
    }

    @Override
    public CaptchaType type() {
        return CaptchaType.SCRATCH;
    }

    @Override
    public CaptchaGenerator<?> create(CaptchaConfig config) {
        return new ScratchCaptchaGenerator(config.getScratch(), backgroundProvider,
                new ScratchBehaviorValidator(config.getBehavior()), config.getMessageProvider());
    }
}
