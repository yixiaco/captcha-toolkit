package com.captcha.toolkit.util;

import org.junit.jupiter.api.Test;

import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

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
}
