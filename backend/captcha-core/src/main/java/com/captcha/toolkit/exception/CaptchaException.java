package com.captcha.toolkit.exception;

/**
 * 验证码引擎业务异常。
 */
public class CaptchaException extends RuntimeException {

    /**
     * @param message 异常信息
     */
    public CaptchaException(String message) {
        super(message);
    }

    /**
     * @param message 异常信息
     * @param cause   根因
     */
    public CaptchaException(String message, Throwable cause) {
        super(message, cause);
    }
}
