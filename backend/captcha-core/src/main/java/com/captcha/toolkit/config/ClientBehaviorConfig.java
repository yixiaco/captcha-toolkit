package com.captcha.toolkit.config;

import lombok.Data;

/**
 * 单个客户端类型的行为校验画像（阈值集合）。
 *
 * <p>触摸端（H5/小程序）轨迹采样更稀疏、点击更快，因此需要比鼠标端更宽松的
 * 跳跃、容差和点击时长阈值。</p>
 */
@Data
public class ClientBehaviorConfig {

    /** 轨迹点数量下限 */
    private int minPoints = 3;

    /** 行为总耗时下限（毫秒） */
    private long minDurationMs = 100;

    /** 行为总耗时上限（毫秒） */
    private long maxDurationMs = 60_000;

    /** 相邻采样点允许的最大归一化跳跃距离 */
    private double maxJumpRatio = 0.5;

    /** 轨迹终点/点击点与提交答案的归一化容差 */
    private double pointTolerance = 0.05;

    /** 点选单次按下到松开的最短时长（毫秒） */
    private long minClickDurationMs = 30;

    /** 点选单次按下到松开的最长时长（毫秒） */
    private long maxClickDurationMs = 5_000;

    /** 行为风险综合分数阈值（0~1）；触摸端数据噪声大，默认比 Web 宽松 */
    private double riskThreshold = 0.8;

    /**
     * 触摸端默认画像：容忍稀疏采样、手指抖动和更快的点击。
     *
     * @return 适合 H5 / 小程序触摸交互的阈值集合
     */
    public static ClientBehaviorConfig touchDefaults() {
        ClientBehaviorConfig config = new ClientBehaviorConfig();
        config.setMaxJumpRatio(0.9);
        config.setPointTolerance(0.08);
        config.setMinClickDurationMs(20);
        config.setRiskThreshold(0.8);
        return config;
    }
}
