package com.captcha.toolkit.generator;

import com.captcha.toolkit.config.ScratchConfig;
import com.captcha.toolkit.model.CaptchaAnswer;
import com.captcha.toolkit.model.GeneratedCaptcha;
import com.captcha.toolkit.model.ScratchChallengeData;
import com.captcha.toolkit.model.VerifyResult;
import com.captcha.toolkit.render.SceneBackgroundProvider;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 刮刮乐（横扫）生成器测试：提示/调试答案、答案滑块位置与“停早/停晚”判定。
 * 最短耗时置 0，避免测试真实等待。
 */
class ScratchCaptchaGeneratorTest {

    /** 测试配置：最短耗时置 0 */
    private static ScratchConfig testConfig() {
        ScratchConfig config = new ScratchConfig();
        config.setMinElapsedMs(0);
        return config;
    }

    /** 创建默认测试生成器（行为校验关闭） */
    private static ScratchCaptchaGenerator newGenerator() {
        return new ScratchCaptchaGenerator(testConfig(), new SceneBackgroundProvider());
    }

    /** 生成一张调试模式的刮刮乐验证码 */
    private static GeneratedCaptcha<ScratchChallengeData> generate() {
        return newGenerator().generate(new GenerateRequest("scratch-test", Map.of(), true));
    }

    @Test
    void generatesBackgroundWithPromptAndAnswerX() {
        GeneratedCaptcha<ScratchChallengeData> captcha = generate();

        assertNotNull(captcha.getImage1());
        assertEquals(null, captcha.getImage2(), "刮刮乐没有独立小图，蒙版由前端绘制");
        assertEquals("scratch", captcha.getSession().getType().getCode());
        ScratchChallengeData data = captcha.getData();
        // 提示词以单张透明背景图片下发
        assertNotNull(data.promptImage());
        assertTrue(data.promptImage().startsWith("data:image/png;base64,"));
        // 目标数量在配置范围内随机
        assertTrue(data.targetCount() >= testConfig().getTargetCountMin()
                && data.targetCount() <= testConfig().getTargetCount());
        assertNotNull(data.debugX());
        assertTrue(data.debugX() > 0 && data.debugX() < 1);
        assertEquals(data.targetCount(), data.debugTargets().size());
        assertEquals(testConfig().getPatternCount(), data.debugPatterns().size());
    }

    @Test
    void verifiesStoppingExactlyAtAnswerX() {
        GeneratedCaptcha<ScratchChallengeData> captcha = generate();
        double answerX = captcha.getData().debugX();

        VerifyResult result = newGenerator().verify(
                captcha.getSession(), CaptchaAnswer.slider(answerX));
        assertTrue(result.isSuccess(), result.getMessage());
    }

    @Test
    void acceptsPositionWithinTolerance() {
        GeneratedCaptcha<ScratchChallengeData> captcha = generate();
        double answerX = captcha.getData().debugX();

        VerifyResult result = newGenerator().verify(
                captcha.getSession(), CaptchaAnswer.slider(answerX + 0.01));
        assertTrue(result.isSuccess(), result.getMessage());
    }

    @Test
    void rejectsStoppingTooEarly() {
        GeneratedCaptcha<ScratchChallengeData> captcha = generate();
        double answerX = captcha.getData().debugX();

        VerifyResult result = newGenerator().verify(
                captcha.getSession(), CaptchaAnswer.slider(answerX - 0.1));
        assertFalse(result.isSuccess());
        assertEquals("WRONG", result.getCode());
    }

    @Test
    void rejectsStoppingTooLate() {
        GeneratedCaptcha<ScratchChallengeData> captcha = generate();
        double answerX = captcha.getData().debugX();

        VerifyResult result = newGenerator().verify(
                captcha.getSession(), CaptchaAnswer.slider(answerX + 0.1));
        assertFalse(result.isSuccess());
        assertEquals("WRONG", result.getCode());
    }

    @Test
    void rejectsMissingXNorm() {
        GeneratedCaptcha<ScratchChallengeData> captcha = generate();

        VerifyResult result = newGenerator().verify(
                captcha.getSession(), CaptchaAnswer.slider(null));
        assertFalse(result.isSuccess());
        assertEquals("BAD_REQUEST", result.getCode());
    }
}
