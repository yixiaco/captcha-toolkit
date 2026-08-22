package com.captcha.toolkit.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 滑动曲线验证码的类型特定化载荷。
 *
 * @param endpoints        曲线两端固定点（像素坐标，前端绘制摆动曲线用）
 * @param amplitude        曲线振幅（像素）
 * @param shape            归一化形状采样（首尾为 0，其余在 [-1, 1]）
 * @param debugSwing       调试：真凹槽对应的摆动答案（0~1，仅 debug 模式返回）
 * @param debugFakeTargets 调试：假凹槽坐标（仅 debug 模式返回）
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SlideCurveChallengeData(
        List<PointVo> endpoints,
        Double amplitude,
        List<Double> shape,
        Double debugSwing,
        List<PointVo> debugFakeTargets) {
}
