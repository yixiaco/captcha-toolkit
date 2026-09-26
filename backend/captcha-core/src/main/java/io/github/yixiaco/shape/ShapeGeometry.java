package io.github.yixiaco.shape;

import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * 形状几何模型：一个形状由多个绘制单元组成。
 *
 * <p>相比旧的“单条闭合路径填充”模型，本模型支持非闭合线条、多条路径组合、
 * 填充与描边混用，以及描边宽度随图形等比缩放。
 * 渲染器（滑块裁剪、刮刮乐绘制、提示词图片）统一消费本模型；
 * 后续新增渐变、虚线、透明度等绘制方式时，只需扩展 {@link ShapePart}。</p>
 */
public final class ShapeGeometry {

    /** 全部绘制单元（不可变列表） */
    private final List<ShapePart> parts;

    /** 覆盖区域边界（已包含描边外扩部分） */
    private final Rectangle2D bounds;

    private ShapeGeometry(List<ShapePart> parts) {
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("形状至少需要一个绘制单元");
        }
        this.parts = List.copyOf(parts);
        this.bounds = computeBounds(this.parts);
    }

    /** 创建单个纯填充单元的几何模型 */
    public static ShapeGeometry filled(Path2D path) {
        return new ShapeGeometry(List.of(ShapePart.filled(path)));
    }

    /** 由多个绘制单元创建几何模型 */
    public static ShapeGeometry of(List<ShapePart> parts) {
        return new ShapeGeometry(parts);
    }

    /** 返回全部绘制单元（只读） */
    public List<ShapePart> parts() {
        return parts;
    }

    /** 返回覆盖区域边界（含描边宽度，调用方可直接用于裁剪/适配） */
    public Rectangle2D bounds() {
        return (Rectangle2D) bounds.clone();
    }

    /** 等比缩放：路径坐标与描边宽度一起缩放 */
    public ShapeGeometry scaled(double scale) {
        List<ShapePart> result = new ArrayList<>(parts.size());
        AffineTransform transform = AffineTransform.getScaleInstance(scale, scale);
        for (ShapePart part : parts) {
            result.add(new ShapePart(
                    transformPath(part.path(), transform),
                    part.filled(), part.stroked(), part.strokeWidth() * scale));
        }
        return new ShapeGeometry(result);
    }

    /** 平移：路径整体位移，描边宽度不变 */
    public ShapeGeometry translated(double dx, double dy) {
        List<ShapePart> result = new ArrayList<>(parts.size());
        AffineTransform transform = AffineTransform.getTranslateInstance(dx, dy);
        for (ShapePart part : parts) {
            result.add(new ShapePart(
                    transformPath(part.path(), transform),
                    part.filled(), part.stroked(), part.strokeWidth()));
        }
        return new ShapeGeometry(result);
    }

    /** 绕 (cx, cy) 旋转指定角度（度），描边宽度不变 */
    public ShapeGeometry rotated(double angleDeg, double cx, double cy) {
        List<ShapePart> result = new ArrayList<>(parts.size());
        AffineTransform transform = AffineTransform.getRotateInstance(
                Math.toRadians(angleDeg), cx, cy);
        for (ShapePart part : parts) {
            result.add(new ShapePart(
                    transformPath(part.path(), transform),
                    part.filled(), part.stroked(), part.strokeWidth()));
        }
        return new ShapeGeometry(result);
    }

    /**
     * 把几何模型合并成一条可填充的路径（兼容旧接口）。
     *
     * <p>描边单元会先转换成描边后的带状区域，再与填充单元取并集；
     * 多单元场景使用 {@link Area} 合并，曲线会被离散化，
     * 因此仅供几何兜底/测试使用，正式渲染应走渲染器的原生描边。</p>
     */
    public Path2D toFilledPath() {
        if (parts.size() == 1) {
            return (Path2D) parts.get(0).path().clone();
        }
        Area area = new Area();
        for (ShapePart part : parts) {
            if (part.stroked()) {
                area.add(new Area(part.stroke().createStrokedShape(part.path())));
            } else {
                area.add(new Area(part.path()));
            }
        }
        return new Path2D.Double(area);
    }

    /** 把一条路径按变换复制为新路径 */
    private static Path2D transformPath(Path2D path, AffineTransform transform) {
        Path2D result = new Path2D.Double(path.getWindingRule());
        result.append(path.getPathIterator(transform), false);
        return result;
    }

    /** 计算覆盖边界：路径边界 + 描边宽度的一半外扩 */
    private static Rectangle2D computeBounds(List<ShapePart> parts) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (ShapePart part : parts) {
            Rectangle2D pathBounds = part.path().getBounds2D();
            double expand = part.stroked() ? part.strokeWidth() / 2.0 : 0;
            minX = Math.min(minX, pathBounds.getMinX() - expand);
            minY = Math.min(minY, pathBounds.getMinY() - expand);
            maxX = Math.max(maxX, pathBounds.getMaxX() + expand);
            maxY = Math.max(maxY, pathBounds.getMaxY() + expand);
        }
        return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
    }
}
