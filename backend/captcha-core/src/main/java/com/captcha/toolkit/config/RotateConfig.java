package com.captcha.toolkit.config;

import lombok.Data;

/**
 * 图片旋转验证码配置。
 */
@Data
public class RotateConfig {

    private int width = 340;
    private int height = 190;
    /** 角度容差（度） */
    private double tolerance = 3;
    /** 最短验证耗时（毫秒） */
    private long minElapsedMs = 800;
    /** 会话有效期（秒） */
    private long expireSeconds = 300;
    /** 错位角度范围（度），避开接近 0/360 的“几乎对齐” */
    private double minAngle = 20;
    private double maxAngle = 340;
    /** 抗锯齿超采样倍数 */
    private int renderScale = 2;
}
