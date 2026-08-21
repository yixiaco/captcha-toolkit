package com.captcha.toolkit.behavior;

/**
 * 单个行为轨迹点：相对起点毫秒、归一化坐标（0~1）、事件类型。
 *
 * @param timeMs 相对轨迹起点的毫秒数
 * @param x      归一化横坐标（0~1）
 * @param y      归一化纵坐标（0~1）
 * @param type   事件类型
 */
public record BehaviorPoint(int timeMs, double x, double y, BehaviorEventType type) {
}
