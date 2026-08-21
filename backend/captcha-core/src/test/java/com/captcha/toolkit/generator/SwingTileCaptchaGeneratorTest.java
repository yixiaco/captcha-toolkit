package com.captcha.toolkit.generator;

import com.captcha.toolkit.config.SwingTileConfig;
import com.captcha.toolkit.model.CaptchaAnswer;
import com.captcha.toolkit.model.GeneratedCaptcha;
import com.captcha.toolkit.model.SwingTileChallengeData;
import com.captcha.toolkit.model.VerifyResult;
import com.captcha.toolkit.render.SceneBackgroundProvider;
import com.captcha.toolkit.shape.PuzzleShapeRegistry;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 滑块摆动图块生成器测试：贝塞尔路径、图块与终点位置校验。
 * 最短耗时置 0，避免测试真实等待。
 */
class SwingTileCaptchaGeneratorTest {

    /** 测试配置：最短耗时置 0，假凹槽 2 个 */
    private static SwingTileConfig testConfig() {
        SwingTileConfig config = new SwingTileConfig();
        config.setMinElapsedMs(0);
        config.setFakeTargetCount(2);
        return config;
    }

    /** 创建默认测试生成器（行为校验关闭） */
    private static SwingTileCaptchaGenerator newGenerator() {
        return new SwingTileCaptchaGenerator(testConfig(), new SceneBackgroundProvider(),
                new PuzzleShapeRegistry());
    }

    /** 生成一张调试模式的滑块摆动图块验证码 */
    private static GeneratedCaptcha<SwingTileChallengeData> generate() {
        return newGenerator().generate(new GenerateRequest("swing-tile-test", Map.of(), true));
    }

    @Test
    void generatesArtworkAndPieceWithBezierPath() {
        GeneratedCaptcha<SwingTileChallengeData> captcha = generate();

        assertNotNull(captcha.getImage1());
        assertNotNull(captcha.getImage2());
        assertEquals("swing-tile", captcha.getSession().getType().getCode());
        SwingTileChallengeData data = captcha.getData();
        // 起点 + 默认 2 个控制点 + 终点
        assertEquals(4, data.path().size());
        assertNotNull(data.startRotation());
        assertNotNull(data.endRotation());
        assertTrue(data.swingAmplitude() > 0);
        assertTrue(data.pieceSize() > 0);
        assertNotNull(data.debugT());
        assertTrue(data.debugT() >= testConfig().getAnswerMin()
                && data.debugT() <= testConfig().getAnswerMax());
        assertEquals(2, data.debugFakeTargets().size());
    }

    @Test
    void verifiesAtAnswerPosition() {
        GeneratedCaptcha<SwingTileChallengeData> captcha = generate();
        VerifyResult result = newGenerator().verify(
                captcha.getSession(), CaptchaAnswer.slider(captcha.getData().debugT()));
        assertTrue(result.isSuccess(), result.getMessage());
    }

    @Test
    void rejectsWrongPosition() {
        GeneratedCaptcha<SwingTileChallengeData> captcha = generate();
        VerifyResult result = newGenerator().verify(
                captcha.getSession(), CaptchaAnswer.slider(captcha.getData().debugT() + 0.2));
        assertFalse(result.isSuccess());
        assertEquals("WRONG", result.getCode());
    }

    @Test
    void rejectsMissingPosition() {
        GeneratedCaptcha<SwingTileChallengeData> captcha = generate();
        VerifyResult result = newGenerator().verify(
                captcha.getSession(), CaptchaAnswer.slider(null));
        assertFalse(result.isSuccess());
        assertEquals("BAD_REQUEST", result.getCode());
    }
}
