package com.captcha.toolkit.behavior;

import com.captcha.toolkit.config.ClientBehaviorConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * 风险评分公共基类：提供分段速度、变异系数、归一化等数学工具，
 * 以及按有效权重汇总特征分数的模板方法。
 */
public abstract class AbstractBehaviorRiskScorer implements BehaviorRiskScorer {

    /** 相邻轨迹点之间的移动段 */
    protected record Segment(double distance, double dt, double speed) {
    }

    /**
     * 汇总特征：按有效权重归一化得到 0~1 综合分数；
     * 样本不足的特征权重为 0，自动从汇总中剔除。
     */
    protected BehaviorRiskResult aggregate(
            ClientBehaviorConfig profile, List<BehaviorRiskResult.Feature> features) {
        double weightSum = 0;
        double weightedAnomaly = 0;
        for (BehaviorRiskResult.Feature feature : features) {
            weightSum += feature.weight();
            weightedAnomaly += feature.weight() * feature.anomaly();
        }
        double score = weightSum <= 0 ? 0 : clamp01(weightedAnomaly / weightSum);
        return new BehaviorRiskResult(score, profile.getRiskThreshold(), features);
    }

    /**
     * 提取相邻轨迹点移动段；原地停留（距离为 0）或时间未推进的点不参与计算。
     */
    protected List<Segment> segments(List<BehaviorPoint> points) {
        List<Segment> result = new ArrayList<>();
        for (int i = 1; i < points.size(); i++) {
            BehaviorPoint prev = points.get(i - 1);
            BehaviorPoint current = points.get(i);
            long dt = current.timeMs() - prev.timeMs();
            double distance = Math.hypot(
                    current.x() - prev.x(), current.y() - prev.y());
            if (dt <= 0 || distance <= 0) {
                continue;
            }
            result.add(new Segment(distance, dt, distance / dt));
        }
        return result;
    }

    /**
     * 计算变异系数（标准差 / 均值）；样本少于 2 或均值为 0 时返回 0。
     */
    protected double coefficientOfVariation(double[] values) {
        if (values.length < 2) {
            return 0;
        }
        double mean = 0;
        for (double value : values) {
            mean += value;
        }
        mean /= values.length;
        if (mean <= 0) {
            return 0;
        }
        double variance = 0;
        for (double value : values) {
            double diff = value - mean;
            variance += diff * diff;
        }
        return Math.sqrt(variance / values.length) / mean;
    }

    /**
     * 低值异常：value 越低越像机器，低于 low 记 1 分，高于 high 记 0 分，中间线性过渡。
     */
    protected double anomalyLow(double value, double low, double high) {
        if (value <= low) {
            return 1;
        }
        if (value >= high) {
            return 0;
        }
        return clamp01((high - value) / (high - low));
    }

    /**
     * 高值异常：value 越高越像机器，高于 high 记 1 分，低于 low 记 0 分，中间线性过渡。
     */
    protected double anomalyHigh(double value, double low, double high) {
        if (value >= high) {
            return 1;
        }
        if (value <= low) {
            return 0;
        }
        return clamp01((value - low) / (high - low));
    }

    /** 限制到 [0,1] */
    protected double clamp01(double value) {
        return Math.max(0, Math.min(1, value));
    }
}
