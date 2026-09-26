package io.github.yixiaco.shape;

import java.awt.BasicStroke;
import java.awt.geom.Path2D;

/**
 * 形状的一个绘制单元。
 *
 * <p>一条路径可以同时携带填充与描边（对应 SVG 元素的 fill / stroke 属性），
 * 也允许只有描边（如非闭合线条、线稿图标）或只有填充。
 * 描边样式当前保留线宽，端点与连接固定为圆角，颜色由使用方决定，
 * 这样同一个几何模型既能用于滑块裁剪（取背景纹理），也能用于刮刮乐着色。</p>
 */
public record ShapePart(Path2D path, boolean filled, boolean stroked, double strokeWidth) {

    /** 纯填充绘制单元 */
    public static ShapePart filled(Path2D path) {
        return new ShapePart(path, true, false, 0);
    }

    /** 纯描边绘制单元（线宽按当前图形坐标单位） */
    public static ShapePart stroked(Path2D path, double strokeWidth) {
        return new ShapePart(path, false, true, Math.max(0, strokeWidth));
    }

    /** 填充 + 描边绘制单元 */
    public static ShapePart filledAndStroked(Path2D path, double strokeWidth) {
        return new ShapePart(path, true, true, Math.max(0, strokeWidth));
    }

    /** 当前单元对应的 Java2D 描边样式（圆角端点与圆角连接） */
    public BasicStroke stroke() {
        return new BasicStroke((float) strokeWidth,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }
}
