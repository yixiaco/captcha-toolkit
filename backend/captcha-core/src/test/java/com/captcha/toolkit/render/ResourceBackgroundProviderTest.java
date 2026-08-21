package com.captcha.toolkit.render;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 背景图资源加载测试：重点覆盖带前导 "/" 的 classpath 路径。
 */
class ResourceBackgroundProviderTest {

    @Test
    void leadingSlashClasspathPathLoadsImage() throws IOException {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, "png", bytes),
                "测试图片写入失败");
        byte[] png = bytes.toByteArray();

        // 模拟真实 classpath：只认识不带前导 "/" 的资源名，
        // 用于验证提供器会先去掉 "/" 再交给 ClassLoader 查找。
        ClassLoader loader = new ClassLoader(null) {
            @Override
            public InputStream getResourceAsStream(String name) {
                return "images/captcha/default.png".equals(name)
                        ? new ByteArrayInputStream(png)
                        : null;
            }
        };
        ResourceBackgroundProvider provider =
                new ResourceBackgroundProvider(List.of("/images/captcha/default.png"), loader);

        assertTrue(provider.provide(1, 1).isPresent(),
                "带前导 / 的 classpath 路径应能加载图片");
    }
}
