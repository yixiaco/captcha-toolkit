package io.github.yixiaco.factory;

import io.github.yixiaco.behavior.SwingTileBehaviorValidator;
import io.github.yixiaco.config.CaptchaConfig;
import io.github.yixiaco.generator.CaptchaGenerator;
import io.github.yixiaco.generator.SwingTileCaptchaGenerator;
import io.github.yixiaco.render.BackgroundProvider;
import io.github.yixiaco.render.FallbackBackgroundProvider;
import io.github.yixiaco.render.SceneBackgroundProvider;
import io.github.yixiaco.shape.PuzzleShapeRegistry;
import io.github.yixiaco.type.CaptchaType;

import java.util.List;

/**
 * 滑块摆动图块验证码工厂。
 */
public class SwingTileCaptchaFactory implements CaptchaFactory {

    /** 滑块摆动图块背景图提供者 */
    private final BackgroundProvider backgroundProvider;

    /** 拼图形状注册表 */
    private final PuzzleShapeRegistry shapeRegistry;

    /** 使用程序生成背景与默认形状注册表 */
    public SwingTileCaptchaFactory() {
        this(new FallbackBackgroundProvider(List.of(new SceneBackgroundProvider())),
                new PuzzleShapeRegistry());
    }

    /**
     * @param backgroundProvider 背景图提供者
     */
    public SwingTileCaptchaFactory(BackgroundProvider backgroundProvider) {
        this(backgroundProvider, new PuzzleShapeRegistry());
    }

    /**
     * @param backgroundProvider 背景图提供者
     * @param shapeRegistry      拼图形状注册表
     */
    public SwingTileCaptchaFactory(BackgroundProvider backgroundProvider,
                                   PuzzleShapeRegistry shapeRegistry) {
        this.backgroundProvider = backgroundProvider;
        this.shapeRegistry = shapeRegistry;
    }

    @Override
    public CaptchaType type() {
        return CaptchaType.SWING_TILE;
    }

    @Override
    public CaptchaGenerator<?> create(CaptchaConfig config) {
        return new SwingTileCaptchaGenerator(config.getSwingTile(), backgroundProvider,
                shapeRegistry, new SwingTileBehaviorValidator(config.getBehavior()),
                config.getMessageProvider());
    }
}
