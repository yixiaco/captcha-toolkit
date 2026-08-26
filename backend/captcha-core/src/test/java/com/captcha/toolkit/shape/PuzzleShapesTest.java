package com.captcha.toolkit.shape;

import com.captcha.toolkit.render.ShapeRenderer;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        // 参考月亮_moon.svg 的月牙：面积约占方块一半，既不是满圆也不是细条
        double ratio = filled / 10000.0;
        assertTrue(ratio > 0.3 && ratio < 0.6,
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

    @Test
    void leafFollowsReferenceSvgProportions() {
        Path2D path = PuzzleShapes.leaf().create(0, 0, 100);
        Rectangle2D bounds = path.getBounds2D();

        assertTrue(bounds.getMinX() >= -0.01 && bounds.getMinY() >= -0.01
                        && bounds.getMaxX() <= 100.01 && bounds.getMaxY() <= 100.01,
                "树叶应完整位于方块内: " + bounds);

        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fill(path);
        g.dispose();

        long filled = 0;
        for (int y = 0; y < 100; y++) {
            for (int x = 0; x < 100; x++) {
                if (((image.getRGB(x, y) >>> 24) & 0xFF) > 0) {
                    filled++;
                }
            }
        }
        double ratio = filled / 10000.0;
        assertTrue(ratio > 0.1 && ratio < 0.7,
                "树叶面积占比应合理（非空也非整块），实际 ratio=" + ratio);
    }

    @Test
    void allShapesFitInBoxWithoutDegenerating() {
        for (PuzzleShape shape : PuzzleShapes.all()) {
            Path2D path = shape.create(0, 0, 100);
            Rectangle2D bounds = path.getBounds2D();
            if ("classic".equals(shape.getName())) {
                // classic 是带外凸圆弧的拼图块，允许少量越界
                assertTrue(bounds.getMinX() >= -20 && bounds.getMinY() >= -20
                                && bounds.getMaxX() <= 120 && bounds.getMaxY() <= 120,
                        shape.getName() + " 边界应在允许范围内: " + bounds);
            } else {
                assertTrue(bounds.getMinX() >= -0.01 && bounds.getMinY() >= -0.01
                                && bounds.getMaxX() <= 100.01 && bounds.getMaxY() <= 100.01,
                        shape.getName() + " 应完整位于方块内: " + bounds);
            }
            assertTrue(bounds.getWidth() > 0 && bounds.getHeight() > 0,
                    shape.getName() + " 不应是空或退化路径: " + bounds);
        }
    }

    @Test
    void butterflyIsStrokedMultiPathGeometry() {
        ShapeGeometry geometry = PuzzleShapes.butterfly().geometry(0, 0, 100);
        assertEquals(3, geometry.parts().size());
        for (ShapePart part : geometry.parts()) {
            assertTrue(part.stroked(), "蝴蝶各单元应为描边绘制");
            assertTrue(!part.filled(), "蝴蝶各单元不应填充");
            assertTrue(part.strokeWidth() > 0, "描边宽度应大于 0");
        }
        // 三条路径描边宽度一致（参考 SVG stroke-width=4）
        double firstWidth = geometry.parts().get(0).strokeWidth();
        assertEquals(firstWidth, geometry.parts().get(1).strokeWidth(), 1e-9);
        assertEquals(firstWidth, geometry.parts().get(2).strokeWidth(), 1e-9);

        // 适配后边界应完整落在方块内（包含描边外扩）
        Rectangle2D bounds = geometry.bounds();
        assertTrue(bounds.getMinX() >= -0.01 && bounds.getMinY() >= -0.01
                        && bounds.getMaxX() <= 100.01 && bounds.getMaxY() <= 100.01,
                "蝴蝶几何应完整位于方块内: " + bounds);
    }

    @Test
    void butterflyRendersVisibleThinStrokes() {
        ShapeGeometry geometry = PuzzleShapes.butterfly().geometry(0, 0, 100);
        BufferedImage image = new BufferedImage(
                100, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        ShapeRenderer.draw(g, geometry, Color.BLACK);
        g.dispose();

        long pixels = 0;
        for (int y = 0; y < 100; y++) {
            for (int x = 0; x < 100; x++) {
                if (((image.getRGB(x, y) >>> 24) & 0xFF) > 0) {
                    pixels++;
                }
            }
        }
        // 纯描边蝴蝶：应能画出可见内容；粗描边线稿面积不为 0，也不会占满整块
        assertTrue(pixels > 200, "蝴蝶描边应渲染出可见内容，实际像素=" + pixels);
        assertTrue(pixels < 8000, "蝴蝶描边不应接近整块填充，实际像素=" + pixels);
    }
}
