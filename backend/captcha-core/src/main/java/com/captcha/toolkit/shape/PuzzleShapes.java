package com.captcha.toolkit.shape;

import java.awt.geom.Arc2D;
import java.awt.geom.AffineTransform;
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

    private PuzzleShapes() {
    }

    public static PuzzleShape classic() {
        return named("classic", "经典", PuzzleShapes::createClassic);
    }

    public static PuzzleShape leaf() {
        return named("leaf", "叶子", (x, y, size) -> {
            Path2D path = new Path2D.Double();
            path.moveTo(x + size * 0.5, y + size * 0.02);
            path.curveTo(x + size * 0.98, y + size * 0.16, x + size * 0.98, y + size * 0.72,
                    x + size * 0.5, y + size * 0.98);
            path.curveTo(x + size * 0.02, y + size * 0.72, x + size * 0.02, y + size * 0.16,
                    x + size * 0.5, y + size * 0.02);
            path.closePath();
            return path;
        });
    }

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

    public static PuzzleShape circle() {
        return named("circle", "圆形", (x, y, size) ->
                new Path2D.Double(new Ellipse2D.Double(x, y, size, size)));
    }

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
            // 用实际边界平移，保证爱心完整且居中于 (x, y, size, size)
            Rectangle2D bounds = path.getBounds2D();
            double dx = x + (size - bounds.getWidth()) / 2.0 - bounds.getMinX();
            double dy = y + (size - bounds.getHeight()) / 2.0 - bounds.getMinY();
            path.transform(AffineTransform.getTranslateInstance(dx, dy));
            return path;
        });
    }

    public static PuzzleShape moon() {
        return named("moon", "月亮", (x, y, size) -> {
            Path2D path = new Path2D.Double();
            double cx = x + size * 0.5;
            double cy = y + size * 0.5;
            double r = size * 0.5;
            // 标准月牙：外缘整半圆，内缘浅凹，中段宽度约 0.5r，饱满接近 🌙
            path.moveTo(cx, cy - r);
            path.quadTo(cx - size, cy, cx, cy + r);
            path.quadTo(cx - size * 0.36, cy, cx, cy - r);
            path.closePath();
            return path;
        });
    }

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

    public static List<PuzzleShape> all() {
        return List.of(classic(), leaf(), triangle(), circle(), diamond(), star(), heart(),
                moon(), hexagon());
    }

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

    @FunctionalInterface
    private interface ShapeFactory {
        Path2D create(double x, double y, double size);
    }
}
