package io.github.yixiaco.render;

import io.github.yixiaco.shape.PuzzleShape;
import io.github.yixiaco.shape.PuzzleShapeRegistry;
import io.github.yixiaco.shape.ShapeGeometry;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * 提示词图片渲染器：把目标图形横向排成一行（透明背景）。
 *
 * <p>文字点选提示词由文字绘制，图形类验证码（刮刮乐、图形点选）共用本工具。</p>
 */
public final class ShapePromptRenderer {

    /** 单个图形边长（px） */
    private static final int SHAPE_SIZE = 44;

    /** 图形间距（px） */
    private static final int GAP = 10;

    /** 四周留白（px） */
    private static final int PADDING = 8;

    private ShapePromptRenderer() {
    }

    /**
     * 渲染提示词图片。
     *
     * @param registry 形状注册表
     * @param shapes   需要提示的图形名称（按提示顺序）
     * @return 透明背景的提示词图片
     */
    public static BufferedImage render(PuzzleShapeRegistry registry, List<String> shapes) {
        int width = PADDING * 2 + shapes.size() * SHAPE_SIZE
                + Math.max(0, shapes.size() - 1) * GAP;
        int height = PADDING * 2 + SHAPE_SIZE;

        BufferedImage image = new BufferedImage(
                width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        PatternPainter.enableAntialias(g);
        for (int i = 0; i < shapes.size(); i++) {
            PuzzleShape shape = registry.resolve(shapes.get(i));
            double x = PADDING + i * (SHAPE_SIZE + GAP);
            double y = PADDING;
            ShapeGeometry geometry = shape.geometry(x + 2, y + 2, SHAPE_SIZE - 4);
            PatternPainter.drawShapeWithInnerShadow(g, geometry,
                    new Color(9, 88, 217), 1, width, height, 0);
        }
        g.dispose();
        return image;
    }
}
