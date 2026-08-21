package com.captcha.toolkit.behavior;

import java.util.List;

/**
 * 行为风险评分结果：综合分数与各特征明细。
 *
 * <p>分数为 0~1，越大越像机器；超过画像阈值时由
 * {@link AbstractBehaviorValidator} 判定为“行为轨迹风险过高”。</p>
 *
 * @param score     归一化综合风险分数（0~1）
 * @param threshold 当前画像的拒绝阈值（供日志/调试对比）
 * @param features  参与评分的特征明细
 */
public record BehaviorRiskResult(
        double score,
        double threshold,
        List<Feature> features) {

    public BehaviorRiskResult {
        features = List.copyOf(features);
    }

    /**
     * 单个风险特征的计算结果。
     *
     * @param name    特征名（英文小写，如 speed-uniformity）
     * @param value   特征原始值
     * @param weight  权重（0 表示样本不足，未参与评分）
     * @param anomaly 该特征的异常度（0~1，1 表示非常像机器）
     */
    public record Feature(String name, double value, double weight, double anomaly) {
    }
}
