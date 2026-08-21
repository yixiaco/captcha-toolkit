package com.captcha.toolkit.config;

import lombok.Data;

/**
 * 角度验证（圆盘旋转）配置。
 *
 * <p>背景中心固定一个圆形转盘，转盘上带一个方向标记；
 * 用户拖动滑块旋转圆盘，直到标记对准顶部的固定凹口。</p>
 */
@Data
public class AngleConfig {

    /** 图片宽度 */
    private int width = 340;

    /** 图片高度 */
    private int height = 190;

    /** 角度容差（度） */
    private double tolerance = 3;

    /** 最短验证耗时（毫秒） */
    private long minElapsedMs = 800;

    /** 会话有效期（秒） */
    private long expireSeconds = 300;

    /** 标记初始错位角度范围（度），避开接近 0/360 的“几乎朝上” */
    private double minAngle = 20;

    /** 标记初始错位角度上限（度） */
    private double maxAngle = 340;

    /** 圆盘半径占画布短边的比例（越大圆盘越大） */
    private double discRadiusRatio = 0.40;

    /** 抗锯齿超采样倍数 */
    private int renderScale = 2;
}
