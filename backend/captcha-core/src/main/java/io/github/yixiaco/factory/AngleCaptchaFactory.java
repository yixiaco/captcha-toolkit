package io.github.yixiaco.factory;

import io.github.yixiaco.behavior.AngleBehaviorValidator;
import io.github.yixiaco.config.CaptchaConfig;
import io.github.yixiaco.generator.AngleCaptchaGenerator;
import io.github.yixiaco.generator.CaptchaGenerator;
import io.github.yixiaco.render.BackgroundProvider;
import io.github.yixiaco.render.FallbackBackgroundProvider;
import io.github.yixiaco.render.SceneBackgroundProvider;
import io.github.yixiaco.type.CaptchaType;

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
