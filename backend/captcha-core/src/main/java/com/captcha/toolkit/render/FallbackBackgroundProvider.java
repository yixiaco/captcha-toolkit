package com.captcha.toolkit.render;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 背景图失败回退链：依次尝试每个提供者，全部失败才返回空。
 */
public class FallbackBackgroundProvider implements BackgroundProvider {

    /** 回退链：按顺序尝试每个提供者 */
    private final List<BackgroundProvider> delegates;

    /**
     * @param delegates 回退提供者列表
     */
    public FallbackBackgroundProvider(List<BackgroundProvider> delegates) {
        this.delegates = delegates == null ? List.of() : new ArrayList<>(delegates);
    }

    /**
     * 便捷构造：素材提供者 + 可选程序生成兜底。
     *
     * @param sources          素材来源列表
     * @param generateFallback 素材全部失败时是否使用程序生成
     * @return 组装好的回退提供者
     */
    public static FallbackBackgroundProvider of(List<String> sources, boolean generateFallback) {
        List<BackgroundProvider> providers = new ArrayList<>();
        if (sources != null && !sources.isEmpty()) {
            providers.add(new ResourceBackgroundProvider(sources));
        }
        if (generateFallback) {
            providers.add(new SceneBackgroundProvider());
        }
        return new FallbackBackgroundProvider(providers);
    }

    @Override
    public Optional<BufferedImage> provide(int width, int height) {
        for (BackgroundProvider delegate : delegates) {
            Optional<BufferedImage> image = delegate.provide(width, height);
            if (image.isPresent()) {
                return image;
            }
        }
        return Optional.empty();
    }
}
