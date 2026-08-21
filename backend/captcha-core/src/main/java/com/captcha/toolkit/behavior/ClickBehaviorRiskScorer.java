package com.captcha.toolkit.behavior;

import com.captcha.toolkit.config.ClientBehaviorConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * 点选风险评分器。
 *
 * <p>特征：点击间移动速度均匀性、单次按下时长一致性、相邻点击间隔一致性、
 * 是否存在完全重复的点击坐标。人类点击的时长与间隔天然有波动，
 * 机器常呈现高度一致甚至完全相同的数值。</p>
 */
public class ClickBehaviorRiskScorer extends AbstractBehaviorRiskScorer {

    /** 点击间移动速度变异系数：低于该区间下界视为“匀速移动” */
    private static final double MOVE_CV_LOW = 0.10;
    private static final double MOVE_CV_HIGH = 0.30;

    /** 按下到松开时长变异系数：低于该区间下界视为“点击时长过于一致” */
    private static final double DWELL_CV_LOW = 0.08;
    private static final double DWELL_CV_HIGH = 0.30;

    /** 相邻点击间隔变异系数：低于该区间下界视为“节奏过于机械” */
    private static final double INTERVAL_CV_LOW = 0.10;
    private static final double INTERVAL_CV_HIGH = 0.35;

    @Override
    public BehaviorRiskResult score(BehaviorTrace trace, ClientBehaviorConfig profile) {
        List<BehaviorPoint> points = trace.points();
        List<BehaviorRiskResult.Feature> features = new ArrayList<>();
        features.add(moveUniformity(segments(points)));
        features.add(dwellUniformity(pressReleasePairs(points)));
        features.add(intervalUniformity(pressReleasePairs(points)));
        features.add(duplicateDowns(points));
        return aggregate(profile, features);
    }

    /** 点击间移动速度均匀性 */
    private BehaviorRiskResult.Feature moveUniformity(List<Segment> segments) {
        if (segments.size() < 3) {
            return new BehaviorRiskResult.Feature("move-uniformity", 0, 0, 0);
        }
        double[] speeds = segments.stream().mapToDouble(Segment::speed).toArray();
        double cv = coefficientOfVariation(speeds);
        return new BehaviorRiskResult.Feature("move-uniformity", cv, 0.35,
                anomalyLow(cv, MOVE_CV_LOW, MOVE_CV_HIGH));
    }

    /** 单次按下到松开时长的一致性 */
    private BehaviorRiskResult.Feature dwellUniformity(List<long[]> pairs) {
        if (pairs.size() < 2) {
            return new BehaviorRiskResult.Feature("dwell-uniformity", 0, 0, 0);
        }
        double[] dwells = new double[pairs.size()];
        for (int i = 0; i < pairs.size(); i++) {
            dwells[i] = pairs.get(i)[1] - pairs.get(i)[0];
        }
        double cv = coefficientOfVariation(dwells);
        return new BehaviorRiskResult.Feature("dwell-uniformity", cv, 0.30,
                anomalyLow(cv, DWELL_CV_LOW, DWELL_CV_HIGH));
    }

    /** 相邻点击按下时间间隔的一致性 */
    private BehaviorRiskResult.Feature intervalUniformity(List<long[]> pairs) {
        if (pairs.size() < 2) {
            return new BehaviorRiskResult.Feature("interval-uniformity", 0, 0, 0);
        }
        double[] intervals = new double[pairs.size() - 1];
        for (int i = 1; i < pairs.size(); i++) {
            intervals[i - 1] = pairs.get(i)[0] - pairs.get(i - 1)[0];
        }
        double cv = coefficientOfVariation(intervals);
        return new BehaviorRiskResult.Feature("interval-uniformity", cv, 0.20,
                anomalyLow(cv, INTERVAL_CV_LOW, INTERVAL_CV_HIGH));
    }

    /** 是否存在坐标完全相同的重复点击（人类几乎不会精确复现） */
    private BehaviorRiskResult.Feature duplicateDowns(List<BehaviorPoint> points) {
        List<BehaviorPoint> downs = points.stream()
                .filter(point -> point.type() == BehaviorEventType.DOWN)
                .toList();
        if (downs.size() < 2) {
            return new BehaviorRiskResult.Feature("duplicate-downs", 0, 0, 0);
        }
        boolean duplicate = false;
        for (int i = 1; i < downs.size(); i++) {
            if (downs.get(i - 1).x() == downs.get(i).x()
                    && downs.get(i - 1).y() == downs.get(i).y()) {
                duplicate = true;
                break;
            }
        }
        return new BehaviorRiskResult.Feature(
                "duplicate-downs", duplicate ? 1 : 0, 0.15, duplicate ? 1 : 0);
    }

    /** 提取按下/松开时间对 */
    private static List<long[]> pressReleasePairs(List<BehaviorPoint> points) {
        List<long[]> pairs = new ArrayList<>();
        BehaviorPoint down = null;
        for (BehaviorPoint point : points) {
            if (point.type() == BehaviorEventType.DOWN) {
                down = point;
            } else if (point.type() == BehaviorEventType.UP && down != null) {
                pairs.add(new long[]{down.timeMs(), point.timeMs()});
                down = null;
            }
        }
        return pairs;
    }
}
