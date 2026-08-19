package com.captcha.toolkit;

import com.captcha.toolkit.image.CaptchaImageCodec;
import com.captcha.toolkit.image.DataUriImageCodec;
import com.captcha.toolkit.model.CaptchaAnswer;
import com.captcha.toolkit.model.CaptchaChallenge;
import com.captcha.toolkit.model.PointVo;
import com.captcha.toolkit.model.VerifyResult;
import com.captcha.toolkit.render.FallbackBackgroundProvider;
import com.captcha.toolkit.render.SceneBackgroundProvider;
import com.captcha.toolkit.store.InMemoryCaptchaSessionStore;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 引擎端到端测试：滑块与点选都走“生成 → 校验 → 一次性会话”的完整流程。
 * 测试环境把最短耗时置 0，避免真实等待。
 */
class CaptchaEngineTest {

    private CaptchaEngine newEngine() {
        CaptchaConfig config = new CaptchaConfig();
        config.setDebugEnabled(true);
        config.getSlider().setMinElapsedMs(0);
        config.getClick().setMinElapsedMs(0);
        CaptchaImageCodec codec = new DataUriImageCodec();
        FallbackBackgroundProvider background = new FallbackBackgroundProvider(
                List.of(new SceneBackgroundProvider()));
        return CaptchaEngine.of(config, new InMemoryCaptchaSessionStore(), codec,
                List.of(), background);
    }

    @Test
    void sliderGeneratesAndVerifiesOnce() {
        CaptchaEngine engine = newEngine();
        CaptchaChallenge challenge = engine.create(CaptchaType.SLIDER,
                Map.of("shape", "classic"), true);

        assertNotNull(challenge.getImage1());
        assertNotNull(challenge.getImage2());
        assertNotNull(challenge.getDebugX());
        assertEquals("slider", challenge.getType());

        VerifyResult ok = engine.verify(challenge.getId(),
                CaptchaAnswer.slider(challenge.getDebugX().doubleValue(), challenge.getWidth()));
        assertTrue(ok.isSuccess(), ok.getMessage());

        // 会话一次性：重复提交必须失败
        VerifyResult again = engine.verify(challenge.getId(),
                CaptchaAnswer.slider(challenge.getDebugX().doubleValue(), challenge.getWidth()));
        assertFalse(again.isSuccess());
    }

    @Test
    void clickGeneratesAndVerifiesInOrder() {
        CaptchaEngine engine = newEngine();
        CaptchaChallenge challenge = engine.create(CaptchaType.CLICK, Map.of(), true);

        assertNotNull(challenge.getImage1());
        assertNotNull(challenge.getPrompt());
        assertEquals(3, challenge.getPrompt().size());
        assertNotNull(challenge.getDebugTargets());

        List<PointVo> points = challenge.getDebugTargets().stream()
                .map(p -> new PointVo(p.getX(), p.getY()))
                .toList();
        VerifyResult ok = engine.verify(challenge.getId(), CaptchaAnswer.click(points));
        assertTrue(ok.isSuccess(), ok.getMessage());
    }

    @Test
    void clickRejectsWrongPoint() {
        CaptchaEngine engine = newEngine();
        CaptchaChallenge challenge = engine.create(CaptchaType.CLICK, Map.of(), true);

        List<PointVo> points = challenge.getDebugTargets().stream()
                .map(p -> new PointVo(p.getX() + 50, p.getY() + 50))
                .toList();
        VerifyResult result = engine.verify(challenge.getId(), CaptchaAnswer.click(points));
        assertFalse(result.isSuccess());
    }

    @Test
    void clickUsesConfiguredTargetText() {
        CaptchaConfig config = new CaptchaConfig();
        config.setDebugEnabled(true);
        config.getClick().setMinElapsedMs(0);
        config.getClick().setTargetText("星巴克");
        CaptchaEngine engine = CaptchaEngine.of(config,
                new InMemoryCaptchaSessionStore(), new DataUriImageCodec(),
                List.of(), new FallbackBackgroundProvider(List.of(new SceneBackgroundProvider())));

        CaptchaChallenge challenge = engine.create(CaptchaType.CLICK, Map.of(), true);
        assertEquals(List.of("星", "巴", "克"), challenge.getPrompt());
        assertEquals(3, challenge.getDebugTargets().size());

        List<PointVo> points = challenge.getDebugTargets().stream()
                .map(p -> new PointVo(p.getX(), p.getY()))
                .toList();
        VerifyResult ok = engine.verify(challenge.getId(), CaptchaAnswer.click(points));
        assertTrue(ok.isSuccess(), ok.getMessage());
    }
}
