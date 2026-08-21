package com.captcha.toolkit.config;

import lombok.Data;

/**
 * 滑块摆动图块验证码配置：用户拖动滑块，让小图块沿多阶贝塞尔曲线运动到目标凹槽，
 * 移动过程中图块方向随路径摆动，最终对准目标；图中可配置多个假凹槽干扰。
 */
@Data
public class SwingTileConfig {

    /** 图片宽度 */
    private int width = 340;

    /** 图片高度 */
    private int height = 190;

    /** 校验容差（归一化滑块位置 0~1），判定图块是否到达终点 */
    private double tolerance = 0.03;

    /** 真凹槽在贝塞尔路径上的位置范围（归一化滑块位置，0~1） */
    private double answerMin = 0.35;

    /** 真凹槽在贝塞尔路径上的位置范围（归一化滑块位置，0~1） */
    private double answerMax = 0.8;

    /** 最短验证耗时（毫秒），拦截脚本秒答 */
    private long minElapsedMs = 800;

    /** 会话有效期（秒） */
    private long expireSeconds = 300;

    /** 假凹槽数量 */
    private int fakeTargetCount = 2;

    /** 假凹槽与真凹槽（以及彼此）之间的最小中心间距（像素） */
    private int fakeTargetMinGap = 56;

    /** 贝塞尔曲线控制点数量（1=二次、2=三次、3=四次，默认三次多阶曲线） */
    private int controlPointCount = 2;

    /** 图块尺寸占图片宽度的比例 */
    private double pieceSizeRatio = 0.12;

    /** 抗锯齿超采样倍数（与原滑块渲染保持一致） */
    private int renderScale = 2;

    /** 凹槽白色蒙版透明度（0~255） */
    private int holeAlpha = 204;

    /** 凹槽内阴影半径（像素） */
    private int shadowRadius = 6;

    /** 凹槽内阴影不透明度（0~1） */
    private float shadowOpacity = 0.5f;

    /** 图块裁剪留白（像素） */
    private int piecePadding = 8;

    /** 移动过程中方向的摆动幅度（度），路径中点最大，两端为 0 */
    private double rotationSwingAmplitude = 45;

    /** 起始方向相对目标方向的随机偏移范围（度） */
    private double startRotationMax = 60;

    /** 目标凹槽方向范围（度） */
    private double endRotationMin = -20;

    /** 目标凹槽方向范围（度） */
    private double endRotationMax = 20;
}
