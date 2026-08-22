package com.captcha.toolkit.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 曲线绘制验证码的类型特定化载荷。
 *
 * @param debugCurve 调试：期望曲线采样点（像素坐标，仅 debug 模式返回）
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CurveChallengeData(List<PointVo> debugCurve) {
}
