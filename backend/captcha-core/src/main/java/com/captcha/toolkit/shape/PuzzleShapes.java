package com.captcha.toolkit.shape;

import com.captcha.toolkit.util.SvgPathParser;

import java.awt.geom.Arc2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * 内置拼图形状集合。
 *
 * <p>经典形状严格参考 puzzle_captcha：3x3 方块，上边内凹、右边外凸、左边内凹。</p>
 */
public final class PuzzleShapes {

    /** 工具类不可实例化 */
    private PuzzleShapes() {
    }

    /** 经典 3x3 拼图块 */
    public static PuzzleShape classic() {
        return named("classic", "经典", PuzzleShapes::createClassic);
    }

    /** 叶子形状 */
    public static PuzzleShape leaf() {
        return named("leaf", "叶子", (x, y, size) -> {
            Path2D path = new Path2D.Double();
            path.append(LEAF_TEMPLATE.getPathIterator(null), false);
            fitToBox(path, x, y, size);
            return path;
        });
    }

    /** 参考 Leaf.svg 的树叶路径（viewBox 1024×1024，解析一次后复用） */
    private static final Path2D LEAF_TEMPLATE = SvgPathParser.parse(
            "M853.333333 128h-171.264 "
                    + "C316.16 128 133.802667 281.301333 127.573333 542.250667 "
                    + "l-0.128 21.973333 "
                    + "c0.597333 74.538667 17.621333 138.325333 68.266667 201.216 "
                    + "a726.186667 726.186667 0 0 0-24.746667 125.866667 "
                    + "42.666667 42.666667 0 1 0 84.778667 9.386666 "
                    + "c3.541333-31.744 8.832-61.738667 16-90.026666 "
                    + "H384 "
                    + "c309.717333 0 490.624-180.992 511.914667-552.234667 "
                    + "L896 170.666667 "
                    + "a42.666667 42.666667 0 0 0-42.666667-42.666667 "
                    + "z");

    /** 三角形 */
    public static PuzzleShape triangle() {
        return named("triangle", "三角", (x, y, size) -> {
            Path2D path = new Path2D.Double();
            path.moveTo(x + size * 0.5, y + size * 0.04);
            path.lineTo(x + size * 0.96, y + size * 0.92);
            path.lineTo(x + size * 0.04, y + size * 0.92);
            path.closePath();
            return path;
        });
    }

    /** 圆形 */
    public static PuzzleShape circle() {
        return named("circle", "圆形", (x, y, size) ->
                new Path2D.Double(new Ellipse2D.Double(x, y, size, size)));
    }

    /** 菱形 */
    public static PuzzleShape diamond() {
        return named("diamond", "菱形", (x, y, size) -> {
            Path2D path = new Path2D.Double();
            path.moveTo(x + size * 0.5, y + size * 0.04);
            path.lineTo(x + size * 0.96, y + size * 0.5);
            path.lineTo(x + size * 0.5, y + size * 0.96);
            path.lineTo(x + size * 0.04, y + size * 0.5);
            path.closePath();
            return path;
        });
    }

    /** 五角星 */
    public static PuzzleShape star() {
        return named("star", "星星", (x, y, size) -> {
            double cx = x + size * 0.5;
            double cy = y + size * 0.5;
            double outer = size * 0.5;
            double inner = size * 0.22;
            Path2D path = new Path2D.Double();
            for (int i = 0; i < 10; i++) {
                double radius = i % 2 == 0 ? outer : inner;
                double angle = -Math.PI / 2 + i * Math.PI / 5;
                double px = cx + Math.cos(angle) * radius;
                double py = cy + Math.sin(angle) * radius;
                if (i == 0) {
                    path.moveTo(px, py);
                } else {
                    path.lineTo(px, py);
                }
            }
            path.closePath();
            return path;
        });
    }

    /** 爱心（参数方程生成后居中缩放） */
    public static PuzzleShape heart() {
        return named("heart", "爱心", (x, y, size) -> {
            Path2D path = new Path2D.Double();
            // 经典参数方程爱心：
            // x = 16 * sin^3(t)
            // y = 13*cos(t) - 5*cos(2t) - 2*cos(3t) - cos(4t)
            // 曲线横向 32 个单位，等比缩放到 size，再按实际边界居中
            double scale = size / 32.0;
            int steps = 256;
            for (int i = 0; i <= steps; i++) {
                double t = i * 2 * Math.PI / steps;
                double sinT = Math.sin(t);
                double px = 16 * sinT * sinT * sinT * scale;
                // 屏幕坐标系 y 向下，参数方程的数学 y 需取反
                double py = -(13 * Math.cos(t) - 5 * Math.cos(2 * t)
                        - 2 * Math.cos(3 * t) - Math.cos(4 * t)) * scale;
                if (i == 0) {
                    path.moveTo(px, py);
                } else {
                    path.lineTo(px, py);
                }
            }
            path.closePath();
            // 等比缩放并居中到 (x, y, size, size)
            fitToBox(path, x, y, size);
            return path;
        });
    }

