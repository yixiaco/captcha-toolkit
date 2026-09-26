package io.github.yixiaco.factory;

import io.github.yixiaco.behavior.ClickBehaviorValidator;
import io.github.yixiaco.config.CaptchaConfig;
import io.github.yixiaco.generator.CaptchaGenerator;
import io.github.yixiaco.generator.ClickShapeCaptchaGenerator;
import io.github.yixiaco.render.BackgroundProvider;
import io.github.yixiaco.render.SceneBackgroundProvider;
import io.github.yixiaco.shape.PuzzleShapeRegistry;
import io.github.yixiaco.type.CaptchaType;

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
