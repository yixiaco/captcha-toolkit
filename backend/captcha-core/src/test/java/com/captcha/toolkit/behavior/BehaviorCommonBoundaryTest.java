package com.captcha.toolkit.behavior;

import com.captcha.toolkit.i18n.CaptchaMessages;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 行为轨迹通用边界测试：对六种验证码模块统一验证协议、视口、时间戳、
 * 耗时、点数、坐标与跳跃阈值的上下边界。
 */
class BehaviorCommonBoundaryTest {

    private static Stream<BehaviorTestFixtures.Fixture> allModules() {
        return BehaviorTestFixtures.allModules().stream();
    }

    @ParameterizedTest
    @MethodSource("allModules")
    void missingTdIsRejected(BehaviorTestFixtures.Fixture fixture) {
        assertEquals(CaptchaMessages.BEHAVIOR_MISSING_TD,
                fixture.validator().validate(null, fixture.answer(), fixture.session()).orElse(""));
        assertEquals(CaptchaMessages.BEHAVIOR_MISSING_TD,
                fixture.validator().validate("   ", fixture.answer(), fixture.session()).orElse(""));
    }

    @ParameterizedTest
    @MethodSource("allModules")
    void malformedTdIsRejected(BehaviorTestFixtures.Fixture fixture) {
        assertEquals(CaptchaMessages.BEHAVIOR_INVALID_FORMAT,
                fixture.validator().validate("not-a-trace", fixture.answer(), fixture.session()).orElse(""));
        assertEquals(CaptchaMessages.BEHAVIOR_INVALID_FORMAT,
                fixture.validator().validate(
                        "1|340|190|1000|2000|0,0.1,0.2,9",
                        fixture.answer(), fixture.session()).orElse(""));
        assertEquals(CaptchaMessages.BEHAVIOR_INVALID_FORMAT,
                fixture.validator().validate(
                        "H4sIAAAAAAA", fixture.answer(), fixture.session()).orElse(""));
    }

    @ParameterizedTest
    @MethodSource("allModules")
    void unsupportedProtocolIsRejected(BehaviorTestFixtures.Fixture fixture) {
        List<BehaviorPoint> points = fixture.points();
        String trace0 = fixture.encode(0, 340, 190,
                fixture.startTime(), fixture.endTime(), points);
        String trace2 = fixture.encode(2, 340, 190,
                fixture.startTime(), fixture.endTime(), points);
        assertEquals(CaptchaMessages.BEHAVIOR_PROTOCOL_UNSUPPORTED,
                fixture.validator().validate(trace0, fixture.answer(), fixture.session()).orElse(""));
        assertEquals(CaptchaMessages.BEHAVIOR_PROTOCOL_UNSUPPORTED,
                fixture.validator().validate(trace2, fixture.answer(), fixture.session()).orElse(""));
    }

    @ParameterizedTest
    @MethodSource("allModules")
    void invalidViewportIsRejected(BehaviorTestFixtures.Fixture fixture) {
        List<BehaviorPoint> points = fixture.points();
        String[] invalid = {"0", "-1", "NaN", "Infinity"};
        for (String value : invalid) {
            assertEquals(CaptchaMessages.BEHAVIOR_INVALID_VIEWPORT,
                    fixture.validator().validate(
                            fixture.encode(1, Double.parseDouble(value), 190,
                                    fixture.startTime(), fixture.endTime(), points),
                            fixture.answer(), fixture.session()).orElse(""),
                    "宽度边界值 " + value + " 应被拒绝");
            assertEquals(CaptchaMessages.BEHAVIOR_INVALID_VIEWPORT,
                    fixture.validator().validate(
                            fixture.encode(1, 340, Double.parseDouble(value),
                                    fixture.startTime(), fixture.endTime(), points),
                            fixture.answer(), fixture.session()).orElse(""),
                    "高度边界值 " + value + " 应被拒绝");
        }
    }

