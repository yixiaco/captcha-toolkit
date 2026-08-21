package com.captcha.toolkit.config;

import lombok.Data;

/**
 * 刮刮乐验证码配置。
 *
 * <p>背景图内埋入多个与背景融合的图形，用户拖动滑块从左往右横扫揭开蒙版，
 * 提示图形全部出现后立即停止；图案颜色从背景采样并做低对比调整，
 * 使机器视觉识别困难。</p>
 */
@Data
public class ScratchConfig {

    /** 图片宽度 */
    private int width = 340;

    /** 图片高度 */
    private int height = 190;

    /** 最短验证耗时（毫秒） */
    private long minElapsedMs = 1000;

    /** 会话有效期（秒） */
    private long expireSeconds = 300;

    /** 图中埋入的图案总数 */
    private int patternCount = 6;

    /** 需要刮出的目标图形数量上限（实际数量在 targetCountMin~targetCount 间随机） */
    private int targetCount = 3;

    /** 需要刮出的目标图形数量下限 */
    private int targetCountMin = 1;

    /** 图案边长占图宽的比例上限（实际尺寸在 min~max 间随机） */
    private double patternSizeRatio = 0.13;

    /** 图案边长占图宽的比例下限 */
    private double patternSizeMinRatio = 0.06;

    /** 图案之间的最小中心间距（像素） */
    private int patternMinGap = 36;

    /** 滑块位置（归一化 0~1）校验容差 */
    private double tolerance = 0.03;

    /** 图案相对背景的明度差范围（越小越难识别） */
    private double lightnessDeltaMin = 0.04;

    /** 图案相对背景的明度差上限 */
    private double lightnessDeltaMax = 0.12;

    /** 图案相对背景的色相偏移上限（度） */
    private double hueShiftMax = 8;

    /** 图案透明度范围（与滑块拼图凹槽的 204/255 ≈ 0.8 保持一致） */
    private double alphaMin = 0.75;

    /** 图案透明度上限 */
    private double alphaMax = 0.85;

    /** 图案上的白色透明层透明度（与滑块拼图凹槽一致，0~1） */
    private double holeWhiteAlpha = 0.5;

    /** 抗锯齿超采样倍数 */
    private int renderScale = 2;
}
