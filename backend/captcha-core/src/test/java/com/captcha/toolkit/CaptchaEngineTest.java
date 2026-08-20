package com.captcha.toolkit;

import com.captcha.toolkit.config.CaptchaConfig;
import com.captcha.toolkit.image.CaptchaImageCodec;
import com.captcha.toolkit.image.DataUriImageCodec;
import com.captcha.toolkit.model.CaptchaAnswer;
import com.captcha.toolkit.model.CaptchaChallenge;
import com.captcha.toolkit.model.PointVo;
import com.captcha.toolkit.model.VerifyResult;
import com.captcha.toolkit.render.SliderRenderer;
import com.captcha.toolkit.render.BackgroundProvider;
import com.captcha.toolkit.render.FallbackBackgroundProvider;
import com.captcha.toolkit.render.SceneBackgroundProvider;
import com.captcha.toolkit.shape.PuzzleShapeRegistry;
import com.captcha.toolkit.store.InMemoryCaptchaSessionStore;
import com.captcha.toolkit.word.WordFactory;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        config.getClick().setTargetText(List.of("星巴克", "麦当劳"));
        CaptchaEngine engine = CaptchaEngine.of(config,
                new InMemoryCaptchaSessionStore(), new DataUriImageCodec(),
                List.of(), new FallbackBackgroundProvider(List.of(new SceneBackgroundProvider())));

        CaptchaChallenge challenge = engine.create(CaptchaType.CLICK, Map.of(), true);
        // 每次从数组中随机选一个词组，并按词组内文字顺序提示
        assertTrue(challenge.getPrompt().equals(List.of("星", "巴", "克"))
                || challenge.getPrompt().equals(List.of("麦", "当", "劳")));
        assertEquals(3, challenge.getDebugTargets().size());

        List<PointVo> points = challenge.getDebugTargets().stream()
                .map(p -> new PointVo(p.getX(), p.getY()))
                .toList();
        VerifyResult ok = engine.verify(challenge.getId(), CaptchaAnswer.click(points));
        assertTrue(ok.isSuccess(), ok.getMessage());
    }

    @Test
    void clickUsesWordFactory() {
        CaptchaConfig config = new CaptchaConfig();
        config.setDebugEnabled(true);
        config.getClick().setMinElapsedMs(0);
        BackgroundProvider background = new FallbackBackgroundProvider(
                List.of(new SceneBackgroundProvider()));
        // 宿主可注入任意词组工厂（例如从数据库/远程接口动态取词组）
        WordFactory wordFactory = () -> List.of("星巴克", "麦当劳");
        CaptchaEngine engine = CaptchaEngine.of(config,
                new InMemoryCaptchaSessionStore(), new DataUriImageCodec(),
                List.of(), background, background, wordFactory);

        CaptchaChallenge challenge = engine.create(CaptchaType.CLICK, Map.of(), true);
        assertTrue(challenge.getPrompt().equals(List.of("星", "巴", "克"))
                || challenge.getPrompt().equals(List.of("麦", "当", "劳")));

        List<PointVo> points = challenge.getDebugTargets().stream()
                .map(p -> new PointVo(p.getX(), p.getY()))
                .toList();
        VerifyResult ok = engine.verify(challenge.getId(), CaptchaAnswer.click(points));
        assertTrue(ok.isSuccess(), ok.getMessage());
    }

    @Test
    void sliderFakeTargetsAvoidSameY() {
        CaptchaConfig config = new CaptchaConfig();
        config.getSlider().setFakeTargetCount(3);
        SliderRenderer renderer = new SliderRenderer(config.getSlider(),
                new FallbackBackgroundProvider(List.of(new SceneBackgroundProvider())),
                new PuzzleShapeRegistry());
        renderer.setShape("classic");
        renderer.run();

        List<Point> fakes = renderer.getFakeTargets();
        assertEquals(3, fakes.size());
        Set<Integer> ys = new HashSet<>();
        ys.add(renderer.getY());
        for (Point fake : fakes) {
            assertTrue(ys.add(fake.y),
                    "假目标与真目标或彼此位于同一 y 轴: " + fake.y);
        }
    }

    @Test
    void successReturnsOneTimeTicket() {
        CaptchaEngine engine = newEngine();
        CaptchaChallenge challenge = engine.create(CaptchaType.SLIDER,
                Map.of("shape", "classic"), true);

        VerifyResult ok = engine.verify(challenge.getId(),
                CaptchaAnswer.slider(challenge.getDebugX().doubleValue(), challenge.getWidth()));
        assertTrue(ok.isSuccess(), ok.getMessage());
        assertNotNull(ok.getTicket());

        // 票据一次性：第一次校验有效，第二次即失效
        assertTrue(engine.consumeTicket(ok.getTicket()).isSuccess());
        assertFalse(engine.consumeTicket(ok.getTicket()).isSuccess());
    }

    @Test
    void sliderRandomShapePicksRandomShape() {
        CaptchaEngine engine = newEngine();
        Set<String> shapes = new HashSet<>();
        for (int i = 0; i < 12; i++) {
            CaptchaChallenge challenge = engine.create(CaptchaType.SLIDER,
                    Map.of("shape", "random"), true);
            shapes.add(challenge.getShape());
        }
        assertTrue(shapes.size() > 1, "shape=random 应出现多种形状: " + shapes);
    }
}
