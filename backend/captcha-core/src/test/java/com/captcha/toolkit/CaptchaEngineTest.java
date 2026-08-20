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
        config.getRotate().setMinElapsedMs(0);
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
    void sliderFakeTargetsFollowPlacementRules() {
        CaptchaConfig config = new CaptchaConfig();
        // 小高度画布强制所有目标落在同一个 y 轴，专门验证同 y 约束
        config.getSlider().setHeight(44);
        config.getSlider().setFakeTargetCount(3);
        BackgroundProvider background = new FallbackBackgroundProvider(
                List.of(new SceneBackgroundProvider()));
        SliderRenderer renderer = new SliderRenderer(config.getSlider(),
                background, new PuzzleShapeRegistry());
        renderer.setShape("classic");
        renderer.run();

        int vwh = renderer.getPieceSize();
        int gap = config.getSlider().getFakeTargetMinGap();
        int threshold = config.getSlider().getFakeTargetAxisThreshold();
        var fakes = renderer.getFakeTargets();
        assertEquals(3, fakes.size());
        for (var fake : fakes) {
            assertEquals(renderer.getY(), fake.getY(), "假目标应与真目标同 y 轴");
            // 同 y：大小或旋转必须与真目标不同
            assertTrue(fake.getSize() != vwh || Math.abs(fake.getRotation()) >= 0.5,
                    "同 y 轴时大小或旋转必须不同");
            // 同 x 且同 y 不允许
            assertTrue(Math.abs(fake.getX() - renderer.getX()) >= threshold,
                    "同 x 且同 y 不允许");
            // 与真目标不能重叠
            assertTrue(Math.hypot(fake.getX() - renderer.getX(), fake.getY() - renderer.getY())
                            >= (vwh + fake.getSize()) / 2.0 + gap,
                    "与真目标不能重叠");
        }
        for (int i = 0; i < fakes.size(); i++) {
            for (int j = i + 1; j < fakes.size(); j++) {
                // 同 x 且同 y 不允许
                assertTrue(Math.abs(fakes.get(i).getX() - fakes.get(j).getX()) >= threshold
                                || Math.abs(fakes.get(i).getY() - fakes.get(j).getY()) >= threshold,
                        "同 x 且同 y 不允许");
                // 同 y：大小或旋转必须不同
                assertTrue(fakes.get(i).getSize() != fakes.get(j).getSize()
                                || Math.abs(fakes.get(i).getRotation() - fakes.get(j).getRotation()) >= 0.5,
                        "同 y 轴时大小或旋转必须不同");
                assertTrue(Math.hypot(fakes.get(i).getX() - fakes.get(j).getX(),
                                fakes.get(i).getY() - fakes.get(j).getY())
                                >= (fakes.get(i).getSize() + fakes.get(j).getSize()) / 2.0 + gap,
                        "假目标之间不能重叠");
            }
        }

        // 常规高度：不违反规则时，大小/旋转应与真目标一致
        CaptchaConfig normal = new CaptchaConfig();
        normal.getSlider().setFakeTargetCount(4);
        for (int round = 0; round < 20; round++) {
            SliderRenderer r2 = new SliderRenderer(normal.getSlider(), background,
                    new PuzzleShapeRegistry());
            r2.run();
            int v = r2.getPieceSize();
            var fs = r2.getFakeTargets();
            for (var fake : fs) {
                boolean sameY = Math.abs(fake.getY() - r2.getY()) < threshold;
                if (sameY) {
                    assertTrue(fake.getSize() != v || Math.abs(fake.getRotation()) >= 0.5,
                            "同 y 轴时大小或旋转必须不同");
                } else {
                    assertEquals(v, fake.getSize(), "不同 y 轴时大小应与真目标一致");
                    assertTrue(Math.abs(fake.getRotation()) < 0.5,
                            "不同 y 轴时旋转应与真目标一致");
                }
                if (Math.abs(fake.getX() - r2.getX()) < threshold) {
                    assertTrue(Math.abs(fake.getY() - r2.getY()) >= threshold,
                            "同 x 时 y 必须不同");
                }
                assertTrue(Math.hypot(fake.getX() - r2.getX(), fake.getY() - r2.getY())
                                >= (v + fake.getSize()) / 2.0 + gap,
                        "与真目标不能重叠");
            }
            for (int i = 0; i < fs.size(); i++) {
                for (int j = i + 1; j < fs.size(); j++) {
                    assertTrue(Math.abs(fs.get(i).getX() - fs.get(j).getX()) >= threshold
                                    || Math.abs(fs.get(i).getY() - fs.get(j).getY()) >= threshold,
                            "同 x 且同 y 不允许");
                    assertTrue(Math.hypot(fs.get(i).getX() - fs.get(j).getX(),
                                    fs.get(i).getY() - fs.get(j).getY())
                                    >= (fs.get(i).getSize() + fs.get(j).getSize()) / 2.0 + gap,
                            "假目标之间不能重叠");
                }
            }
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

    @Test
    void rotateGeneratesAndVerifies() {
        CaptchaEngine engine = newEngine();
        CaptchaChallenge challenge = engine.create(CaptchaType.ROTATE, Map.of(), true);

        assertNotNull(challenge.getImage1());
        assertNotNull(challenge.getImage2());
        assertNotNull(challenge.getDebugAngle());

        VerifyResult ok = engine.verify(challenge.getId(),
                CaptchaAnswer.rotate(challenge.getDebugAngle()));
        assertTrue(ok.isSuccess(), ok.getMessage());

        // 错误角度应失败
        CaptchaChallenge wrongChallenge = engine.create(CaptchaType.ROTATE, Map.of(), true);
        VerifyResult wrong = engine.verify(wrongChallenge.getId(),
                CaptchaAnswer.rotate(wrongChallenge.getDebugAngle() + 30));
        assertFalse(wrong.isSuccess());
    }
}
