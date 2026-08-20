package com.captcha.toolkit.shape;

import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
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
            // 宽版爱心：左右叶更宽，整体更接近方块比例
            path.moveTo(x + size * 0.5, y + size * 0.95);
            path.curveTo(x + size * 0.02, y + size * 0.6, x + size * 0.0, y + size * 0.12,
                    x + size * 0.28, y + size * 0.04);
            path.curveTo(x + size * 0.42, y + size * 0.0, x + size * 0.5, y + size * 0.16,
                    x + size * 0.5, y + size * 0.26);
            path.curveTo(x + size * 0.5, y + size * 0.16, x + size * 0.58, y + size * 0.0,
                    x + size * 0.72, y + size * 0.04);
            path.curveTo(x + size * 1.0, y + size * 0.12, x + size * 0.98, y + size * 0.6,
                    x + size * 0.5, y + size * 0.95);
            path.closePath();
            return path;
        });
    }

    public static PuzzleShape moon() {
        return named("moon", "月亮", (x, y, size) -> {
            Path2D path = new Path2D.Double();
            double cx = x + size * 0.5;
            double cy = y + size * 0.5;
            double r = size * 0.5;
            // 标准月牙：左凸缘 + 右凹缘，两端在上下顶点闭合
            path.moveTo(cx, cy - r);
            // 外缘接近整半圆（最左点到 cx - r），内缘按 0.618 比例内凹，
            // 中段月牙宽度约为 0.38r，接近 🌙 的比例
            path.quadTo(cx - size, cy, cx, cy + r);
            path.quadTo(cx - size * 0.618, cy, cx, cy - r);
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
