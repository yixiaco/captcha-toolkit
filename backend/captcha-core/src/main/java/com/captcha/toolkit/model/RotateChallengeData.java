package com.captcha.toolkit.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 图片旋转验证码的类型特定化载荷。
 *
 * @param debugAngle 调试：正确答案角度（度，仅 debug 模式返回）
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RotateChallengeData(Double debugAngle) {
}
