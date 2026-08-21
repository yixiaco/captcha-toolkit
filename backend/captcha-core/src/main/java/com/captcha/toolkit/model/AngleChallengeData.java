package com.captcha.toolkit.model;

/**
 * 角度验证码的类型特定化载荷。
 *
 * @param discSize   圆形图直径（像素），前端按此尺寸居中渲染
 * @param debugAngle 调试：正确答案角度（度，仅 debug 模式返回）
 */
public record AngleChallengeData(Integer discSize, Double debugAngle) {
}
