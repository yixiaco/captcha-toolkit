package io.github.yixiaco.shape;

import java.awt.geom.Path2D;

/**
 * 拼图块形状策略。
 *
 * <p>形状以 {@link ShapeGeometry} 为唯一权威模型，支持多条路径、
 * 非闭合线条与填充/描边组合；新增形状时实现 {@link #geometry} 并注册到
 * {@link PuzzleShapeRegistry}，各渲染器无需任何改动。</p>
 */
public interface PuzzleShape {

    /** 形状唯一名称（如 classic / leaf） */
    String getName();

    /** 形状展示名称（中文标签） */
    String getLabel();

    /**
     * 在 (x, y) 处生成边长为 size 的完整几何模型。
     *
     * @param x    左上角 x
     * @param y    左上角 y
     * @param size 边长
     * @return 已适配到目标方块的几何模型
     */
    ShapeGeometry geometry(double x, double y, double size);

    /**
     * 兼容旧接口：返回可填充的合并路径。
     *
     * <p>描边单元会先转成带状区域，多单元会取并集，曲线可能被离散化；
     * 正式渲染应使用 {@link #geometry}。</p>
     *
     * @param x    左上角 x
     * @param y    左上角 y
     * @param size 边长
     * @return 合并后的填充路径
     */
    default Path2D create(double x, double y, double size) {
        return geometry(x, y, size).toFilledPath();
    }
}
