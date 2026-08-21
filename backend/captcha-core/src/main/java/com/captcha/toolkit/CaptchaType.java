package com.captcha.toolkit;

/**
 * 验证码类型。
 *
 * <p>类型是工厂模式的“产品线”标识：新增一种验证码时，
 * 只需要新增枚举值、对应工厂与生成器，无需改动引擎。</p>
 */
public enum CaptchaType {

    /** 滑块拼图 */
    SLIDER("slider"),

    /** 文字点选 */
    CLICK("click"),

    /** 图片旋转 */
    ROTATE("rotate"),

    /** 曲线绘制 */
    CURVE("curve");

    /** 对外使用的类型编码 */
    private final String code;

    /**
     * @param code 对外使用的类型编码
     */
    CaptchaType(String code) {
        this.code = code;
    }

    /** 返回对外使用的类型编码 */
    public String getCode() {
        return code;
    }

    /**
     * 按编码（忽略大小写）解析类型；不支持时抛出 {@link IllegalArgumentException}。
     *
     * @param code 类型编码，如 slider / click / rotate
     * @return 对应的验证码类型
     */
    public static CaptchaType fromCode(String code) {
        for (CaptchaType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("不支持的验证码类型: " + code);
    }
}
