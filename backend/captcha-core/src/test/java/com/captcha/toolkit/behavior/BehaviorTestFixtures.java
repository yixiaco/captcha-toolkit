package com.captcha.toolkit.behavior;

import com.captcha.toolkit.config.BehaviorConfig;
import com.captcha.toolkit.model.CaptchaAnswer;
import com.captcha.toolkit.model.CaptchaSession;
import com.captcha.toolkit.model.NormalizedPoint;
import com.captcha.toolkit.model.PointVo;

import java.util.List;

/**
 * 行为校验测试夹具：为六种验证码模块提供“完整有效轨迹 + 答案 + 会话”，
 * 供通用边界测试复用，避免各模块测试各自维护一套相同基线。
 */
final class BehaviorTestFixtures {

    private BehaviorTestFixtures() {
    }

    /**
     * 单个模块的验证夹具。
     *
     * @param validator     该模块的行为校验器（已开启）
     * @param answer        与完整轨迹匹配的答案
     * @param minimalAnswer 与最小点数轨迹匹配的答案（点选/曲线与完整轨迹答案不同）
     * @param session       该模块的验证码会话
     * @param points        完整有效轨迹点
     * @param minimalPoints 恰好等于点数下限（3 点）的有效轨迹点
     * @param startTime     轨迹起始时间戳
     * @param endTime       轨迹结束时间戳
     */
    record Fixture(
            BehaviorValidator validator,
            CaptchaAnswer answer,
            CaptchaAnswer minimalAnswer,
            CaptchaSession session,
            List<BehaviorPoint> points,
            List<BehaviorPoint> minimalPoints,
            long startTime,
            long endTime) {

        Fixture {
            points = List.copyOf(points);
            minimalPoints = List.copyOf(minimalPoints);
        }

        /** 按默认协议/视口/时间编码完整轨迹 */
        String encode() {
            return encode(1, 340, 190, startTime, endTime, points);
        }

        /** 用指定起止时间编码完整轨迹（用于耗时上下限边界） */
        String encode(long start, long end) {
            return encode(1, 340, 190, start, end, points);
        }

        /** 用指定轨迹点编码（用于事件/坐标/时间边界） */
        String encode(List<BehaviorPoint> modified) {
            return encode(1, 340, 190, startTime, endTime, modified);
        }

        /** 用指定协议、视口、时间与轨迹点编码（用于通用边界） */
        String encode(int protocol, double width, double height,
                      long start, long end, List<BehaviorPoint> modified) {
            return BehaviorTraceCodec.encode(new BehaviorTrace(
                    protocol, width, height, start, end, modified));
        }

        /** 编码恰好等于点数下限的轨迹 */
        String encodeMinimal() {
            return BehaviorTraceCodec.encode(new BehaviorTrace(
                    1, 340, 190, startTime, endTime, minimalPoints));
        }
    }

    /** 全部模块的夹具，供参数化测试使用 */
    static List<Fixture> allModules() {
        return List.of(slider(), click(), rotate(), angle(), scratch(),
                curve(), slideCurve(), swingTile());
    }

    /** 开启行为校验（不开启风险评分）的默认配置 */
    private static BehaviorConfig enabledConfig() {
        BehaviorConfig config = new BehaviorConfig();
        config.setEnabled(true);
        return config;
    }

    /** 拖拽类共用完整轨迹：起点 → 移动 ×2 → 松开，终点 x = endX */
    private static List<BehaviorPoint> dragPoints(double endX) {
        return List.of(
                new BehaviorPoint(0, 0.01, 0.5, BehaviorEventType.START),
                new BehaviorPoint(100, 0.2, 0.5, BehaviorEventType.MOVE),
                new BehaviorPoint(200, 0.35, 0.5, BehaviorEventType.MOVE),
                new BehaviorPoint(300, endX, 0.5, BehaviorEventType.UP));
    }

    /** 拖拽类共用最小轨迹：恰好 3 个点 */
    private static List<BehaviorPoint> dragMinimal(double endX) {
        return List.of(
                new BehaviorPoint(0, 0.5, 0.5, BehaviorEventType.START),
                new BehaviorPoint(100, 0.25, 0.5, BehaviorEventType.MOVE),
                new BehaviorPoint(200, endX, 0.5, BehaviorEventType.UP));
    }

    /** 滑块模块夹具 */
    static Fixture slider() {
        CaptchaAnswer answer = CaptchaAnswer.slider(0.5);
        return new Fixture(
                new SliderBehaviorValidator(enabledConfig()),
                answer, answer,
                CaptchaSession.slider("slider-boundary", "classic", 170, 95, 340, 190, 300_000L),
                dragPoints(0.5), dragMinimal(0.5), 1_000_000L, 1_001_000L);
    }

    /** 旋转模块夹具 */
    static Fixture rotate() {
        CaptchaAnswer answer = CaptchaAnswer.rotate(90.0);
        return new Fixture(
                new RotateBehaviorValidator(enabledConfig()),
                answer, answer,
                CaptchaSession.rotate("rotate-boundary", 340, 190, 90, 300_000L),
                dragPoints(0.5), dragMinimal(0.5), 1_000_000L, 1_001_000L);
    }

    /** 角度验证（圆盘旋转）模块夹具 */
    static Fixture angle() {
        CaptchaAnswer answer = CaptchaAnswer.rotate(180.0);
        return new Fixture(
                new AngleBehaviorValidator(enabledConfig()),
                answer, answer,
                CaptchaSession.angle("angle-boundary", 340, 190, 180, 300_000L),
                dragPoints(0.5), dragMinimal(0.5), 1_000_000L, 1_001_000L);
    }

