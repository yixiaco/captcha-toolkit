package com.captcha.toolkit.config;

import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 滑块验证码配置，CaptchaConfig 与 CaptchaProperties 共用。
 */
@Data
public class SliderConfig {

    private int width = 340;
    private int height = 190;
    /** 像素容差（服务端图片坐标系） */
    private double tolerance = 8;
    /** 最短验证耗时，拦截脚本秒答 */
    private long minElapsedMs = 500;
    /** 会话有效期（秒） */
    private long expireSeconds = 300;
    /** 拼图块边长占宽度比例（参考 puzzle_captcha：280 宽对 30） */
    private double pieceSizeRatio = 30.0 / 280.0;
    /** 拼图块允许出现的边界留白 */
    private int margin = 10;
    /** 抗锯齿超采样倍数 */
    private int renderScale = 2;
    /** 缺口白色蒙版 alpha（0-255） */
    private int holeAlpha = 204;
    private float shadowRadius = 6f;
    private float shadowOpacity = 0.8f;
    private float shadowOffsetX = 2f;
    private float shadowOffsetY = -1f;
    /** 默认形状；shape 参数非法或未启用时回退到它 */
    private String defaultShape = "classic";
    /** 对外可用的形状白名单 */
    private List<String> enabledShapes = new ArrayList<>(Arrays.asList(
            "classic", "leaf", "triangle", "circle", "diamond", "star", "heart",
            "moon", "hexagon"));
    /** 小图左右留白，用于容纳柔光投影 */
    private int piecePadding = 8;
    /** 假目标（干扰凹槽）数量：图中会出现多个缺口，但只有真目标能拼合 */
    private int fakeTargetCount = 0;
    /** 假目标中心最小间距（像素），用于避免图形重叠 */
    private int fakeTargetMinGap = 24;
    /** 判定“同一 y/x 轴”的像素阈值：小于该值视为同轴 */
    private int fakeTargetAxisThreshold = 12;
}
