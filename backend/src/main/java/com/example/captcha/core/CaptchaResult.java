package com.example.captcha.core;

/**
 * 验证结果
 */
public class CaptchaResult {

    private boolean success;
    private boolean done;
    private String message;

    public static CaptchaResult ok(String message, boolean done) {
        CaptchaResult result = new CaptchaResult();
        result.success = true;
        result.done = done;
        result.message = message;
        return result;
    }

    public static CaptchaResult fail(String message) {
        CaptchaResult result = new CaptchaResult();
        result.success = false;
        result.done = false;
        result.message = message;
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
