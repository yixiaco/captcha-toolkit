package com.captcha.toolkit.behavior;

/**
 * 行为轨迹事件类型（与极验 td.p 的事件编码对齐）：
 * <pre>
 * 0 = START  轨迹起点（按下拖拽 / 点选首个事件）
 * 1 = MOVE   指针移动
 * 2 = UP     松开（拖拽结束 / 点选结束）
 * 3 = DOWN   按下（点选点击）
 * </pre>
 */
public enum BehaviorEventType {
    /** 轨迹起点：按下拖拽开始 / 点选首个事件 */
    START(0),

    /** 指针移动 */
    MOVE(1),

    /** 松开：拖拽结束 / 点选结束 */
    UP(2),

    /** 按下：点选点击 */
    DOWN(3);

    /** 报文中的数字编码 */
    private final int code;

    /**
     * @param code 报文中的数字编码
     */
    BehaviorEventType(int code) {
        this.code = code;
    }

    /** 返回报文中的数字编码 */
    public int code() {
        return code;
    }

    /**
     * 按数字编码解析事件类型；未知编码抛出 {@link IllegalArgumentException}。
     *
     * @param code 报文中的数字编码
     * @return 对应的事件类型
     */
    public static BehaviorEventType fromCode(int code) {
        for (BehaviorEventType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知行为事件类型: " + code);
    }
}
