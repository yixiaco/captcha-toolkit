package io.github.yixiaco.util;

import io.github.yixiaco.shape.ShapeGeometry;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SVG 路径解析器测试：基础命令、相对命令与椭圆弧。
 */
class SvgPathParserTest {

    @Test
    void parsesBasicCommands() {
        Path2D path = SvgPathParser.parse(
                "M0 0L10 0L10 10L0 10Z");
        Rectangle2D bounds = path.getBounds2D();
        assertEquals(0, bounds.getMinX(), 1e-9);
        assertEquals(0, bounds.getMinY(), 1e-9);
        assertEquals(10, bounds.getMaxX(), 1e-9);
        assertEquals(10, bounds.getMaxY(), 1e-9);
    }

    @Test
    void parsesRelativeAndSmoothCommands() {
        Path2D path = SvgPathParser.parse(
                "M100 100h100v100h-100z"
                        + "M0 0c10 10 20 10 30 0s20-10 30 0"
                        + "q5 10 15 0t15 0");
        assertTrue(path.getBounds2D().getWidth() > 0);
        assertTrue(path.getBounds2D().getHeight() > 0);
    }

    @Test
    void parsesEllipticalArc() {
        Path2D path = SvgPathParser.parse(
                "M0 0a50 50 0 1 1 100 0z");
        Rectangle2D bounds = path.getBounds2D();
        // 半径为 50 的圆弧端点跨度为 100，边界应在合理范围内
        assertTrue(bounds.getMinX() >= -1 && bounds.getMinY() >= -51
                && bounds.getMaxX() <= 101 && bounds.getMaxY() <= 51,
                "圆弧边界异常: " + bounds);
        assertTrue(bounds.getWidth() > 0 && bounds.getHeight() > 0,
                "圆弧不应退化: " + bounds);
    }

    @Test
    void parsesLeafReferencePath() {
        Path2D path = SvgPathParser.parse(LeafPath.REFERENCE);
        Rectangle2D bounds = path.getBounds2D();
        assertTrue(bounds.getMinX() >= 0 && bounds.getMinY() >= 0
                && bounds.getMaxX() <= 1024 && bounds.getMaxY() <= 1024,
                "树叶路径应落在 viewBox 内: " + bounds);
        assertTrue(bounds.getWidth() > 100 && bounds.getHeight() > 100,
                "树叶路径不应是空或退化路径: " + bounds);
    }

    @Test
    void parsesMoonReferencePath() {
        Path2D path = SvgPathParser.parse(MoonPath.REFERENCE);
        Rectangle2D bounds = path.getBounds2D();
        assertTrue(bounds.getMinX() >= 0 && bounds.getMinY() >= 0
                && bounds.getMaxX() <= 48 && bounds.getMaxY() <= 48,
                "月亮路径应落在 viewBox 内: " + bounds);
        assertTrue(bounds.getWidth() > 20 && bounds.getHeight() > 20,
                "月亮路径不应是空或退化路径: " + bounds);
    }

    @Test
    void rendersMultipleClosedSubpathsWithHole() {
        // 外方块 + 内方块：两个闭合子路径，even-odd 规则下内部应镂空
        Path2D path = SvgPathParser.parse(
                "M0 0H40V40H0Z M10 10H30V30H10Z");
        Rectangle2D bounds = path.getBounds2D();
        assertEquals(0, bounds.getMinX(), 1e-9);
        assertEquals(40, bounds.getMaxX(), 1e-9);

        BufferedImage image = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fill(path);
        g.dispose();

        // 外框区域有内容，中心内方块为透明镂空
        assertTrue(((image.getRGB(2, 20) >>> 24) & 0xFF) > 0, "外框应被填充");
        assertEquals(0, (image.getRGB(20, 20) >>> 24) & 0xFF, "内方块应镂空");
    }

