package com.captcha.toolkit.behavior;

import java.util.List;

/**
 * 一次验证交互的完整行为轨迹，字段与极验 td 解包后的结构对齐：
 * <pre>
 * m = 协议版本
 * w / h = 前端视口（容器）尺寸
 * s / e = 起始 / 结束时间戳（毫秒）
 * p = 轨迹点列表
 * </pre>
 *
 * @param protocol       协议版本
 * @param viewportWidth  前端视口（容器）宽度
 * @param viewportHeight 前端视口（容器）高度
 * @param startTime      起始时间戳（毫秒）
 * @param endTime        结束时间戳（毫秒）
 * @param points         轨迹点列表
 */
public record BehaviorTrace(
        int protocol,
        double viewportWidth,
        double viewportHeight,
        long startTime,
        long endTime,
        List<BehaviorPoint> points) {

    public BehaviorTrace {
        points = List.copyOf(points);
    }

    /** 行为总耗时（毫秒） */
    public long durationMillis() {
        return endTime - startTime;
    }
}
