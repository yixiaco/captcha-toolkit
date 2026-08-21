package com.captcha.toolkit.generator;

import com.captcha.toolkit.config.AngleConfig;
import com.captcha.toolkit.model.AngleChallengeData;
import com.captcha.toolkit.model.CaptchaAnswer;
import com.captcha.toolkit.model.GeneratedCaptcha;
import com.captcha.toolkit.model.VerifyResult;
import com.captcha.toolkit.render.SceneBackgroundProvider;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 角度验证生成器测试：圆盘/背景渲染、答案角度与容差校验。
 * 最短耗时置 0，避免测试真实等待。
 */
class AngleCaptchaGeneratorTest {

    /** 测试配置：最短耗时置 0 */
    private static AngleConfig testConfig() {
        AngleConfig config = new AngleConfig();
        config.setMinElapsedMs(0);
        return config;
    }

    /** 创建默认测试生成器（行为校验关闭） */
    private static AngleCaptchaGenerator newGenerator() {
        return new AngleCaptchaGenerator(testConfig(), new SceneBackgroundProvider());
    }

    /** 生成一张调试模式的角度验证码 */
    private static GeneratedCaptcha<AngleChallengeData> generate() {
        return newGenerator().generate(
                new GenerateRequest("angle-test", Map.of(), true));
    }

    @Test
    void generatesDiscOnlyWithDebugAnswer() {
        GeneratedCaptcha<AngleChallengeData> captcha = generate();

        assertEquals(null, captcha.getImage1(), "角度验证不再下发背景图");
        assertNotNull(captcha.getImage2());
        assertNotNull(captcha.getData().discSize());
        assertTrue(captcha.getData().discSize() > 0);
        // 圆盘图必须是正方形，避免前端拉伸成椭圆
        assertEquals(captcha.getData().discSize(), captcha.getImage2().getWidth());
        assertEquals(captcha.getData().discSize(), captcha.getImage2().getHeight());
        assertEquals("angle", captcha.getSession().getType().getCode());
        AngleChallengeData data = captcha.getData();
        assertNotNull(data.debugAngle());
        assertTrue(data.debugAngle() > 0 && data.debugAngle() < 360);
    }

    @Test
    void verifiesExactAngle() {
        GeneratedCaptcha<AngleChallengeData> captcha = generate();
        double answer = captcha.getData().debugAngle();

        VerifyResult result = newGenerator().verify(
                captcha.getSession(), CaptchaAnswer.rotate(answer));
        assertTrue(result.isSuccess(), result.getMessage());
    }

    @Test
    void acceptsAngleWithinTolerance() {
        GeneratedCaptcha<AngleChallengeData> captcha = generate();
        double answer = captcha.getData().debugAngle();

        VerifyResult result = newGenerator().verify(
                captcha.getSession(), CaptchaAnswer.rotate(answer + 3));
        assertTrue(result.isSuccess(), result.getMessage());
    }

    @Test
    void rejectsAngleBeyondTolerance() {
        GeneratedCaptcha<AngleChallengeData> captcha = generate();
        double answer = captcha.getData().debugAngle();

        VerifyResult result = newGenerator().verify(
                captcha.getSession(), CaptchaAnswer.rotate(answer + 30));
        assertFalse(result.isSuccess());
        assertEquals("WRONG", result.getCode());
    }

    @Test
    void rejectsMissingAngle() {
        GeneratedCaptcha<AngleChallengeData> captcha = generate();

        VerifyResult result = newGenerator().verify(
                captcha.getSession(), CaptchaAnswer.rotate(null));
        assertFalse(result.isSuccess());
        assertEquals("BAD_REQUEST", result.getCode());
    }
}
