package com.captcha.toolkit.model;

/**
 * 归一化坐标（0~1），用于点选答案提交；
 * 与服务端图片尺寸无关，前端缩放/不同分辨率下语义一致。
 *
 * @param x 归一化横坐标（0~1）
 * @param y 归一化纵坐标（0~1）
 */
public record NormalizedPoint(double x, double y) {
}
