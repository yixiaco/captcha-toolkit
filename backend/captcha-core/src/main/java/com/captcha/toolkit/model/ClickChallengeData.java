package com.captcha.toolkit.model;

import java.util.List;

/**
 * 文字点选验证码的类型特定化载荷。
 *
 * @param prompt          提示文字（按点击顺序）
 * @param debugTargets    调试：目标坐标（仅 debug 模式返回）
 * @param debugFakeTargets 调试：假目标坐标（预留，仅 debug 模式返回）
 */
public record ClickChallengeData(
        List<String> prompt,
        List<PointVo> debugTargets,
        List<PointVo> debugFakeTargets) {
}
