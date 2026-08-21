package com.captcha.toolkit.behavior;

import com.captcha.toolkit.config.ClientBehaviorConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * 拖拽（滑块/旋转）风险评分器。
 *
 * <p>特征：速度均匀性（机器人匀速直线）、末端减速（人类接近目标会减速）、
 * 起始停顿（人类按下后会短暂犹豫）、路径效率（人类轨迹有轻微摆动）。
 * 样本不足的特征权重自动降为 0，避免稀疏触摸轨迹被误判。</p>
 */
public class DragBehaviorRiskScorer extends AbstractBehaviorRiskScorer {

    /** 速度变异系数：低于该区间下界视为“匀速机器” */
    private static final double SPEED_CV_LOW = 0.10;
    private static final double SPEED_CV_HIGH = 0.30;

    /** 末端平均速度 / 峰值速度：高于该区间上界视为“没有减速” */
    private static final double END_RATIO_LOW = 0.45;
    private static final double END_RATIO_HIGH = 0.80;

    /** 路径长度 / 直线位移的超出量：低于该区间下界视为“绝对直线” */
    private static final double EXTRA_PATH_LOW = 0.005;
    private static final double EXTRA_PATH_HIGH = 0.05;

    /** 人类按下到开始移动的合理停顿区间（毫秒） */
    private static final double START_PAUSE_LOW_MS = 0;
    private static final double START_PAUSE_HIGH_MS = 120;

    /** 判定“开始移动”的最小累计归一化距离 */
    private static final double START_MOVE_DISTANCE = 0.02;

    @Override
    public BehaviorRiskResult score(BehaviorTrace trace, ClientBehaviorConfig profile) {
        List<Segment> segments = segments(trace.points());
        List<BehaviorRiskResult.Feature> features = new ArrayList<>();
        features.add(speedUniformity(segments));
        features.add(endDeceleration(segments));
        features.add(startPause(trace.points()));
        features.add(pathEfficiency(trace.points()));
        return aggregate(profile, features);
    }

    /** 速度均匀性：采样点足够时，速度变异系数越低越可疑 */
    private BehaviorRiskResult.Feature speedUniformity(List<Segment> segments) {
        if (segments.size() < 3) {
            return new BehaviorRiskResult.Feature("speed-uniformity", 0, 0, 0);
        }
        double[] speeds = segments.stream().mapToDouble(Segment::speed).toArray();
        double cv = coefficientOfVariation(speeds);
        return new BehaviorRiskResult.Feature("speed-uniformity", cv, 0.35,
                anomalyLow(cv, SPEED_CV_LOW, SPEED_CV_HIGH));
    }

    /** 末端减速：最后两段的平均速度与峰值速度的比值 */
    private BehaviorRiskResult.Feature endDeceleration(List<Segment> segments) {
        if (segments.size() < 3) {
            return new BehaviorRiskResult.Feature("end-deceleration", 0, 0, 0);
        }
        double peak = 0;
        for (Segment segment : segments) {
            peak = Math.max(peak, segment.speed());
        }
        if (peak <= 0) {
            return new BehaviorRiskResult.Feature("end-deceleration", 0, 0, 0);
        }
        int count = Math.min(2, segments.size());
        double lastSum = 0;
        for (int i = segments.size() - count; i < segments.size(); i++) {
            lastSum += segments.get(i).speed();
        }
        double ratio = lastSum / (count * peak);
        return new BehaviorRiskResult.Feature("end-deceleration", ratio, 0.25,
                anomalyHigh(ratio, END_RATIO_LOW, END_RATIO_HIGH));
    }

    /** 起始停顿：从按下到累计位移超过阈值的时间，越短越可疑 */
    private BehaviorRiskResult.Feature startPause(List<BehaviorPoint> points) {
        if (points.size() < 2) {
            return new BehaviorRiskResult.Feature("start-pause", 0, 0, 0);
        }
        BehaviorPoint start = points.getFirst();
        double accumulated = 0;
        for (int i = 1; i < points.size(); i++) {
            BehaviorPoint current = points.get(i);
            accumulated += Math.hypot(
                    current.x() - points.get(i - 1).x(),
                    current.y() - points.get(i - 1).y());
            if (accumulated >= START_MOVE_DISTANCE) {
                double pause = current.timeMs() - start.timeMs();
                return new BehaviorRiskResult.Feature("start-pause", pause, 0.20,
                        anomalyLow(pause, START_PAUSE_LOW_MS, START_PAUSE_HIGH_MS));
            }
        }
        return new BehaviorRiskResult.Feature("start-pause", 0, 0, 0);
    }

    /** 路径效率：实际路径长度与首尾直线位移的比值，越接近 1 越可疑 */
    private BehaviorRiskResult.Feature pathEfficiency(List<BehaviorPoint> points) {
        if (points.size() < 2) {
            return new BehaviorRiskResult.Feature("path-efficiency", 0, 0, 0);
        }
        double displacement = Math.hypot(
                points.getLast().x() - points.getFirst().x(),
                points.getLast().y() - points.getFirst().y());
        if (displacement <= 0) {
            return new BehaviorRiskResult.Feature("path-efficiency", 0, 0, 0);
        }
        double pathLength = 0;
        for (int i = 1; i < points.size(); i++) {
            pathLength += Math.hypot(
                    points.get(i).x() - points.get(i - 1).x(),
                    points.get(i).y() - points.get(i - 1).y());
        }
        double extra = pathLength / displacement - 1;
        return new BehaviorRiskResult.Feature("path-efficiency", extra, 0.20,
                anomalyLow(extra, EXTRA_PATH_LOW, EXTRA_PATH_HIGH));
    }
}
