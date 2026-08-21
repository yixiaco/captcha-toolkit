package com.captcha.toolkit.util;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * 图片处理工具：等比裁剪填充（cover）与高质量缩小，供滑块/点选复用。
 */
public final class ImageUtil {

    /** 工具类不可实例化 */
    private ImageUtil() {
    }

    /**
     * 把任意尺寸的图片等比缩放并居中裁剪到目标尺寸（照片墙 cover 效果）。
     */
    public static BufferedImage cover(BufferedImage src, int width, int height) {
        double scale = Math.max((double) width / src.getWidth(), (double) height / src.getHeight());
        int sw = (int) Math.ceil(src.getWidth() * scale);
        int sh = (int) Math.ceil(src.getHeight() * scale);
        BufferedImage scaled = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_RGB);
        Graphics2D sg = scaled.createGraphics();
        sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        sg.drawImage(src, 0, 0, sw, sh, null);
        sg.dispose();

        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(scaled, (width - sw) / 2, (height - sh) / 2, null);
        g.dispose();
        return out;
    }

    /**
     * 高质量缩小（保留 alpha），用于把高清渲染的字形缩回目标字号，消除锯齿。
     */
    public static BufferedImage scaleDown(BufferedImage src, int width, int height) {
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(src, 0, 0, width, height, null);
        g.dispose();
        return out;
    }
}
