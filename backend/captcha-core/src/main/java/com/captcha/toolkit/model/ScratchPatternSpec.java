package com.captcha.toolkit.model;

/**
 * 刮刮乐单个图案的服务端布局规格（保存在会话里，不下发给前端）。
 *
 * @param shape    图形名称（与 {@link com.captcha.toolkit.shape.PuzzleShapes} 一致）
 * @param x        图案中心归一化横坐标（0~1）
 * @param y        图案中心归一化纵坐标（0~1）
 * @param size     图案边长（归一化 0~1，相对图片宽度）
 * @param rotation 图案旋转角度（度）
 */
public record ScratchPatternSpec(
        String shape,
        double x,
        double y,
        double size,
        double rotation) {
}
