package com.captcha.toolkit.i18n;

/**
 * 面向用户的提示消息编码（多语言资源配置）。
 *
 * <p>所有编码在 {@code captcha-messages*.properties} 中维护，业务代码禁止硬编码
 * 用户提示文本；HTTP 层可结合 Accept-Language / {@code lang} 参数按请求本地化。</p>
 */
public final class CaptchaMessages {

    private CaptchaMessages() {
    }

    /** 验证通过 */
    public static final String VERIFY_OK = "verify.ok";

    /** 验证失败，请重试 */
    public static final String VERIFY_WRONG = "verify.wrong";

    /** 验证码已过期，请刷新重试 */
    public static final String VERIFY_EXPIRED = "verify.expired";

    /** 验证速度异常 */
    public static final String VERIFY_TOO_FAST = "verify.too-fast";

    /** 验证码类型不匹配 */
    public static final String VERIFY_TYPE_MISMATCH = "verify.type-mismatch";

    /** 缺少验证码 id */
    public static final String VERIFY_MISSING_ID = "verify.missing-id";

    /** 缺少票据 ticket */
    public static final String VERIFY_MISSING_TICKET = "verify.missing-ticket";

    /** 不支持的验证码类型 */
    public static final String VERIFY_UNSUPPORTED_TYPE = "verify.unsupported-type";

    /** 参数错误 */
    public static final String VERIFY_BAD_PARAM = "verify.bad-param";

    /** 票据无效或已过期 */
    public static final String TICKET_INVALID = "ticket.invalid";

    /** 票据有效 */
    public static final String TICKET_VALID = "ticket.valid";

    /** 请求过于频繁，请稍后再试 */
    public static final String RATE_LIMIT_EXCEEDED = "rate-limit.exceeded";

    /** 缺少滑块位移 xNorm */
    public static final String SLIDER_MISSING_X_NORM = "slider.missing-x-norm";

    /** 缺少旋转角度 angle */
    public static final String ROTATE_MISSING_ANGLE = "rotate.missing-angle";

    /** 点击错误，请重试 */
    public static final String CLICK_WRONG = "click.wrong";

    /** 缺少行为轨迹 td */
    public static final String BEHAVIOR_MISSING_TD = "behavior.missing-td";

    /** 行为轨迹格式错误 */
    public static final String BEHAVIOR_INVALID_FORMAT = "behavior.invalid-format";

    /** 行为轨迹协议版本不支持 */
    public static final String BEHAVIOR_PROTOCOL_UNSUPPORTED = "behavior.protocol-unsupported";

    /** 行为轨迹视口尺寸不合法 */
    public static final String BEHAVIOR_INVALID_VIEWPORT = "behavior.invalid-viewport";

    /** 行为轨迹时间戳不合法 */
    public static final String BEHAVIOR_INVALID_TIMESTAMP = "behavior.invalid-timestamp";

    /** 行为耗时过短 */
    public static final String BEHAVIOR_TOO_SHORT = "behavior.too-short";

    /** 行为耗时过长 */
    public static final String BEHAVIOR_TOO_LONG = "behavior.too-long";

    /** 行为轨迹点数不足 */
    public static final String BEHAVIOR_NOT_ENOUGH_POINTS = "behavior.not-enough-points";

    /** 行为轨迹起始时间不合法 */
    public static final String BEHAVIOR_INVALID_START_TIME = "behavior.invalid-start-time";

    /** 行为轨迹时间乱序 */
    public static final String BEHAVIOR_TIME_OUT_OF_ORDER = "behavior.time-out-of-order";

    /** 行为轨迹坐标越界 */
    public static final String BEHAVIOR_COORDINATE_OUT_OF_RANGE = "behavior.coordinate-out-of-range";

    /** 行为轨迹跳跃异常 */
    public static final String BEHAVIOR_JUMP_TOO_LARGE = "behavior.jump-too-large";

    /** 行为轨迹风险过高 */
    public static final String BEHAVIOR_RISK_TOO_HIGH = "behavior.risk-too-high";

    /** 滑块轨迹应以按下开始 */
    public static final String SLIDER_EXPECTED_START = "slider.expected-start";

    /** 滑块轨迹应以松开结束 */
    public static final String SLIDER_EXPECTED_RELEASE = "slider.expected-release";

    /** 滑块轨迹不允许出现点击事件 */
    public static final String SLIDER_CLICK_NOT_ALLOWED = "slider.click-not-allowed";

    /** 滑块缺少移动轨迹 */
    public static final String SLIDER_MISSING_MOVE = "slider.missing-move";

    /** 滑块终点与轨迹不一致 */
    public static final String SLIDER_END_MISMATCH = "slider.end-mismatch";

    /** 点选轨迹应以按下开始 */
    public static final String CLICK_EXPECTED_START = "click.expected-start";

    /** 点选轨迹应以松开结束 */
    public static final String CLICK_EXPECTED_RELEASE = "click.expected-release";

    /** 点选轨迹存在未松开的点击 */
    public static final String CLICK_UNRELEASED = "click.unreleased";

    /** 点选轨迹点击顺序异常 */
    public static final String CLICK_ORDER_INVALID = "click.order-invalid";

    /** 缺少点选坐标 */
    public static final String CLICK_MISSING_POINTS = "click.missing-points";

    /** 点选次数与轨迹不一致 */
    public static final String CLICK_COUNT_MISMATCH = "click.count-mismatch";

    /** 点击位置与轨迹不一致 */
    public static final String CLICK_POSITION_MISMATCH = "click.position-mismatch";

    /** 点击时长异常 */
    public static final String CLICK_DURATION_INVALID = "click.duration-invalid";

    /** 旋转轨迹应以按下开始 */
    public static final String ROTATE_EXPECTED_START = "rotate.expected-start";

    /** 旋转轨迹应以松开结束 */
    public static final String ROTATE_EXPECTED_RELEASE = "rotate.expected-release";

    /** 旋转轨迹不允许出现点击事件 */
    public static final String ROTATE_CLICK_NOT_ALLOWED = "rotate.click-not-allowed";

    /** 旋转缺少移动轨迹 */
    public static final String ROTATE_MISSING_MOVE = "rotate.missing-move";
}