    /** 月亮（外圆减内圆后旋转） */
    public static PuzzleShape moon() {
        return named("moon", "月亮", (x, y, size) -> {
            double cx = x + size * 0.5;
            double cy = y + size * 0.5;
            double r = size * 0.5;
            // 外圆减内圆得到月牙：内圆向左上偏移 0.7r/1.4r，半径 0.9r
            Ellipse2D outer = new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r);
            Ellipse2D inner = new Ellipse2D.Double(cx - r * 0.7, cy - r * 1.4,
                    2 * r * 0.9, 2 * r * 0.9);
            Area moon = new Area(outer);
            moon.subtract(new Area(inner));
            // 顺时针倾斜 30°，更接近 🌙 的姿势
            moon.transform(AffineTransform.getRotateInstance(Math.toRadians(30), cx, cy));

            Path2D path = new Path2D.Double();
            path.append(moon.getPathIterator(null), false);
            // 等比缩放并居中到 (x, y, size, size)
            fitToBox(path, x, y, size);
            return path;
        });
    }

    /** 六边形 */
    public static PuzzleShape hexagon() {
        return named("hexagon", "六边形", (x, y, size) -> {
            Path2D path = new Path2D.Double();
            double cx = x + size * 0.5;
            double cy = y + size * 0.5;
            double r = size * 0.5;
            for (int i = 0; i < 6; i++) {
                double angle = -Math.PI / 2 + i * Math.PI / 3;
                double px = cx + Math.cos(angle) * r;
                double py = cy + Math.sin(angle) * r;
                if (i == 0) {
                    path.moveTo(px, py);
                } else {
                    path.lineTo(px, py);
                }
            }
            path.closePath();
            return path;
        });
    }

    /** 返回全部内置形状 */
    public static List<PuzzleShape> all() {
        return List.of(classic(), leaf(), triangle(), circle(), diamond(), star(), heart(),
                moon(), hexagon());
    }

    /** 包装名称、标签与绘制工厂为一个不可变形状 */
    private static PuzzleShape named(String name, String label, ShapeFactory factory) {
        return new PuzzleShape() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getLabel() {
                return label;
            }

            @Override
            public Path2D create(double x, double y, double size) {
                return factory.create(x, y, size);
            }
        };
    }

    /**
     * puzzle_captcha 经典图形：上边内凹、右边外凸、左边内凹。
     */
    private static Path2D createClassic(double x, double y, double size) {
        double u = size / 3.0;
        Path2D path = new Path2D.Double();
        path.moveTo(x, y);
        path.lineTo(x + u, y);
        // 上边内凹半圆
        path.append(new Arc2D.Double(x + u, y - u / 2, u, u, 180, -180, Arc2D.OPEN), true);
        path.lineTo(x + size, y);
        path.lineTo(x + size, y + u);
        // 右边外凸半圆
        path.append(new Arc2D.Double(x + size - u / 2, y + u, u, u, 90, -180, Arc2D.OPEN), true);
        path.lineTo(x + size, y + size);
        path.lineTo(x, y + size);
        path.lineTo(x, y + 2 * u);
        // 左边内凹半圆
        path.append(new Arc2D.Double(x - u / 2, y + u, u, u, -90, 180, Arc2D.OPEN), true);
        path.lineTo(x, y);
        path.closePath();
        return path;
    }

    /**
     * 将路径等比缩放并居中到 (x, y, size, size) 方块内。
     */
    private static void fitToBox(Path2D path, double x, double y, double size) {
        Rectangle2D bounds = path.getBounds2D();
        double scale = Math.min(size / bounds.getWidth(), size / bounds.getHeight());
        path.transform(AffineTransform.getScaleInstance(scale, scale));
        Rectangle2D scaled = path.getBounds2D();
        double dx = x + (size - scaled.getWidth()) / 2.0 - scaled.getMinX();
        double dy = y + (size - scaled.getHeight()) / 2.0 - scaled.getMinY();
        path.transform(AffineTransform.getTranslateInstance(dx, dy));
    }

    @FunctionalInterface
    private interface ShapeFactory {

        /** 在 (x, y) 处绘制边长为 size 的形状路径 */
        Path2D create(double x, double y, double size);
    }
}
