package com.captcha.toolkit.render;

import java.awt.image.BufferedImage;
import java.util.Optional;

/**
 * 背景图提供策略：滑块与点选共用，支持资源图、文件图或程序生成图。
 */
@FunctionalInterface
public interface BackgroundProvider {

    /** 返回一张尺寸尽量接近 width*height 的背景图；无法提供时返回 Optional.empty() */
    Optional<BufferedImage> provide(int width, int height);
}