    /** 刮刮乐模块夹具：单次“按下 → 移动 → 松开”拖拽横扫 */
    static Fixture scratch() {
        CaptchaAnswer answer = CaptchaAnswer.slider(0.5);
        List<BehaviorPoint> minimal = List.of(
                new BehaviorPoint(0, 0.5, 0.5, BehaviorEventType.START),
                new BehaviorPoint(100, 0.25, 0.5, BehaviorEventType.MOVE),
                new BehaviorPoint(200, 0.5, 0.5, BehaviorEventType.UP));
        return new Fixture(
                new ScratchBehaviorValidator(enabledConfig()),
                answer, answer,
                CaptchaSession.scratch("scratch-boundary", 340, 190, 5000,
                        List.of(), List.of(), 300_000L),
                dragPoints(0.5), minimal, 1_000_000L, 1_001_000L);
    }

    /** 滑动曲线模块夹具 */
    static Fixture slideCurve() {
        CaptchaAnswer answer = CaptchaAnswer.slider(0.5);
        return new Fixture(
                new SlideCurveBehaviorValidator(enabledConfig()),
                answer, answer,
                CaptchaSession.slideCurve("slide-curve-boundary", 340, 190, 5000, 300_000L),
                dragPoints(0.5), dragMinimal(0.5), 1_000_000L, 1_001_000L);
    }

    /** 滑块摆动图块模块夹具 */
    static Fixture swingTile() {
        CaptchaAnswer answer = CaptchaAnswer.slider(0.5);
        return new Fixture(
                new SwingTileBehaviorValidator(enabledConfig()),
                answer, answer,
                CaptchaSession.swingTile("swing-tile-boundary", 340, 190, 5000, 300_000L),
                dragPoints(0.5), dragMinimal(0.5), 1_000_000L, 1_001_000L);
    }

    /** 点选模块夹具：两次点击（按下/松开成对），点击之间带移动 */
    static Fixture click() {
        CaptchaAnswer answer = CaptchaAnswer.click(List.of(
                new NormalizedPoint(0.3, 0.4),
                new NormalizedPoint(0.6, 0.7)));
        CaptchaAnswer minimalAnswer = CaptchaAnswer.click(List.of(
                new NormalizedPoint(0.5, 0.5)));
        List<BehaviorPoint> points = List.of(
                new BehaviorPoint(0, 0.3, 0.4, BehaviorEventType.START),
                new BehaviorPoint(60, 0.3, 0.4, BehaviorEventType.DOWN),
                new BehaviorPoint(140, 0.3, 0.4, BehaviorEventType.UP),
                new BehaviorPoint(240, 0.6, 0.7, BehaviorEventType.MOVE),
                new BehaviorPoint(340, 0.6, 0.7, BehaviorEventType.DOWN),
                new BehaviorPoint(420, 0.6, 0.7, BehaviorEventType.UP));
        List<BehaviorPoint> minimal = List.of(
                new BehaviorPoint(0, 0.5, 0.5, BehaviorEventType.START),
                new BehaviorPoint(60, 0.5, 0.5, BehaviorEventType.DOWN),
                new BehaviorPoint(140, 0.5, 0.5, BehaviorEventType.UP));
        return new Fixture(
                new ClickBehaviorValidator(enabledConfig()),
                answer, minimalAnswer,
                CaptchaSession.click("click-boundary", 340, 190,
                        List.of(new PointVo(102, 76), new PointVo(204, 133)),
                        List.of("测", "试"), 300_000L),
                points, minimal, 1_000_000L, 1_001_000L);
    }

    /** 曲线绘制模块夹具：起点/终点与答案曲线首尾一致 */
    static Fixture curve() {
        List<NormalizedPoint> curve = List.of(
                new NormalizedPoint(0.1, 0.1),
                new NormalizedPoint(0.35, 0.35),
                new NormalizedPoint(0.65, 0.65),
                new NormalizedPoint(0.9, 0.9));
        List<BehaviorPoint> points = List.of(
                new BehaviorPoint(0, 0.1, 0.1, BehaviorEventType.START),
                new BehaviorPoint(100, 0.35, 0.35, BehaviorEventType.MOVE),
                new BehaviorPoint(200, 0.65, 0.65, BehaviorEventType.MOVE),
                new BehaviorPoint(300, 0.9, 0.9, BehaviorEventType.UP));
        List<BehaviorPoint> minimal = List.of(
                new BehaviorPoint(0, 0.1, 0.1, BehaviorEventType.START),
                new BehaviorPoint(100, 0.3, 0.3, BehaviorEventType.MOVE),
                new BehaviorPoint(200, 0.5, 0.5, BehaviorEventType.UP));
        return new Fixture(
                new CurveBehaviorValidator(enabledConfig()),
                CaptchaAnswer.curve(curve),
                CaptchaAnswer.curve(List.of(
                        new NormalizedPoint(0.1, 0.1),
                        new NormalizedPoint(0.3, 0.3),
                        new NormalizedPoint(0.5, 0.5))),
                CaptchaSession.curve("curve-boundary", 340, 190,
                        List.of(new PointVo(34, 19), new PointVo(119, 67),
                                new PointVo(221, 124), new PointVo(306, 171)),
                        300_000L),
                points, minimal, 1_000_000L, 1_001_000L);
    }
}
