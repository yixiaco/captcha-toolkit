package com.captcha.toolkit.model;

/**
 * 验证结果（面向调用方/前端的响应模型）。
 */
public class VerifyResult {

    private boolean success;
    private boolean done;
    private String message;
    /** 业务码：OK / WRONG / TOO_FAST / EXPIRED / BAD_REQUEST */
    private String code = "OK";
    /** 验证通过后发放的一次性票据，供登录等业务接口校验 */
    private String ticket;

    public static VerifyResult ok(String message) {
        VerifyResult result = new VerifyResult();
        result.success = true;
        result.done = true;
        result.message = message;
        result.code = "OK";
        return result;
    }

    public static VerifyResult fail(String message, String code) {
        VerifyResult result = new VerifyResult();
        result.success = false;
        result.done = false;
        result.message = message;
        result.code = code;
        return result;
    }

    public static VerifyResult expired(String message) {
        return fail(message, "EXPIRED");
    }

    public static VerifyResult tooFast(String message) {
        return fail(message, "TOO_FAST");
    }

    public static VerifyResult badRequest(String message) {
        return fail(message, "BAD_REQUEST");
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTicket() {
        return ticket;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }
}
