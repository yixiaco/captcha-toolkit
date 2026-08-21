package com.captcha.toolkit.generator;

import com.captcha.toolkit.config.CurveConfig;
import com.captcha.toolkit.model.CaptchaAnswer;
import com.captcha.toolkit.model.GeneratedCaptcha;
import com.captcha.toolkit.model.NormalizedPoint;
import com.captcha.toolkit.model.PointVo;
import com.captcha.toolkit.model.VerifyResult;
import com.captcha.toolkit.render.SceneBackgroundProvider;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 曲线绘制生成器测试：曲线生成、覆盖率校验与起终点容差。
 * 最短耗时置 0，避免测试真实等待。
 */
class CurveCaptchaGeneratorTest {

    /** 测试配置：最短耗时置 0，其余使用默认值 */
    private static CurveConfig testConfig() {
        CurveConfig config = new CurveConfig();
        config.setMinElapsedMs(0);
        return config;
    }

    /** 创建默认测试生成器（行为校验关闭） */
    private static CurveCaptchaGenerator newGenerator() {
        return new CurveCaptchaGenerator(testConfig(), new SceneBackgroundProvider());
    }

    /** 生成一张调试模式的曲线验证码 */
    private static GeneratedCaptcha generate(CurveCaptchaGenerator generator) {
        return generator.generate(new GenerateRequest("curve-test", Map.of(), true));
    }

    /** 把服务端期望曲线转为归一化答案 */
    private static List<NormalizedPoint> normalize(List<PointVo> curve, int width, int height) {
        return curve.stream()
                .map(p -> new NormalizedPoint(
                        p.getX() / (double) width,
                        p.getY() / (double) height))
                .toList();
    }

    @Test
    void generatesGuidedCurveWithExpectedSampleCount() {
        GeneratedCaptcha captcha = generate(newGenerator());

        assertNotNull(captcha.getImage1());
        assertEquals("curve", captcha.getShape());
        List<PointVo> curve = captcha.getSession().getCurve();
        assertEquals(48, curve.size(), "默认采样点数应为 48");

        // 曲线不能退化为一个点
        PointVo first = curve.getFirst();
        PointVo last = curve.getLast();
        assertFalse(first.getX() == last.getX() && first.getY() == last.getY(),
                "引导曲线起终点不能重合");

        // 曲线应有足够长度：48 个采样点沿曲线展开后总长度明显大于画布短边
        double total = 0;
        for (int i = 1; i < curve.size(); i++) {
            total += Math.hypot(
                    curve.get(i).getX() - curve.get(i - 1).getX(),
                    curve.get(i).getY() - curve.get(i - 1).getY());
        }
        assertTrue(total > 60, "引导曲线长度过短: " + total);
    }

    @Test
    void debugModeExposesExpectedCurve() {
        GeneratedCaptcha captcha = generate(newGenerator());
        assertNotNull(captcha.getDebugCurve());
        assertEquals(captcha.getSession().getCurve(), captcha.getDebugCurve());
    }

    @Test
    void verifiesExactCurve() {
        GeneratedCaptcha captcha = generate(newGenerator());
        int width = captcha.getWidth();
        int height = captcha.getHeight();
        List<NormalizedPoint> answer = normalize(captcha.getSession().getCurve(), width, height);

        VerifyResult result = newGenerator().verify(
                captcha.getSession(), CaptchaAnswer.curve(answer));
        assertTrue(result.isSuccess(), result.getMessage());
    }

    @Test
    void rejectsWrongStartPoint() {
        GeneratedCaptcha captcha = generate(newGenerator());
        int width = captcha.getWidth();
        int height = captcha.getHeight();
        List<NormalizedPoint> answer = new ArrayList<>(
                normalize(captcha.getSession().getCurve(), width, height));
        // 把起点偏移到画布右下角，终点保持不变
        answer.set(0, new NormalizedPoint(0.95, 0.95));

        VerifyResult result = newGenerator().verify(
                captcha.getSession(), CaptchaAnswer.curve(answer));
        assertFalse(result.isSuccess());
        assertEquals("WRONG", result.getCode());
    }

    @Test
    void rejectsWrongEndPoint() {
        GeneratedCaptcha captcha = generate(newGenerator());
        int width = captcha.getWidth();
        int height = captcha.getHeight();
        List<NormalizedPoint> answer = new ArrayList<>(
                normalize(captcha.getSession().getCurve(), width, height));
        answer.set(answer.size() - 1, new NormalizedPoint(0.02, 0.02));

        VerifyResult result = newGenerator().verify(
                captcha.getSession(), CaptchaAnswer.curve(answer));
        assertFalse(result.isSuccess());
        assertEquals("WRONG", result.getCode());
    }

    @Test
    void rejectsLowCoverageCurve() {
        GeneratedCaptcha captcha = generate(newGenerator());
        int width = captcha.getWidth();
        int height = captcha.getHeight();
        List<PointVo> expected = captcha.getSession().getCurve();

        // 只绘制前四分之一 + 直接跳到终点：起终点正确但覆盖率明显不足 60%
        List<NormalizedPoint> answer = new ArrayList<>();
        for (int i = 0; i < expected.size() / 4; i++) {
            answer.add(new NormalizedPoint(
                    expected.get(i).getX() / (double) width,
                    expected.get(i).getY() / (double) height));
        }
        answer.add(new NormalizedPoint(
                expected.getLast().getX() / (double) width,
                expected.getLast().getY() / (double) height));

        VerifyResult result = newGenerator().verify(
                captcha.getSession(), CaptchaAnswer.curve(answer));
        assertFalse(result.isSuccess());
        assertEquals("WRONG", result.getCode());
    }

    @Test
    void rejectsEmptyCurve() {
        GeneratedCaptcha captcha = generate(newGenerator());
        VerifyResult result = newGenerator().verify(
                captcha.getSession(), CaptchaAnswer.curve(List.of()));
        assertFalse(result.isSuccess());
        assertEquals("BAD_REQUEST", result.getCode());
    }
}
