package io.github.yixiaco.factory;

import io.github.yixiaco.behavior.ScratchBehaviorValidator;
import io.github.yixiaco.config.CaptchaConfig;
import io.github.yixiaco.generator.CaptchaGenerator;
import io.github.yixiaco.generator.ScratchCaptchaGenerator;
import io.github.yixiaco.render.BackgroundProvider;
import io.github.yixiaco.render.FallbackBackgroundProvider;
import io.github.yixiaco.render.SceneBackgroundProvider;
import io.github.yixiaco.type.CaptchaType;

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
