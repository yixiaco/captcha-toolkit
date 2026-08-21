package com.captcha.toolkit.generator;

import com.captcha.toolkit.config.SlideCurveConfig;
import com.captcha.toolkit.model.CaptchaAnswer;
import com.captcha.toolkit.model.GeneratedCaptcha;
import com.captcha.toolkit.model.PointVo;
import com.captcha.toolkit.model.SlideCurveChallengeData;
import com.captcha.toolkit.model.VerifyResult;
import com.captcha.toolkit.render.SceneBackgroundProvider;
import com.captcha.toolkit.render.SlideCurveRenderer;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 滑动曲线生成器测试：摆动曲线数据、真/假凹槽与摆动量校验。
 * 最短耗时置 0，避免测试真实等待。
 */
class SlideCurveCaptchaGeneratorTest {

    /** 测试配置：最短耗时置 0，假凹槽 2 个 */
    private static SlideCurveConfig testConfig() {
        SlideCurveConfig config = new SlideCurveConfig();
        config.setMinElapsedMs(0);
        config.setFakeTargetCount(2);
        return config;
    }

    /** 创建默认测试生成器（行为校验关闭） */
    private static SlideCurveCaptchaGenerator newGenerator() {
        return new SlideCurveCaptchaGenerator(testConfig(), new SceneBackgroundProvider());
    }

    /** 生成一张调试模式的滑动曲线验证码 */
    private static GeneratedCaptcha<SlideCurveChallengeData> generate() {
        return newGenerator().generate(
                new GenerateRequest("slide-curve-test", Map.of(), true));
    }

    @Test
    void generatesArtworkAndSwingDataWithFakeGrooves() {
        GeneratedCaptcha<SlideCurveChallengeData> captcha = generate();

        assertNotNull(captcha.getImage1());
        assertEquals(null, captcha.getImage2(), "滑动曲线没有独立小图，曲线由前端实时绘制");
        assertEquals("slide-curve", captcha.getSession().getType().getCode());
        SlideCurveChallengeData data = captcha.getData();
        assertEquals(2, data.endpoints().size());
        assertEquals(testConfig().getSampleCount(), data.shape().size());
        assertTrue(data.amplitude() > 0);
        assertNotNull(data.debugSwing());
        assertTrue(data.debugSwing() >= testConfig().getSwingMin()
                && data.debugSwing() <= testConfig().getSwingMax());
        assertEquals(2, data.debugFakeTargets().size());

        // 假凹槽与真曲线共用两端固定点（左端一致），但形状不同，摆动无法对准
        assertEquals(data.endpoints().getFirst(), data.debugFakeTargets().getFirst());
    }

    @Test
    void verifiesExactSwing() {
        GeneratedCaptcha<SlideCurveChallengeData> captcha = generate();
        double xNorm = captcha.getData().debugSwing();

        VerifyResult result = newGenerator().verify(
                captcha.getSession(), CaptchaAnswer.slider(xNorm));
        assertTrue(result.isSuccess(), result.getMessage());
    }

    @Test
    void rejectsWrongSwing() {
        GeneratedCaptcha<SlideCurveChallengeData> captcha = generate();
        double wrongX = captcha.getData().debugSwing() + 0.2;

        VerifyResult result = newGenerator().verify(
                captcha.getSession(), CaptchaAnswer.slider(wrongX));
        assertFalse(result.isSuccess());
        assertEquals("WRONG", result.getCode());
    }

    @Test
    void rejectsMissingSwing() {
        GeneratedCaptcha<SlideCurveChallengeData> captcha = generate();
        VerifyResult result = newGenerator().verify(
                captcha.getSession(), CaptchaAnswer.slider(null));
        assertFalse(result.isSuccess());
        assertEquals("BAD_REQUEST", result.getCode());
    }

    @Test
    void fakeTargetsAreOptional() {
        SlideCurveConfig config = testConfig();
        config.setFakeTargetCount(0);
        SlideCurveCaptchaGenerator generator = new SlideCurveCaptchaGenerator(
                config, new SceneBackgroundProvider());
        GeneratedCaptcha<SlideCurveChallengeData> captcha = generator.generate(
                new GenerateRequest("slide-curve-no-fake", Map.of(), true));
        assertEquals(0, captcha.getData().debugFakeTargets().size());
    }

    @Test
    void fakeGroovesCannotBeAlignedByAnySwing() {
        SlideCurveConfig config = testConfig();
        SlideCurveRenderer renderer = new SlideCurveRenderer(config, new SceneBackgroundProvider());
        for (int round = 0; round < 10; round++) {
            renderer.run();
            PointVo left = renderer.getLeftEnd();
            PointVo right = renderer.getRightEnd();
            List<Double> shape = renderer.getShape();
            double amplitude = renderer.getAmplitude();
            for (SlideCurveRenderer.FakeGroove fake : renderer.getFakeTargets()) {
                boolean alignable = false;
                for (int s = 0; s <= 100 && !alignable; s++) {
                    double swing = s / 100.0;
                    double factor = (swing * 2 - 1) * amplitude;
                    double maxDist = 0;
                    for (int i = 0; i < shape.size(); i++) {
                        double u = i / (double) (shape.size() - 1);
                        double realX = left.getX() + (right.getX() - left.getX()) * u;
                        double realY = left.getY() + (right.getY() - left.getY()) * u
                                + factor * shape.get(i);
                        PointVo fp = fake.getPoints().get(i);
                        maxDist = Math.max(maxDist,
                                Math.hypot(realX - fp.getX(), realY - fp.getY()));
                    }
                    if (maxDist <= 3) {
                        alignable = true;
                    }
                }
                assertFalse(alignable, "假凹槽不应被任何摆动量对准");
            }
        }
    }

    @Test
    void endpointDistanceIsRandomized() {
        SlideCurveConfig config = testConfig();
        SlideCurveRenderer renderer = new SlideCurveRenderer(config, new SceneBackgroundProvider());
        Set<Integer> spans = new HashSet<>();
        for (int i = 0; i < 12; i++) {
            renderer.run();
            spans.add(renderer.getRightEnd().getX() - renderer.getLeftEnd().getX());
        }
        assertTrue(spans.size() > 1, "两端水平距离应随机变化，而不是固定满宽");
    }
}
