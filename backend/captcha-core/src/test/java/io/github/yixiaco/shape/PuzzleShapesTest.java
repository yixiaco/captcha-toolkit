package io.github.yixiaco.shape;

import io.github.yixiaco.render.ShapeRenderer;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;

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
            // 渲染器实际消费的几何模型：非 classic 形状必须完整落在方块内
            Rectangle2D geometry = shape.geometry(0, 0, 100).bounds();
            // 兼容兜底路径：多单元 Area 合并会离散化曲线，误差幅度随 JDK 版本变化
            // （JDK 17 的离散化比 21 粗，最大偏差约 2%），因此只校验“没有明显跑偏”
            Rectangle2D fallback = shape.create(0, 0, 100).getBounds2D();
            if ("classic".equals(shape.getName())) {
                // classic 是带外凸圆弧的拼图块，允许少量越界
                assertTrue(withinBox(geometry, -20, 120),
                        shape.getName() + " 边界应在允许范围内: " + geometry);
                assertTrue(withinBox(fallback, -20, 120),
                        shape.getName() + " 兜底路径边界应在允许范围内: " + fallback);
            } else {
                assertTrue(withinBox(geometry, -0.01, 100.01),
                        shape.getName() + " 应完整位于方块内: " + geometry);
                assertTrue(withinBox(fallback, -2, 102),
                        shape.getName() + " 兜底路径不应明显超出方块: " + fallback);
            }
            assertTrue(geometry.getWidth() > 0 && geometry.getHeight() > 0,
                    shape.getName() + " 不应是空或退化路径: " + geometry);
            assertTrue(fallback.getWidth() > 0 && fallback.getHeight() > 0,
                    shape.getName() + " 兜底路径不应是空或退化路径: " + fallback);
        }
    }

    /** 判断矩形是否完整落在 [min, max] 包围盒内 */
    private static boolean withinBox(Rectangle2D bounds, double min, double max) {
        return bounds.getMinX() >= min && bounds.getMinY() >= min
                && bounds.getMaxX() <= max && bounds.getMaxY() <= max;
    }

    @Test
    void butterflyIsMixedGeometryWithStrokedAntennae() {
        ShapeGeometry geometry = PuzzleShapes.butterfly().geometry(0, 0, 100);
        assertEquals(3, geometry.parts().size());
        boolean filledBody = false;
        int strokedOnly = 0;
        for (ShapePart part : geometry.parts()) {
            if (part.filled() && part.stroked()) {
                filledBody = true;
            }
            if (part.stroked() && !part.filled()) {
                strokedOnly++;
            }
            assertTrue(part.strokeWidth() > 0, "描边宽度应大于 0");
        }
        assertTrue(filledBody, "蝴蝶主体应为填充 + 描边");
        assertEquals(2, strokedOnly, "两条触角应为纯描边非闭合线条");
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
    void butterflyRendersVisibleGeometry() {
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
        // 填充 + 描边蝴蝶：应能画出可见内容，也不会占满整块
        assertTrue(pixels > 200, "蝴蝶应渲染出可见内容，实际像素=" + pixels);
        assertTrue(pixels < 8000, "蝴蝶不应接近整块填充，实际像素=" + pixels);
    }

    @Test
    void svgLineArtShapesKeepFilledBodyWithWhiteDetails() {
        for (PuzzleShape shape : List.of(
                PuzzleShapes.eagle(), PuzzleShapes.frog(), PuzzleShapes.school())) {
            ShapeGeometry geometry = shape.geometry(0, 0, 100);
            boolean stroked = false;
            boolean filled = false;
            boolean filledOnly = false;
            boolean strokedOnly = false;
            for (ShapePart part : geometry.parts()) {
                stroked |= part.stroked();
                filled |= part.filled();
                filledOnly |= part.filled() && !part.stroked();
                strokedOnly |= part.stroked() && !part.filled();
            }
            assertTrue(stroked, shape.getName() + " 应有描边");
            assertTrue(filled, shape.getName() + " 应有填充主体");
            if ("school".equals(shape.getName())) {
                assertTrue(strokedOnly,
                        "学校应有白色描边细节（门线/屋顶线）");
            } else {
                assertTrue(filledOnly,
                        shape.getName() + " 应有白色填充细节（眼睛/斑点）");
            }

            Rectangle2D bounds = geometry.bounds();
            assertTrue(bounds.getMinX() >= -0.01 && bounds.getMinY() >= -0.01
                            && bounds.getMaxX() <= 100.01 && bounds.getMaxY() <= 100.01,
                    shape.getName() + " 应完整位于方块内: " + bounds);
        }
    }

    @Test
    void allRegisteredShapesFitInBox() {
        PuzzleShapeRegistry registry = new PuzzleShapeRegistry();
        for (String name : registry.names()) {
            ShapeGeometry geometry = registry.resolve(name).geometry(0, 0, 100);
            Rectangle2D bounds = geometry.bounds();
            if ("classic".equals(name)) {
                // classic 是带外凸圆弧的拼图块，允许少量越界
                assertTrue(bounds.getMinX() >= -20 && bounds.getMinY() >= -20
                                && bounds.getMaxX() <= 120 && bounds.getMaxY() <= 120,
                        name + " 边界应在允许范围内: " + bounds);
            } else {
                assertTrue(bounds.getMinX() >= -0.01 && bounds.getMinY() >= -0.01
                                && bounds.getMaxX() <= 100.01 && bounds.getMaxY() <= 100.01,
                        name + " 应完整位于方块内: " + bounds);
            }
        }
    }
}
