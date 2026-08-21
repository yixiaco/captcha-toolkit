package com.captcha.toolkit.config;

import lombok.Data;

/**
 * 设备维度高频请求限流配置。
 *
 * <p>前端采集设备指纹并随请求提交，后端哈希后按“设备”维度做固定窗口计数，
 * 超过 {@code maxRequests} 时拒绝后续请求，防止脚本用同一设备反复刷验证码。</p>
 */
@Data
public class RateLimitConfig {

    /** 是否开启设备维度限流；默认关闭，避免影响未接入指纹的存量前端 */
    private boolean enabled = false;

    /** 每个时间窗口内同一设备允许的最大请求数 */
    private int maxRequests = 20;

    /** 时间窗口长度（秒） */
    private long windowSeconds = 60;

    /** 设备指纹脱敏盐：与原始指纹拼接后哈希存储，避免明文指纹落库 */
    private String fingerprintSalt = "";
}
