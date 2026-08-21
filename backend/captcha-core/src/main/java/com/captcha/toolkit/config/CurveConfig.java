package com.captcha.toolkit.config;

import lombok.Data;

/**
 * 曲线绘制验证码配置：用户沿图片中的引导曲线绘制路径，
 * 后端校验绘制覆盖率与起终点是否与期望曲线一致。
 */
@Data
public class CurveConfig {

    /** 图片宽度 */
    private int width = 340;

    /** 图片高度 */
    private int height = 190;

    /** 绘制容差（服务端像素），判定绘制点是否贴近期望曲线 */
    private double tolerance = 12;

    /** 最短验证耗时（毫秒），拦截脚本秒答 */
    private long minElapsedMs = 800;

    /** 会话有效期（秒） */
    private long expireSeconds = 300;

    /** 引导曲线的控制点数量（越多曲线越曲折） */
    private int controlPointCount = 5;

    /** 期望曲线采样点数量，用于覆盖率计算 */
    private int pointCount = 48;

    /** 最小覆盖率（0~1）：绘制点覆盖期望曲线的比例下限 */
    private double minCoverage = 0.6;

    /** 绘制答案最少点数，少于该值直接判定失败 */
    private int minDrawnPoints = 5;
}
