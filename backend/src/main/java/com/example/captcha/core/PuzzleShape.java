package com.example.captcha.core;

import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

/**
 * 拼图块形状：经典形状严格参考 puzzle_captcha（上凹 + 右凸 + 左凹），
 * 其余形状为叶子/三角/圆形/菱形/星星/爱心。
 */
public final class PuzzleShape {

    public static final String[] NAMES = {
            "classic", "leaf", "triangle", "circle", "diamond", "star", "heart"
    };

    private PuzzleShape() {
    }

    public static Path2D create(String shape, double x, double y, double size) {
        String name = shape == null ? "classic" : shape;
        switch (name) {
            case "leaf":
                return leaf(x, y, size);
            case "triangle":
                return triangle(x, y, size);
            case "circle":
                return circle(x, y, size);
            case "diamond":
                return diamond(x, y, size);
            case "star":
                return star(x, y, size);
            case "heart":
                return heart(x, y, size);
            default:
                return classic(x, y, size);
        }
    }

    /**
     * puzzle_captcha 经典图形：3x3 方块，上边内凹、右边外凸、左边内凹。
     */
    private static Path2D classic(double x, double y, double size) {
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

    private static Path2D leaf(double x, double y, double size) {
        Path2D path = new Path2D.Double();
        path.moveTo(x + size * 0.5, y + size * 0.02);
        path.curveTo(x + size * 0.98, y + size * 0.16, x + size * 0.98, y + size * 0.72,
                x + size * 0.5, y + size * 0.98);
        path.curveTo(x + size * 0.02, y + size * 0.72, x + size * 0.02, y + size * 0.16,
                x + size * 0.5, y + size * 0.02);
        path.closePath();
        return path;
    }

    private static Path2D triangle(double x, double y, double size) {
        Path2D path = new Path2D.Double();
        path.moveTo(x + size * 0.5, y + size * 0.04);
        path.lineTo(x + size * 0.96, y + size * 0.92);
        path.lineTo(x + size * 0.04, y + size * 0.92);
        path.closePath();
        return path;
    }

    private static Path2D circle(double x, double y, double size) {
        return new Path2D.Double(new Ellipse2D.Double(x, y, size, size));
    }

    private static Path2D diamond(double x, double y, double size) {
        Path2D path = new Path2D.Double();
        path.moveTo(x + size * 0.5, y + size * 0.04);
        path.lineTo(x + size * 0.96, y + size * 0.5);
        path.lineTo(x + size * 0.5, y + size * 0.96);
        path.lineTo(x + size * 0.04, y + size * 0.5);
        path.closePath();
        return path;
    }

    private static Path2D star(double x, double y, double size) {
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
    }

    private static Path2D heart(double x, double y, double size) {
        Path2D path = new Path2D.Double();
        path.moveTo(x + size * 0.5, y + size * 0.92);
        path.curveTo(x + size * 0.06, y + size * 0.58, x + size * 0.04, y + size * 0.16,
                x + size * 0.3, y + size * 0.06);
        path.curveTo(x + size * 0.44, y + size * 0.01, x + size * 0.5, y + size * 0.14,
                x + size * 0.5, y + size * 0.24);
        path.curveTo(x + size * 0.5, y + size * 0.14, x + size * 0.56, y + size * 0.01,
                x + size * 0.7, y + size * 0.06);
        path.curveTo(x + size * 0.96, y + size * 0.16, x + size * 0.94, y + size * 0.58,
                x + size * 0.5, y + size * 0.92);
        path.closePath();
        return path;
    }
}
