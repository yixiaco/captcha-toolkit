package com.captcha.toolkit.shape;

import com.captcha.toolkit.render.ShapeRenderer;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SVG 形状资源库测试：资源加载、命名解析与内置形状覆盖。
 */
class SvgShapeLibraryTest {

    @Test
    void loadsResourceShapesWithChineseLabels() {
        List<PuzzleShape> shapes = SvgShapeLibrary.load();
        assertFalse(shapes.isEmpty(), "资源库不应为空");
        assertTrue(shapes.size() >= 100, "资源库应包含全部新增图标: " + shapes.size());

        Map<String, PuzzleShape> byName = shapes.stream()
                .collect(Collectors.toMap(PuzzleShape::getName, Function.identity()));
        assertEquals("风车", byName.get("windmill-two").getLabel());
        assertEquals("太阳1", byName.get("sun-one").getLabel());
        assertEquals("爆米花", byName.get("popcorn-one").getLabel());
        assertEquals("出租车", byName.get("taxi").getLabel());
        assertEquals("菱形3", byName.get("diamond-three").getLabel());
        assertEquals("床头柜", byName.get("bedside").getLabel());
    }

    @Test
    void resourceShapesOverrideBuiltinsInRegistry() {
        PuzzleShapeRegistry registry = new PuzzleShapeRegistry();

        // 同名资源覆盖内置实现：资源版主体为填充 + 描边
        for (String name : List.of("airplane", "fire", "triangle")) {
            ShapeGeometry geometry = registry.resolve(name).geometry(0, 0, 100);
            boolean stroked = geometry.parts().stream()
                    .anyMatch(ShapePart::stroked);
            assertTrue(stroked, name + " 应被资源版（描边）覆盖");
        }

        // 新增形状已注册
        assertTrue(registry.contains("sun-one"));
        assertTrue(registry.contains("popcorn-one"));
        assertTrue(registry.contains("sailboat-one"));
        assertTrue(registry.contains("shower-head"));
        assertTrue(registry.contains("round"));
    }

    @Test
    void parseGeometryIgnoresDefsAndClipPathDefinitions() {
        String svg = "<svg>"
                + "<defs><clipPath id=\"c\">"
                + "<rect width=\"48\" height=\"48\" fill=\"#FFF\"/>"
                + "</clipPath></defs>"
                + "<path d=\"M4 4L44 4L44 44L4 44Z\" fill=\"#333\"/>"
                + "</svg>";

        ShapeGeometry geometry = com.captcha.toolkit.util.SvgPathParser.parseGeometry(svg);
        assertEquals(1, geometry.parts().size());
        Rectangle2D bounds = geometry.bounds();
        assertTrue(bounds.getWidth() < 48 && bounds.getHeight() < 48,
                "defs 内的裁剪矩形不应作为可见图形: " + bounds);
    }

    @Test
    void parseGeometryAppliesElementTransformAndRoundedRect() {
        String svg = "<rect x=\"0\" y=\"0\" width=\"10\" height=\"20\" rx=\"2\""
                + " transform=\"translate(20 10)\" fill=\"#333\"/>";

        ShapeGeometry geometry = com.captcha.toolkit.util.SvgPathParser.parseGeometry(svg);
        Rectangle2D bounds = geometry.bounds();
        assertEquals(20, bounds.getMinX(), 1e-9);
        assertEquals(10, bounds.getMinY(), 1e-9);
        assertEquals(30, bounds.getMaxX(), 1e-9);
        assertEquals(30, bounds.getMaxY(), 1e-9);
    }

    @Test
    void previouslyBrokenShapesDoNotRenderFullCanvas() {
        PuzzleShapeRegistry registry = new PuzzleShapeRegistry();
        for (String name : List.of("chicken-leg", "banana", "tickets-one", "chili",
                "take-off-one", "vicia-faba", "scallion", "pumpkin")) {
            ShapeGeometry geometry = registry.resolve(name).geometry(0, 0, 100);
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
            assertTrue(pixels < 9000,
                    name + " 不应渲染成全画布矩形，实际像素=" + pixels);
        }
    }
}
