package com.captcha.toolkit;

/**
 * 验证码类型。
 *
 * <p>类型是工厂模式的“产品线”标识：新增一种验证码时，
 * 只需要新增枚举值、对应工厂与生成器，无需改动引擎。</p>
 */
public enum CaptchaType {

    SLIDER("slider"),
    CLICK("click");

    private final String code;

    CaptchaType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static CaptchaType fromCode(String code) {
        for (CaptchaType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("不支持的验证码类型: " + code);
    }
}