    @Test
    void outermostContourIgnoresInnerDetails() {
        String circleHouseSvg =
                "<svg viewBox=\"0 0 48 48\">"
                        + "<path d=\"M44 23H4C4 23 14.5 17 19 12C23.5 7 24.5 4 24.5 4"
                        + "C24.5 4 25.5 7 30 12C34.5 17 44 23 44 23Z\"/>"
                        + "<rect x=\"8\" y=\"31\" width=\"32\" height=\"13\"/>"
                        + "<rect x=\"13\" y=\"23\" width=\"22\" height=\"8\"/>"
                        + "</svg>";
        Path2D contour = SvgPathParser.outermostContour(circleHouseSvg);
        Rectangle2D bounds = contour.getBounds2D();
        // 最外围轮廓是外拱：x 4~44、y 4~23
        assertTrue(bounds.getMinX() >= 3.99 && bounds.getMinY() >= 3.99
                && bounds.getMaxX() <= 44.01 && bounds.getMaxY() <= 23.01,
                "应取最外围闭合轮廓: " + bounds);
        // 包围盒面积应大于内部门窗矩形，确保没有选中内部细节
        assertTrue(bounds.getWidth() * bounds.getHeight() > 416,
                "最外围轮廓包围盒应大于内部矩形: " + bounds);
    }

