package io.github.yixiaco.factory;

import io.github.yixiaco.behavior.SlideCurveBehaviorValidator;
import io.github.yixiaco.config.CaptchaConfig;
import io.github.yixiaco.generator.CaptchaGenerator;
import io.github.yixiaco.generator.SlideCurveCaptchaGenerator;
import io.github.yixiaco.render.BackgroundProvider;
import io.github.yixiaco.render.FallbackBackgroundProvider;
import io.github.yixiaco.render.SceneBackgroundProvider;
import io.github.yixiaco.type.CaptchaType;

import java.util.List;

/**
 * 滑动曲线验证码工厂。
 */
public class SlideCurveCaptchaFactory implements CaptchaFactory {

    /** 滑动曲线背景图提供者 */
    private final BackgroundProvider backgroundProvider;

    /** 使用程序生成背景 */
    public SlideCurveCaptchaFactory() {
        this(new FallbackBackgroundProvider(List.of(new SceneBackgroundProvider())));
    }

    /**
     * @param backgroundProvider 背景图提供者
     */
    public SlideCurveCaptchaFactory(BackgroundProvider backgroundProvider) {
        this.backgroundProvider = backgroundProvider;
    }

    @Override
    public CaptchaType type() {
        return CaptchaType.SLIDE_CURVE;
    }

    @Override
    public CaptchaGenerator<?> create(CaptchaConfig config) {
        return new SlideCurveCaptchaGenerator(config.getSlideCurve(), backgroundProvider,
                new SlideCurveBehaviorValidator(config.getBehavior()),
                config.getMessageProvider());
    }
}
