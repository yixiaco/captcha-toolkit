package com.captcha.toolkit.shape;

import java.awt.geom.Arc2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * 内置拼图形状集合。
 *
 * <p>程序化形状（经典/三角/圆/菱形/星星/爱心/六边形）直接由代码生成；
 * 其余 SVG 图标形状统一由 {@link SvgShapeLibrary} 从 classpath 资源加载，
 * 本类只保留便捷入口方法，不再内嵌 SVG 数据。</p>
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
        return SvgShapeLibrary.shape("leaf");
    }

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

    /** 月亮（参考 月亮_moon.svg 的月牙轮廓） */
    public static PuzzleShape moon() {
        return SvgShapeLibrary.shape("moon");
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

    /** 蝙蝠（SVG 资源加载） */
    public static PuzzleShape bat() {
        return SvgShapeLibrary.shape("bat");
    }

    /** 大象（SVG 资源加载） */
    public static PuzzleShape elephant() {
        return SvgShapeLibrary.shape("elephant");
    }

    /** 海豚（SVG 资源加载） */
    public static PuzzleShape dolphin() {
        return SvgShapeLibrary.shape("dolphin");
    }

    /** 蝴蝶（SVG 资源加载） */
    public static PuzzleShape butterfly() {
        return SvgShapeLibrary.shape("butterfly");
    }

    /** 鲸鱼（SVG 资源加载） */
    public static PuzzleShape whale() {
        return SvgShapeLibrary.shape("whale");
    }

    /** 猫头鹰（SVG 资源加载） */
    public static PuzzleShape owl() {
        return SvgShapeLibrary.shape("owl");
    }

    /** 鸟（SVG 资源加载） */
    public static PuzzleShape bird() {
        return SvgShapeLibrary.shape("bird");
    }

    /** 青蛙（SVG 资源加载） */
    public static PuzzleShape frog() {
        return SvgShapeLibrary.shape("frog");
    }

    /** 熊（SVG 资源加载） */
    public static PuzzleShape bear() {
        return SvgShapeLibrary.shape("bear");
    }

    /** 鸭子（SVG 资源加载） */
    public static PuzzleShape duck() {
        return SvgShapeLibrary.shape("duck");
    }

    /** 鹰（SVG 资源加载） */
    public static PuzzleShape eagle() {
        return SvgShapeLibrary.shape("eagle");
    }

    /** 鱼（SVG 资源加载） */
    public static PuzzleShape fish() {
        return SvgShapeLibrary.shape("fish");
    }

    /** 猪（SVG 资源加载） */
    public static PuzzleShape pig() {
        return SvgShapeLibrary.shape("pig");
    }

    /** 飞机（SVG 资源加载） */
    public static PuzzleShape airplane() {
        return SvgShapeLibrary.shape("airplane");
    }

    /** 火热（SVG 资源加载） */
    public static PuzzleShape fire() {
        return SvgShapeLibrary.shape("fire");
    }

    /** 学校（SVG 资源加载） */
    public static PuzzleShape school() {
        return SvgShapeLibrary.shape("school");
    }

    /** 包装名称、标签与路径工厂为一个纯填充形状 */
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
            public ShapeGeometry geometry(double x, double y, double size) {
                return ShapeGeometry.filled(factory.create(x, y, size));
            }
        };
    }

    /** 返回全部内置形状（程序化形状 + SVG 资源形状，同名时资源覆盖） */
    public static List<PuzzleShape> all() {
        List<PuzzleShape> shapes = new ArrayList<>(List.of(
                classic(), triangle(), circle(), diamond(), star(), heart(), hexagon()));
        shapes.addAll(SvgShapeLibrary.load());
        return shapes;
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
