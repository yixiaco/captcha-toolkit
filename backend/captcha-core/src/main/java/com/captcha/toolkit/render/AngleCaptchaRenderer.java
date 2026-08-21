package com.captcha.toolkit.render;

import com.captcha.toolkit.config.AngleConfig;
import com.captcha.toolkit.util.ImageUtil;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

/**
 * 角度验证码渲染器。
 *
 * <p>只生成一张圆形图：把背景场景按错位角度旋转后裁成圆形，
 * 场景本身具有“上下”方向（如天空在上、地面在下），
 * 用户把圆形图转回正立方向即完成验证；不带背景框、凹口或方向箭头。</p>
 */
public class AngleCaptchaRenderer {

    /** 角度验证渲染配置 */
    private final AngleConfig options;

    /**
     * @param options 角度验证配置
     */
    public AngleCaptchaRenderer(AngleConfig options) {
        this.options = options;
    }

    /**
     * 渲染角度验证码的圆形图（正方形画布，圆外透明）。
     *
     * @param raw         原始背景图
     * @param markerAngle 圆盘方向标记的初始错位角度（度）
     * @return 圆形图（整幅画布大小，圆外透明，已缩放到目标尺寸）
     */
    public BufferedImage render(BufferedImage raw, double markerAngle) {
        int discSize = discSize();
        int scale = Math.max(1, options.getRenderScale());
        int hiSize = discSize * scale;
        BufferedImage thumb = ImageUtil.cover(raw, hiSize, hiSize);

        int cx = hiSize / 2;
        int cy = hiSize / 2;
        int radius = hiSize / 2;

        // 圆盘：正方形画布，场景按错位角度旋转后裁成圆形，圆外透明
        BufferedImage disc = new BufferedImage(hiSize, hiSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = disc.createGraphics();
        enableAntialias(g);
        g.rotate(Math.toRadians(markerAngle), cx, cy);
        g.setClip(new Ellipse2D.Double(0, 0, hiSize, hiSize));
        g.drawImage(thumb, 0, 0, null);
        g.dispose();

        return ImageUtil.scaleDown(disc, discSize, discSize);
    }

    /** 圆盘直径（像素）：短边 × 2 × 半径比例 */
    public int discSize() {
        return (int) Math.round(
                Math.min(options.getWidth(), options.getHeight())
                        * 2 * options.getDiscRadiusRatio());
    }

    /** 打开抗锯齿与高质量渲染 */
    private static void enableAntialias(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

}
