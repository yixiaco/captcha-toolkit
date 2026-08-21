package com.captcha.toolkit.model;

import java.util.List;

/**
 * 刮刮乐验证码的类型特定化载荷。
 *
 * @param promptImage  提示词整图（透明背景，data URI）
 * @param targetCount  需要刮出的图形总数
 * @param debugX       调试：全部提示图形刚好出现的滑块位置（归一化 0~1）
 * @param debugTargets 调试：目标图案在 debugPatterns 中的下标
 * @param debugPatterns 调试：全部图案（形状 + 归一化中心坐标），仅 debug 模式返回
 */
public record ScratchChallengeData(
        String promptImage,
        Integer targetCount,
        Double debugX,
        List<Integer> debugTargets,
        List<ScratchDebugPattern> debugPatterns) {

    /**
     * 调试用图案信息。
     *
     * @param shape 图形名称
     * @param x     归一化中心横坐标
     * @param y     归一化中心纵坐标
     */
    public record ScratchDebugPattern(String shape, double x, double y) {
    }
}
