package io.github.yixiaco.factory;

import io.github.yixiaco.config.CaptchaConfig;
import io.github.yixiaco.behavior.SliderBehaviorValidator;
import io.github.yixiaco.generator.CaptchaGenerator;
import io.github.yixiaco.generator.SliderCaptchaGenerator;
import io.github.yixiaco.render.BackgroundProvider;
import io.github.yixiaco.render.FallbackBackgroundProvider;
import io.github.yixiaco.render.SceneBackgroundProvider;
import io.github.yixiaco.shape.PuzzleShapeRegistry;
import io.github.yixiaco.type.CaptchaType;

import java.util.List;

/**
 * 滑块验证码工厂：负责把滑块参数、背景策略、形状注册表组装成生成器。
 */
public class SliderCaptchaFactory implements CaptchaFactory {

    /** 滑块背景图提供者 */
    private final BackgroundProvider backgroundProvider;

    /** 拼图形状注册表 */
    private final PuzzleShapeRegistry shapeRegistry;

    /** 使用程序生成背景与默认形状注册表 */
    public SliderCaptchaFactory() {
        this(new FallbackBackgroundProvider(List.of(new SceneBackgroundProvider())));
    }

    /**
     * @param backgroundProvider 背景图提供者
     */
    public SliderCaptchaFactory(BackgroundProvider backgroundProvider) {
        this(backgroundProvider, new PuzzleShapeRegistry());
    }

    /**
     * @param backgroundProvider 背景图提供者
     * @param shapeRegistry      拼图形状注册表
     */
    public SliderCaptchaFactory(BackgroundProvider backgroundProvider, PuzzleShapeRegistry shapeRegistry) {
        this.backgroundProvider = backgroundProvider;
        this.shapeRegistry = shapeRegistry;
    }

    @Override
    public CaptchaType type() {
        return CaptchaType.SLIDER;
    }

    @Override
    public CaptchaGenerator<?> create(CaptchaConfig config) {
        return new SliderCaptchaGenerator(config.getSlider(), backgroundProvider, shapeRegistry,
                new SliderBehaviorValidator(config.getBehavior()), config.getMessageProvider());
    }
}
