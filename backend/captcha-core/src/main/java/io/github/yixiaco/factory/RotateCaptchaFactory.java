package io.github.yixiaco.factory;

import io.github.yixiaco.config.CaptchaConfig;
import io.github.yixiaco.behavior.RotateBehaviorValidator;
import io.github.yixiaco.generator.CaptchaGenerator;
import io.github.yixiaco.generator.RotateCaptchaGenerator;
import io.github.yixiaco.render.BackgroundProvider;
import io.github.yixiaco.render.FallbackBackgroundProvider;
import io.github.yixiaco.render.SceneBackgroundProvider;
import io.github.yixiaco.type.CaptchaType;

import java.util.List;

/**
 * 图片旋转验证码工厂。
 */
public class RotateCaptchaFactory implements CaptchaFactory {

    /** 旋转背景图提供者 */
    private final BackgroundProvider backgroundProvider;

    /** 使用程序生成背景 */
    public RotateCaptchaFactory() {
        this(new FallbackBackgroundProvider(List.of(new SceneBackgroundProvider())));
    }

    /**
     * @param backgroundProvider 背景图提供者
     */
    public RotateCaptchaFactory(BackgroundProvider backgroundProvider) {
        this.backgroundProvider = backgroundProvider;
    }

    @Override
    public CaptchaType type() {
        return CaptchaType.ROTATE;
    }

    @Override
    public CaptchaGenerator<?> create(CaptchaConfig config) {
        return new RotateCaptchaGenerator(config.getRotate(), backgroundProvider,
                new RotateBehaviorValidator(config.getBehavior()), config.getMessageProvider());
    }
}
