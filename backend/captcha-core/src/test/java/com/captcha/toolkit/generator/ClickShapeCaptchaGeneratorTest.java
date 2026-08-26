package com.captcha.toolkit.generator;

import com.captcha.toolkit.config.ClickShapeConfig;
import com.captcha.toolkit.model.CaptchaAnswer;
import com.captcha.toolkit.model.ClickChallengeData;
import com.captcha.toolkit.model.GeneratedCaptcha;
import com.captcha.toolkit.model.NormalizedPoint;
import com.captcha.toolkit.model.VerifyResult;
import com.captcha.toolkit.render.SceneBackgroundProvider;
import com.captcha.toolkit.shape.PuzzleShapeRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 图形点选生成器测试：目标图形布局、提示词图片与顺序校验。
 */
class ClickShapeCaptchaGeneratorTest {

    /** 测试配置：最短耗时置 0，避免真实等待 */
    private static ClickShapeConfig testConfig() {
        ClickShapeConfig config = new ClickShapeConfig();
        config.setMinElapsedMs(0);
        return config;
    }

    /** 创建默认测试生成器（行为校验关闭） */
    private static ClickShapeCaptchaGenerator newGenerator() {
        return new ClickShapeCaptchaGenerator(
                testConfig(), new SceneBackgroundProvider(), new PuzzleShapeRegistry());
    }

    /** 生成一张调试模式的图形点选验证码 */
    private static GeneratedCaptcha<ClickChallengeData> generate() {
        return newGenerator().generate(
                new GenerateRequest("click-shape-test", Map.of(), true));
    }

    /** 按目标坐标构造归一化点击序列 */
    private static List<NormalizedPoint> normalizedTargets(
            GeneratedCaptcha<ClickChallengeData> captcha) {
        return captcha.getData().debugTargets().stream()
                .map(p -> new NormalizedPoint(
                        p.getX() / (double) captcha.getWidth(),
                        p.getY() / (double) captcha.getHeight()))
                .toList();
    }

    @Test
    void generatesImageAndPromptWithTargets() {
        GeneratedCaptcha<ClickChallengeData> captcha = generate();

        assertNotNull(captcha.getImage1());
        ClickChallengeData data = captcha.getData();
        assertNotNull(data.promptImage());
        assertTrue(data.promptImage().startsWith("data:image/png;base64,"));
        assertEquals(3, data.targetCount());
        assertEquals(3, data.debugTargets().size());
        assertEquals(3, captcha.getSession().getTargets().size());
        assertEquals(3, captcha.getSession().getPrompt().size());
        assertEquals("click-shape", captcha.getSession().getType().getCode());
    }

    @Test
    void verifiesAtTargetPointsInOrder() {
        GeneratedCaptcha<ClickChallengeData> captcha = generate();
        VerifyResult result = newGenerator().verify(
                captcha.getSession(),
                CaptchaAnswer.click(normalizedTargets(captcha)));
        assertTrue(result.isSuccess(), result.getMessage());
    }

    @Test
    void rejectsWrongPoint() {
        GeneratedCaptcha<ClickChallengeData> captcha = generate();
        List<NormalizedPoint> points = normalizedTargets(captcha);
        NormalizedPoint first = points.get(0);
        List<NormalizedPoint> wrong = List.of(
                new NormalizedPoint(
                        (first.x() * captcha.getWidth() + 50) / captcha.getWidth(),
                        first.y()),
                points.get(1),
                points.get(2));

        VerifyResult result = newGenerator().verify(captcha.getSession(),
                CaptchaAnswer.click(wrong));
        assertFalse(result.isSuccess());
        assertEquals("WRONG", result.getCode());
    }

    @Test
    void rejectsWrongPointCount() {
        GeneratedCaptcha<ClickChallengeData> captcha = generate();
        List<NormalizedPoint> points = normalizedTargets(captcha).subList(0, 2);

        VerifyResult result = newGenerator().verify(captcha.getSession(),
                CaptchaAnswer.click(points));
        assertFalse(result.isSuccess());
        assertEquals("BAD_REQUEST", result.getCode());
    }
}
