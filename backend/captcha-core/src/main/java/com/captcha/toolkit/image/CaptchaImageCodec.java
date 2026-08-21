package com.captcha.toolkit.image;

import java.awt.image.BufferedImage;

/**
 * 图片编码策略：把 BufferedImage 转成前端可用的字符串格式。
 */
public interface CaptchaImageCodec {

    /**
     * 把图片编码为字符串（如 Base64 Data URI）。
     *
     * @param image  图片
     * @param format 图片格式（png / jpg）
     * @return 编码后的字符串
     */
    String encode(BufferedImage image, String format);
}