    @ParameterizedTest
    @MethodSource("allModules")
    void invalidTimestampIsRejected(BehaviorTestFixtures.Fixture fixture) {
        long start = fixture.startTime();
        assertEquals(CaptchaMessages.BEHAVIOR_INVALID_TIMESTAMP,
                fixture.validator().validate(
                        fixture.encode(start, start),
                        fixture.answer(), fixture.session()).orElse(""));
        assertEquals(CaptchaMessages.BEHAVIOR_INVALID_TIMESTAMP,
                fixture.validator().validate(
                        fixture.encode(start + 1, start),
                        fixture.answer(), fixture.session()).orElse(""));
    }

    @ParameterizedTest
    @MethodSource("allModules")
    void durationBoundaries(BehaviorTestFixtures.Fixture fixture) {
        long start = fixture.startTime();
        assertEquals(CaptchaMessages.BEHAVIOR_TOO_SHORT,
                fixture.validator().validate(
                        fixture.encode(start, start + 99),
                        fixture.answer(), fixture.session()).orElse(""));
        assertTrue(fixture.validator().validate(
                fixture.encode(start, start + 100),
                fixture.answer(), fixture.session()).isEmpty(),
                "耗时恰好等于下限应通过");
        assertEquals(CaptchaMessages.BEHAVIOR_TOO_LONG,
                fixture.validator().validate(
                        fixture.encode(start, start + 60_001),
                        fixture.answer(), fixture.session()).orElse(""));
        assertTrue(fixture.validator().validate(
                fixture.encode(start, start + 60_000),
                fixture.answer(), fixture.session()).isEmpty(),
                "耗时恰好等于上限应通过");
    }

    @ParameterizedTest
    @MethodSource("allModules")
    void minimumPointBoundaries(BehaviorTestFixtures.Fixture fixture) {
        List<BehaviorPoint> points = fixture.points();
        for (int size : List.of(0, 1, 2)) {
            assertEquals(CaptchaMessages.BEHAVIOR_NOT_ENOUGH_POINTS,
                    fixture.validator().validate(
                            fixture.encode(points.subList(0, size)),
                            fixture.answer(), fixture.session()).orElse(""),
                    "点数 " + size + " 应低于下限");
        }
        assertTrue(fixture.validator().validate(
                fixture.encodeMinimal(), fixture.minimalAnswer(), fixture.session()).isEmpty(),
                "点数恰好等于下限应通过");
    }

    @ParameterizedTest
    @MethodSource("allModules")
    void firstPointOffsetBoundaries(BehaviorTestFixtures.Fixture fixture) {
        List<BehaviorPoint> atBoundary = shiftTimes(fixture.points(), 100);
        assertTrue(fixture.validator().validate(
                fixture.encode(atBoundary),
                fixture.answer(), fixture.session()).isEmpty(),
                "起点偏移恰好等于 100ms 应通过");

        List<BehaviorPoint> overBoundary = shiftTimes(fixture.points(), 101);
        assertEquals(CaptchaMessages.BEHAVIOR_INVALID_START_TIME,
                fixture.validator().validate(
                        fixture.encode(overBoundary),
                        fixture.answer(), fixture.session()).orElse(""));

        List<BehaviorPoint> negative = withTime(fixture.points(), 0, -1);
        assertEquals(CaptchaMessages.BEHAVIOR_INVALID_START_TIME,
                fixture.validator().validate(
                        fixture.encode(negative),
                        fixture.answer(), fixture.session()).orElse(""));
    }

    @ParameterizedTest
    @MethodSource("allModules")
    void decreasingTimeIsRejected(BehaviorTestFixtures.Fixture fixture) {
        List<BehaviorPoint> points = fixture.points();
        List<BehaviorPoint> outOfOrder = withTime(points, 2, points.get(1).timeMs() - 1);
        assertEquals(CaptchaMessages.BEHAVIOR_TIME_OUT_OF_ORDER,
                fixture.validator().validate(
                        fixture.encode(outOfOrder),
                        fixture.answer(), fixture.session()).orElse(""));
    }

    @ParameterizedTest
    @MethodSource("allModules")
    void equalTimeIsAccepted(BehaviorTestFixtures.Fixture fixture) {
        List<BehaviorPoint> points = fixture.points();
        List<BehaviorPoint> sameTime = withTime(points, 1, points.getFirst().timeMs());
        assertTrue(fixture.validator().validate(
                fixture.encode(sameTime),
                fixture.answer(), fixture.session()).isEmpty(),
                "相邻点时间相同（时间未倒退）应通过");
    }

