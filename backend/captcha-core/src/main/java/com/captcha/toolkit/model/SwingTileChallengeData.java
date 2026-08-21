package com.captcha.toolkit.model;

import java.util.List;

/**
 * 滑块摆动图块验证码的类型特定化载荷。
 *
 * @param path            贝塞尔路径点（起点 + 控制点 + 终点，像素坐标）
 * @param startRotation   起始方向（度）
 * @param endRotation     终点方向（度，与目标凹槽一致）
 * @param swingAmplitude  方向摆动幅度（度）
 * @param pieceSize       图块图片边长（像素，含裁剪留白；与凹槽形状大小一致）
 * @param debugT          调试：真凹槽在路径上的位置（0~1，仅 debug 模式返回）
 * @param debugFakeTargets 调试：假凹槽中心坐标（仅 debug 模式返回）
 */
public record SwingTileChallengeData(
        List<PointVo> path,
        Double startRotation,
        Double endRotation,
        Double swingAmplitude,
        Integer pieceSize,
        Double debugT,
        List<PointVo> debugFakeTargets) {
}
