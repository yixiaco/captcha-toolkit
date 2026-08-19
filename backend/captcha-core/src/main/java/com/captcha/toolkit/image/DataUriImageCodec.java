package com.captcha.toolkit.image;

import com.captcha.toolkit.model.CaptchaException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * 默认编码：Base64 Data URI（PNG）。
 */
public class DataUriImageCodec implements CaptchaImageCodec {

    @Override
    public String encode(BufferedImage image, String format) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, format, out);
            return "data:image/" + format + ";base64,"
                    + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            throw new CaptchaException("图片编码失败", e);
        }
    }
}
