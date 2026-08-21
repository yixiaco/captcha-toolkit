package com.captcha.toolkit.model;

import lombok.Data;

/**
 * 验证结果（面向调用方/前端的响应模型）。
 */
@Data
public class VerifyResult {

    /** 校验是否成功 */
    private boolean success;

    /** 校验流程是否已完成（成功或失败都算完成） */
    private boolean done;

    /** 面向用户的提示信息 */
    private String message;

    /** 业务码：OK / WRONG / TOO_FAST / EXPIRED / BAD_REQUEST */
    private String code = "OK";

    /** 验证通过后发放的一次性票据，供登录等业务接口校验 */
    private String ticket;

    /** 构造成功结果 */
    public static VerifyResult ok(String message) {
        VerifyResult result = new VerifyResult();
        result.success = true;
        result.done = true;
        result.message = message;
        result.code = "OK";
        return result;
    }

    /**
     * 构造失败结果。
     *
     * @param message 面向用户的提示
     * @param code    业务码（WRONG / TOO_FAST / EXPIRED / BAD_REQUEST / BEHAVIOR 等）
     */
    public static VerifyResult fail(String message, String code) {
        VerifyResult result = new VerifyResult();
        result.success = false;
        result.done = false;
        result.message = message;
        result.code = code;
        return result;
    }

    /** 构造已过期结果 */
    public static VerifyResult expired(String message) {
        return fail(message, "EXPIRED");
    }

    /** 构造验证过快结果 */
    public static VerifyResult tooFast(String message) {
        return fail(message, "TOO_FAST");
    }

    /** 构造参数错误结果 */
    public static VerifyResult badRequest(String message) {
        return fail(message, "BAD_REQUEST");
    }
}
