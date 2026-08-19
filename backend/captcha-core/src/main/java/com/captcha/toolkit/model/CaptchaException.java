package com.captcha.toolkit.model;

/**
 * 验证码引擎业务异常。
 */
public class CaptchaException extends RuntimeException {

    public CaptchaException(String message) {
        super(message);
    }

    public CaptchaException(String message, Throwable cause) {
        super(message, cause);
    }
}
