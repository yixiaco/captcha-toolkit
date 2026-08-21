package com.captcha.toolkit;

import com.captcha.toolkit.config.CaptchaConfig;
import com.captcha.toolkit.behavior.SliderBehaviorValidator;
import com.captcha.toolkit.generator.CaptchaGenerator;
import com.captcha.toolkit.generator.SliderCaptchaGenerator;
import com.captcha.toolkit.render.BackgroundProvider;
import com.captcha.toolkit.render.FallbackBackgroundProvider;
import com.captcha.toolkit.render.SceneBackgroundProvider;
import com.captcha.toolkit.shape.PuzzleShapeRegistry;

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
    public CaptchaGenerator create(CaptchaConfig config) {
        return new SliderCaptchaGenerator(config.getSlider(), backgroundProvider, shapeRegistry,
                new SliderBehaviorValidator(config.getBehavior()));
    }
}
