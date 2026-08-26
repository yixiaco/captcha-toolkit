package com.captcha.toolkit.factory;

import com.captcha.toolkit.behavior.ClickBehaviorValidator;
import com.captcha.toolkit.config.CaptchaConfig;
import com.captcha.toolkit.generator.CaptchaGenerator;
import com.captcha.toolkit.generator.ClickShapeCaptchaGenerator;
import com.captcha.toolkit.render.BackgroundProvider;
import com.captcha.toolkit.render.SceneBackgroundProvider;
import com.captcha.toolkit.shape.PuzzleShapeRegistry;
import com.captcha.toolkit.type.CaptchaType;

/**
 * 图形点选验证码工厂。
 */
public class ClickShapeCaptchaFactory implements CaptchaFactory {

    /** 图形点选背景图提供者 */
    private final BackgroundProvider backgroundProvider;

    /** 图形形状注册表 */
    private final PuzzleShapeRegistry shapeRegistry;

    /** 使用程序生成背景与默认形状注册表 */
    public ClickShapeCaptchaFactory() {
        this(new SceneBackgroundProvider(), new PuzzleShapeRegistry());
    }

    /**
     * @param backgroundProvider 背景图提供者
     */
    public ClickShapeCaptchaFactory(BackgroundProvider backgroundProvider) {
        this(backgroundProvider, new PuzzleShapeRegistry());
    }

    /**
     * @param backgroundProvider 背景图提供者
     * @param shapeRegistry      图形形状注册表
     */
    public ClickShapeCaptchaFactory(BackgroundProvider backgroundProvider,
                                    PuzzleShapeRegistry shapeRegistry) {
        this.backgroundProvider = backgroundProvider;
        this.shapeRegistry = shapeRegistry;
    }

    @Override
    public CaptchaType type() {
        return CaptchaType.SHAPE_CLICK;
    }

    @Override
    public CaptchaGenerator<?> create(CaptchaConfig config) {
        return new ClickShapeCaptchaGenerator(config.getClickShape(),
                backgroundProvider, shapeRegistry,
                new ClickBehaviorValidator(config.getBehavior()),
                config.getMessageProvider());
    }
}
