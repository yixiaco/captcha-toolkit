package com.captcha.toolkit.config;

import lombok.Data;

/**
 * 滑动曲线验证码配置：用户拖动滑块把曲线滑入对应的凹槽，
 * 图中可放置多个形状不同的假凹槽干扰，后端校验曲线摆动量。
 *
 * <p>曲线两端固定在大图上，滑块位置决定曲线的摆动幅度与方向：
 * 摆动量从 -1（向左/下弯）到 +1（向右/上弯），0 为两端之间的直线；
 * 只有摆动到真凹槽对应的摆动量时，曲线才与凹槽完全重合。</p>
 */
@Data
public class SlideCurveConfig {

    /** 图片宽度 */
    private int width = 340;

    /** 图片高度 */
    private int height = 190;

    /** 校验容差（归一化摆动量 0~1），判定曲线是否对准真凹槽 */
    private double tolerance = 0.035;

    /** 最短验证耗时（毫秒），拦截脚本秒答 */
    private long minElapsedMs = 800;

    /** 会话有效期（秒） */
    private long expireSeconds = 300;

    /** 假凹槽数量 */
    private int fakeTargetCount = 2;

    /** 曲线振幅范围（像素），用于生成不同波形的凹槽 */
    private double amplitudeMin = 28;

    /** 曲线振幅范围（像素），用于生成不同波形的凹槽 */
    private double amplitudeMax = 48;

    /** 摆动曲线采样点数量（含两端） */
    private int sampleCount = 24;

    /** 真凹槽摆动答案的最小值（0~1 滑块位置） */
    private double swingMin = 0.1;

    /** 真凹槽摆动答案的最大值（0~1 滑块位置） */
    private double swingMax = 0.8;

    /** 凹槽描边宽度（像素） */
    private int grooveStrokeWidth = 6;

    /** 摆动曲线描边宽度（像素） */
    private int curveStrokeWidth = 4;
}