    @ParameterizedTest
    @MethodSource("allModules")
    void outOfRangeCoordinatesAreRejected(BehaviorTestFixtures.Fixture fixture) {
        double[] invalidX = {-0.0001, 1.0001};
        double[] invalidY = {-0.0001, 1.0001};
        for (double x : invalidX) {
            assertCoordinateRejected(fixture, x, 0.5, "x=" + x);
        }
        for (double y : invalidY) {
            assertCoordinateRejected(fixture, 0.5, y, "y=" + y);
        }
        // NaN/Infinity 必须显式拒绝，避免绕过“< 0 / > 1”比较
        assertCoordinateRejected(fixture, Double.NaN, 0.5, "x=NaN");
        assertCoordinateRejected(fixture, Double.POSITIVE_INFINITY, 0.5, "x=+Inf");
        assertCoordinateRejected(fixture, Double.NEGATIVE_INFINITY, 0.5, "x=-Inf");
        assertCoordinateRejected(fixture, 0.5, Double.NaN, "y=NaN");
    }

    @ParameterizedTest
    @MethodSource("allModules")
    void firstPointOutOfRangeIsRejected(BehaviorTestFixtures.Fixture fixture) {
        List<BehaviorPoint> points = new ArrayList<>(fixture.points());
        BehaviorPoint first = points.getFirst();
        points.set(0, new BehaviorPoint(first.timeMs(), 1.01, 0.5, first.type()));
        assertEquals(CaptchaMessages.BEHAVIOR_COORDINATE_OUT_OF_RANGE,
                fixture.validator().validate(
                        fixture.encode(points),
                        fixture.answer(), fixture.session()).orElse(""),
                "起点坐标越界同样应被拒绝");
    }

    @ParameterizedTest
    @MethodSource("allModules")
    void jumpAboveMaximumIsRejected(BehaviorTestFixtures.Fixture fixture) {
        // 把中间点从基线位置挪到 (0.85, 0.5)，与前一点的归一化距离必然超过 0.5
        List<BehaviorPoint> points = withPoint(fixture.points(), 1, 0.85, 0.5);
        assertEquals(CaptchaMessages.BEHAVIOR_JUMP_TOO_LARGE,
                fixture.validator().validate(
                        fixture.encode(points),
                        fixture.answer(), fixture.session()).orElse(""));
    }

    /** 断言指定坐标值触发“坐标越界” */
    private static void assertCoordinateRejected(
            BehaviorTestFixtures.Fixture fixture, double x, double y, String label) {
        List<BehaviorPoint> points = withPoint(fixture.points(), 1, x, y);
        assertEquals(CaptchaMessages.BEHAVIOR_COORDINATE_OUT_OF_RANGE,
                fixture.validator().validate(
                        fixture.encode(points),
                        fixture.answer(), fixture.session()).orElse(""),
                "坐标 " + label + " 应被拒绝");
    }

    /** 替换指定索引轨迹点的坐标（保留时间与事件类型） */
    private static List<BehaviorPoint> withPoint(
            List<BehaviorPoint> points, int index, double x, double y) {
        List<BehaviorPoint> modified = new ArrayList<>(points);
        BehaviorPoint point = modified.get(index);
        modified.set(index, new BehaviorPoint(point.timeMs(), x, y, point.type()));
        return modified;
    }

    /** 替换指定索引轨迹点的时间（保留坐标与事件类型） */
    private static List<BehaviorPoint> withTime(
            List<BehaviorPoint> points, int index, int timeMs) {
        List<BehaviorPoint> modified = new ArrayList<>(points);
        BehaviorPoint point = modified.get(index);
        modified.set(index, new BehaviorPoint(timeMs, point.x(), point.y(), point.type()));
        return modified;
    }

    /** 所有轨迹点的时间统一平移 offset */
    private static List<BehaviorPoint> shiftTimes(
            List<BehaviorPoint> points, int offset) {
        List<BehaviorPoint> shifted = new ArrayList<>();
        for (BehaviorPoint point : points) {
            shifted.add(new BehaviorPoint(
                    point.timeMs() + offset, point.x(), point.y(), point.type()));
        }
        return shifted;
    }
}