    @Test
    void outermostContourRejectsOpenOnlySvg() {
        try {
            SvgPathParser.outermostContour("<path d=\"M4 4L40 4\"/>");
            throw new AssertionError("应抛出 IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // 预期行为
        }
    }

    @Test
    void parseGeometryKeepsFillStrokeAndOpenPaths() {
        String svg = "<svg viewBox=\"0 0 48 48\">"
                + "<path d=\"M5 5L43 5L43 43L5 43Z\""
                + " fill=\"none\" stroke=\"#333\" stroke-width=\"4\"/>"
                + "<path d=\"M24 24C24 18 28 10 33 8\""
                + " stroke=\"#333\" stroke-width=\"4\"/>"
                + "<rect x=\"10\" y=\"10\" width=\"28\" height=\"28\"/>"
                + "</svg>";

        ShapeGeometry geometry = SvgPathParser.parseGeometry(svg);
        assertEquals(3, geometry.parts().size());

        // 显式 fill="none" + stroke：只描边不填充
        assertTrue(geometry.parts().get(0).stroked());
        assertTrue(!geometry.parts().get(0).filled());
        assertEquals(4, geometry.parts().get(0).strokeWidth(), 1e-9);

        // 无 Z 的路径：非闭合线条，同样是纯描边
        assertTrue(geometry.parts().get(1).stroked());
        assertTrue(!geometry.parts().get(1).filled());

        // 无 fill/stroke 属性的元素按 SVG 规范默认填充
        assertTrue(geometry.parts().get(2).filled());
        assertTrue(!geometry.parts().get(2).stroked());

        // 边界应包含描边宽度的一半外扩（最左 5 - 2 = 3）
        Rectangle2D bounds = geometry.bounds();
        assertTrue(bounds.getMinX() >= 2.99 && bounds.getMinY() >= 2.99
                        && bounds.getMaxX() <= 45.01 && bounds.getMaxY() <= 45.01,
                "几何边界应包含描边外扩: " + bounds);
    }

    @Test
    void parseGeometrySupportsLineAndPolyline() {
        String svg = "<svg>"
                + "<line x1=\"2\" y1=\"2\" x2=\"46\" y2=\"2\""
                + " stroke=\"#333\" stroke-width=\"3\"/>"
                + "<polyline points=\"4,4 20,20 40,10\""
                + " stroke=\"#333\" stroke-width=\"2\" fill=\"none\"/>"
                + "</svg>";

        ShapeGeometry geometry = SvgPathParser.parseGeometry(svg);
        assertEquals(2, geometry.parts().size());
        assertTrue(geometry.parts().get(0).stroked());
        assertEquals(3, geometry.parts().get(0).strokeWidth(), 1e-9);
        assertTrue(geometry.parts().get(1).stroked());
        assertEquals(2, geometry.parts().get(1).strokeWidth(), 1e-9);
    }

    @Test
    void parseGeometryParsesButterflyReference() {
        ShapeGeometry geometry = SvgPathParser.parseGeometry(ButterflySvg.REFERENCE);
        assertEquals(3, geometry.parts().size());
        for (var part : geometry.parts()) {
            assertTrue(part.stroked(), "蝴蝶各单元应为描边");
            assertTrue(!part.filled(), "蝴蝶各单元不应填充");
            assertEquals(4, part.strokeWidth(), 1e-9);
        }
        Rectangle2D bounds = geometry.bounds();
        assertTrue(bounds.getMinX() >= 0 && bounds.getMinY() >= 0
                        && bounds.getMaxX() <= 48.5 && bounds.getMaxY() <= 48.5,
                "蝴蝶边界应落在 viewBox 内并包含描边外扩: " + bounds);
    }


    /** 与 Leaf.svg 一致的路径数据（供解析回归测试复用） */
    static final class LeafPath {
        static final String REFERENCE =
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
                        + "z";

        private LeafPath() {
        }
    }

    /** 与 月亮_moon.svg 一致的路径数据 */
    static final class MoonPath {
        static final String REFERENCE =
                "M28.0527 4.41085 "
                        + "C22.5828 5.83695 18.5455 10.8106 18.5455 16.7273 "
                        + "C18.5455 23.7564 24.2436 29.4545 31.2727 29.4545 "
                        + "C37.1894 29.4545 42.1631 25.4172 43.5891 19.9473 "
                        + "C43.8585 21.256 44 22.6115 44 24 "
                        + "C44 35.0457 35.0457 44 24 44 "
                        + "C12.9543 44 4 35.0457 4 24 "
                        + "C4 12.9543 12.9543 4 24 4 "
                        + "C25.3885 4 26.744 4.14149 28.0527 4.41085 "
                        + "z";

        private MoonPath() {
        }
    }

    /** 与 蝴蝶_butterfly.svg 一致的文档数据（供解析回归测试复用） */
    static final class ButterflySvg {
        static final String REFERENCE =
                "<path d=\"M5.0002 12.0003C8.66389 7.70613 19.0814 18.8191 24.0002 25"
                        + "C28.9191 18.8191 39.3366 7.70599 43.0002 12.0001"
                        + "C43.6787 12.6507 44.4427 14.877 42.0002 18"
                        + "C41.3217 18.9759 40.186 21.7059 41.0002 26"
                        + "C41.0002 27.1386 39.8852 28.9519 35.0002 27"
                        + "C37.3749 28.6266 41.8498 33.0962 39.0002 37"
                        + "C36.2864 40.4158 29.9649 44.4569 26.0002 35"
                        + "L24.0002 31L22.0002 35"
                        + "C18.0356 44.4569 11.7141 40.4158 9.00024 37"
                        + "C6.15071 33.0962 10.6255 28.6268 13.0001 27.0003"
                        + "C8.1152 28.9521 7.00011 27.1389 7.00011 26.0003"
                        + "C7.81438 21.7061 6.67867 18.9762 6.00012 18.0003"
                        + "C3.55766 14.8773 4.32174 12.6509 5.0002 12.0003Z\""
                        + " fill=\"none\" stroke=\"#333\" stroke-width=\"4\""
                        + " stroke-linecap=\"round\" stroke-linejoin=\"round\"/>"
                        + "<path d=\"M24.0322 23C23.534 17.8642 28.9135 7 33 7\""
                        + " stroke=\"#333\" stroke-width=\"4\""
                        + " stroke-linecap=\"round\" stroke-linejoin=\"round\"/>"
                        + "<path d=\"M23.9678 23C24.466 17.8642 19.0865 7 15 7\""
                        + " stroke=\"#333\" stroke-width=\"4\""
                        + " stroke-linecap=\"round\" stroke-linejoin=\"round\"/>";

        private ButterflySvg() {
        }
    }
}
