package com.captcha.toolkit.config;

import lombok.Data;

/**
 * 行为轨迹校验配置（滑块/点选/旋转共用）。
 *
 * <p>行为报文（td）字段与极验对齐：m=协议版本、w/h=视口尺寸、
 * s/e=起止时间戳、p=轨迹点（timeMs,x,y,type，坐标归一化 0~1）。
 * 下方阈值字段是默认画像（Web/未知客户端）；H5 与小程序使用各自的触摸画像。</p>
 */
@Data
public class BehaviorConfig {

    /** 是否开启行为轨迹校验；默认关闭，开启后前端必须提交 td */
    private boolean enabled = false;

    /** 当前支持的行为报文协议版本 */
    private int protocol = 1;

    /** 轨迹点数量下限，少于该值直接判定异常 */
    private int minPoints = 3;

    /** 行为总耗时下限（毫秒），拦截脚本瞬间提交；拖拽本身可能很快，故默认放宽到 100ms */
    private long minDurationMs = 100;

    /** 行为总耗时上限（毫秒），超过视为挂机/异常 */
    private long maxDurationMs = 60_000;

    /** 相邻采样点允许的最大归一化跳跃距离，超过视为瞬移 */
    private double maxJumpRatio = 0.5;

    /** 轨迹终点/点击点与提交答案的归一化容差 */
    private double pointTolerance = 0.05;

    /** 点选单次按下到松开的最短时长（毫秒） */
    private long minClickDurationMs = 30;

    /** 点选单次按下到松开的最长时长（毫秒） */
    private long maxClickDurationMs = 5_000;

    /** H5（手机浏览器/WebView）触摸端画像 */
    private ClientBehaviorConfig h5 = ClientBehaviorConfig.touchDefaults();

    /** 微信小程序触摸端画像 */
    private ClientBehaviorConfig miniProgram = ClientBehaviorConfig.touchDefaults();

    /**
     * 按客户端类型选择画像；未知或未指定时回退到默认（Web）画像。
     */
    public ClientBehaviorConfig profileFor(String clientType) {
        if ("h5".equals(clientType)) {
            return h5;
        }
        if ("mini_program".equals(clientType)) {
            return miniProgram;
        }
        return defaultProfile();
    }

    /** 由顶层阈值字段构建默认（Web）画像 */
    public ClientBehaviorConfig defaultProfile() {
        ClientBehaviorConfig profile = new ClientBehaviorConfig();
        profile.setMinPoints(minPoints);
        profile.setMinDurationMs(minDurationMs);
        profile.setMaxDurationMs(maxDurationMs);
        profile.setMaxJumpRatio(maxJumpRatio);
        profile.setPointTolerance(pointTolerance);
        profile.setMinClickDurationMs(minClickDurationMs);
        profile.setMaxClickDurationMs(maxClickDurationMs);
        return profile;
    }
}
