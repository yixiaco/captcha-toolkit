package io.github.yixiaco.factory;

import io.github.yixiaco.behavior.CurveBehaviorValidator;
import io.github.yixiaco.config.CaptchaConfig;
import io.github.yixiaco.generator.CaptchaGenerator;
import io.github.yixiaco.generator.CurveCaptchaGenerator;
import io.github.yixiaco.render.BackgroundProvider;
import io.github.yixiaco.render.FallbackBackgroundProvider;
import io.github.yixiaco.render.SceneBackgroundProvider;
import io.github.yixiaco.type.CaptchaType;

import java.util.List;

/**
 * 曲线绘制验证码工厂。
 */
public class CurveCaptchaFactory implements CaptchaFactory {

    /** 曲线背景图提供者 */
    private final BackgroundProvider backgroundProvider;

    /** 使用程序生成背景 */
    public CurveCaptchaFactory() {
        this(new FallbackBackgroundProvider(List.of(new SceneBackgroundProvider())));
    }

    /**
     * @param backgroundProvider 背景图提供者
     */
    public CurveCaptchaFactory(BackgroundProvider backgroundProvider) {
        this.backgroundProvider = backgroundProvider;
    }

    @Override
    public CaptchaType type() {
        return CaptchaType.CURVE;
    }

    @Override
    public CaptchaGenerator<?> create(CaptchaConfig config) {
        return new CurveCaptchaGenerator(config.getCurve(), backgroundProvider,
                new CurveBehaviorValidator(config.getBehavior()), config.getMessageProvider());
    }
}
