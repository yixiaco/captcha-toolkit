package com.captcha.toolkit.shape;

import java.awt.geom.Path2D;

/**
 * 拼图块形状策略。
 *
 * <p>新增形状时实现本接口并注册到 {@link PuzzleShapeRegistry}，
 * 滑块生成器无需任何改动。</p>
 */
public interface PuzzleShape {

    String getName();

    String getLabel();

    /** 在 (x, y) 处生成边长为 size 的闭合路径 */
    Path2D create(double x, double y, double size);
}
