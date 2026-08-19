package com.captcha.toolkit.render;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * 从 classpath / 文件系统读取背景图：
 * 支持 "classpath:/images/a.jpg"、"/images/a.jpg"、"./a.jpg"、"D:/a.jpg"、"file:D:/a.jpg" 等写法。
 */
public class ResourceBackgroundProvider implements BackgroundProvider {

    private final List<String> sources;
    private final ClassLoader classLoader;
    private final Random random = new Random();

    public ResourceBackgroundProvider(List<String> sources) {
        this(sources, Thread.currentThread().getContextClassLoader());
    }

    public ResourceBackgroundProvider(List<String> sources, ClassLoader classLoader) {
        this.sources = sources == null ? List.of() : new ArrayList<>(sources);
        this.classLoader = classLoader == null
                ? Thread.currentThread().getContextClassLoader()
                : classLoader;
    }

    @Override
    public Optional<BufferedImage> provide(int width, int height) {
        List<String> candidates = new ArrayList<>(sources);
        Collections.shuffle(candidates, random);
        for (String source : candidates) {
            BufferedImage image = read(source);
            if (image != null) {
                return Optional.of(image);
            }
        }
        return Optional.empty();
    }

    private BufferedImage read(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        String path = source.trim();
        try {
            if (path.startsWith("file:")) {
                Path file = Paths.get(path.substring("file:".length()));
                if (Files.exists(file)) {
                    try (InputStream in = Files.newInputStream(file)) {
                        return ImageIO.read(in);
                    }
                }
                return null;
            }
            if (path.startsWith("classpath:")) {
                path = path.substring("classpath:".length());
            }
            try (InputStream in = classLoader.getResourceAsStream(path)) {
                if (in != null) {
                    return ImageIO.read(in);
                }
            }
            Path file = Paths.get(path);
            if (Files.exists(file)) {
                try (InputStream in = Files.newInputStream(file)) {
                    return ImageIO.read(in);
                }
            }
        } catch (IOException | RuntimeException ignored) {
            // 单个素材失败不影响其他素材
        }
        return null;
    }
}
