package com.captcha.toolkit.exception;

/**
 * 设备维度请求频率超限异常：下发验证码阶段被限流时抛出，
 * 由 HTTP 层转换为 RATE_LIMITED 响应。
 */
public class RateLimitExceededException extends CaptchaException {

    /** 构造限流异常 */
    public RateLimitExceededException() {
        super("rate-limit.exceeded");
    }
}
