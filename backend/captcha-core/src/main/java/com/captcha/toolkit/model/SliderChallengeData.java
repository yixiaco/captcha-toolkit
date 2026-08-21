package com.captcha.toolkit.model;

import java.util.List;

/**
 * 滑块拼图验证码的类型特定化载荷。
 *
 * @param shape            拼图形状名
 * @param pieceOffsetX     拼图块内部左侧留白
 * @param debugX           调试：滑块答案 x（仅 debug 模式返回）
 * @param debugFakeTargets 调试：假目标坐标（仅 debug 模式返回）
 */
public record SliderChallengeData(
        String shape,
        Integer pieceOffsetX,
        Integer debugX,
        List<PointVo> debugFakeTargets) {
}
