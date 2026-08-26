package com.captcha.toolkit.render;

import com.captcha.toolkit.shape.ShapeGeometry;
import com.captcha.toolkit.shape.ShapePart;

import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;

/**
 * 形状几何统一渲染器。
 *
 * <p>填充单元用 {@code fill} 绘制；描边单元用其自带线宽 {@code draw} 绘制
 * （原生 Java2D 描边，保留贝塞尔曲线精度）；同时提供“描边转填充区域”的能力，
 * 供需要裁剪/蒙版的场景（滑块拼图块、刮刮乐内阴影）复用。</p>
 */
public final class ShapeRenderer {

    private ShapeRenderer() {
    }

    /**
     * 原生绘制几何模型：填充单元 fill，描边单元按线宽 draw。
     *
     * @param g        目标画布
     * @param geometry 形状几何模型
     * @param paint    填充/描边使用的颜色或渐变色
     */
    public static void draw(Graphics2D g, ShapeGeometry geometry, Paint paint) {
        g.setPaint(paint);
        for (ShapePart part : geometry.parts()) {
            if (part.filled()) {
                g.fill(part.path());
            }
            if (part.stroked()) {
                g.setStroke(part.stroke());
                g.draw(part.path());
            }
        }
    }

    /**
     * 填充几何模型的覆盖区域：描边单元先转换成带状区域再填充。
     *
     * <p>与 {@link #draw} 不同，本方法不保留原生描边精度，适用于蒙版、
     * 裁剪或内阴影等“只需要区域”的场景。</p>
     *
     * @param g        目标画布
     * @param geometry 形状几何模型
     * @param paint    填充颜色
     */
    public static void fillRegion(Graphics2D g, ShapeGeometry geometry, Paint paint) {
        g.setPaint(paint);
        for (ShapePart part : geometry.parts()) {
            g.fill(region(part));
        }
    }

    /**
     * 返回单个绘制单元的覆盖区域：填充单元为路径本身，描边单元为描边后的带状区域。
     *
     * @param part 绘制单元
     * @return 覆盖区域
     */
    public static Shape region(ShapePart part) {
        return part.stroked()
                ? part.stroke().createStrokedShape(part.path())
                : part.path();
    }

    /**
     * 把画布裁剪到单个绘制单元的覆盖区域。
     *
     * @param g    目标画布
     * @param part 绘制单元
     */
    public static void clip(Graphics2D g, ShapePart part) {
        g.setClip(region(part));
    }
}
