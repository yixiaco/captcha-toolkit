package com.captcha.toolkit.image;

import java.awt.image.BufferedImage;

/**
 * 图片编码策略：把 BufferedImage 转成前端可用的字符串格式。
 */
public interface CaptchaImageCodec {

    String encode(BufferedImage image, String format);
}
