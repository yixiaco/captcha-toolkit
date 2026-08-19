package com.example.captcha.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * 图片转 Base64 Data URI
 */
public final class ImageConvertUtil {

    private ImageConvertUtil() {
    }

    public static String toDataUri(BufferedImage image, String format) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, format, out);
            return "data:image/" + format + ";base64,"
                    + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("图片编码失败", e);
        }
    }
}
