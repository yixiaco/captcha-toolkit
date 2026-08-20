package com.captcha.toolkit.shape;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 内置形状的几何回归测试。
 */
class PuzzleShapesTest {

    @Test
    void heartKeepsParametricProportions() {
        Path2D path = PuzzleShapes.heart().create(0, 0, 100);
        Rectangle2D bounds = path.getBounds2D();

        // 参数方程爱心宽 32、高约 28.9：等比缩放后应落在方块内且宽大于高
        assertTrue(bounds.getMinX() >= 0 && bounds.getMinY() >= 0,
                "爱心应完整位于方块内: " + bounds);
        assertTrue(bounds.getMaxX() <= 100 && bounds.getMaxY() <= 100,
                "爱心应完整位于方块内: " + bounds);
        assertTrue(bounds.getWidth() > bounds.getHeight(),
                "参数方程爱心应宽于高，实际 width=" + bounds.getWidth()
                        + ", height=" + bounds.getHeight());
        // 高宽比应接近参数方程的 28.9/32，允许少量误差
        double ratio = bounds.getHeight() / bounds.getWidth();
        assertTrue(ratio > 0.85 && ratio < 0.95,
                "爱心高宽比应接近 28.9/32，实际 ratio=" + ratio);
    }

    @Test
    void moonIsPlumpCrescent() {
        Path2D path = PuzzleShapes.moon().create(0, 0, 100);
        Rectangle2D bounds = path.getBounds2D();

        // 外圆减内圆后应完整落在方块内
        assertTrue(bounds.getMinX() >= -0.01 && bounds.getMinY() >= -0.01
                        && bounds.getMaxX() <= 100.01 && bounds.getMaxY() <= 100.01,
                "月亮应完整位于方块内: " + bounds);

        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fill(path);
        g.dispose();

        long filled = 0;
        int[] rowCount = new int[100];
        for (int y = 0; y < 100; y++) {
            for (int x = 0; x < 100; x++) {
                if (((image.getRGB(x, y) >>> 24) & 0xFF) > 0) {
                    filled++;
                    rowCount[y]++;
                }
            }
        }
        // 外圆减内圆的面积约 34% 方块：既不是半圆也不是细条
        double ratio = filled / 10000.0;
        assertTrue(ratio > 0.25 && ratio < 0.45,
                "月亮面积占比应在饱满月牙范围，实际 ratio=" + ratio);

        // 内凹缺口在顶部：最顶行宽度应明显小于最宽行
        int topFilledRow = 0;
        for (int row : rowCount) {
            if (row > 0) {
                topFilledRow = row;
                break;
            }
        }
        int maxRow = 0;
        for (int row : rowCount) {
            maxRow = Math.max(maxRow, row);
        }
        assertTrue(topFilledRow < maxRow * 0.9,
                "月亮顶部应有内凹缺口，top=" + topFilledRow + ", max=" + maxRow);
    }
}
