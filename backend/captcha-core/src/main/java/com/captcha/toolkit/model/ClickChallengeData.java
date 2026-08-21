package com.captcha.toolkit.model;

import java.util.List;

/**
 * 文字点选验证码的类型特定化载荷。
 *
 * @param promptImage     提示词整图（透明背景，data URI）
 * @param targetCount     需要点击的目标数量
 * @param debugTargets    调试：目标坐标（仅 debug 模式返回）
 * @param debugFakeTargets 调试：假目标坐标（预留，仅 debug 模式返回）
 */
public record ClickChallengeData(
        String promptImage,
        Integer targetCount,
        List<PointVo> debugTargets,
        List<PointVo> debugFakeTargets) {
}
