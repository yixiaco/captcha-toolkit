package io.github.yixiaco.config;

import lombok.Data;

/**
 * 图形点选验证码配置。
 *
 * <p>背景图内埋入多个内置图形（目标 + 干扰），用户按提示依次点击目标图形；
 * 图形颜色从背景采样并做低对比调整，让机器视觉识别困难。</p>
 */
@Data
public class ClickShapeConfig {

    /** 图片宽度 */
    private int width = 340;

    /** 图片高度 */
    private int height = 190;

    /** 目标图形数量 */
    private int targetCount = 3;

    /** 干扰图形数量 */
    private int distractorCount = 5;

    /** 点击坐标像素容差 */
    private double tolerance = 18;

    /** 最短验证耗时（毫秒） */
    private long minElapsedMs = 800;

    /** 会话有效期（秒） */
    private long expireSeconds = 300;

    /** 图形边长占图宽的比例上限（实际尺寸在 min~max 间随机） */
    private double patternSizeRatio = 0.13;

    /** 图形边长占图宽的比例下限 */
    private double patternSizeMinRatio = 0.06;

    /** 图形之间的最小中心间距（像素） */
    private int patternMinGap = 40;

    /** 图形最大旋转角度（度） */
    private double rotationMax = 20;

    /** 图形相对背景的明度差范围（越小越难识别） */
    private double lightnessDeltaMin = 0.04;

    /** 图形相对背景的明度差上限 */
    private double lightnessDeltaMax = 0.12;

    /** 图形相对背景的色相偏移上限（度） */
    private double hueShiftMax = 8;

    /** 图形透明度范围 */
    private double alphaMin = 0.78;

    /** 图形透明度上限 */
    private double alphaMax = 0.9;

    /** 图形上的白色透明层透明度（0~1，与滑块拼图凹槽一致） */
    private double holeWhiteAlpha = 0.5;

    /** 抗锯齿超采样倍数 */
    private int renderScale = 2;
}
