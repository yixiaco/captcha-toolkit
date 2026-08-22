package com.captcha.toolkit;

import com.captcha.toolkit.config.CaptchaConfig;
import com.captcha.toolkit.exception.RateLimitExceededException;
import com.captcha.toolkit.image.CaptchaImageCodec;
import com.captcha.toolkit.image.DataUriImageCodec;
import com.captcha.toolkit.model.CaptchaAnswer;
import com.captcha.toolkit.model.CaptchaChallenge;
import com.captcha.toolkit.model.AngleChallengeData;
import com.captcha.toolkit.model.ClickChallengeData;
import com.captcha.toolkit.model.CurveChallengeData;
import com.captcha.toolkit.model.NormalizedPoint;
import com.captcha.toolkit.model.RotateChallengeData;
import com.captcha.toolkit.model.ScratchChallengeData;
import com.captcha.toolkit.model.SliderChallengeData;
import com.captcha.toolkit.model.SlideCurveChallengeData;
import com.captcha.toolkit.model.SwingTileChallengeData;
import com.captcha.toolkit.model.VerifyResult;
import com.captcha.toolkit.render.SliderRenderer;
import com.captcha.toolkit.render.BackgroundProvider;
import com.captcha.toolkit.render.FallbackBackgroundProvider;
import com.captcha.toolkit.render.SceneBackgroundProvider;
import com.captcha.toolkit.shape.PuzzleShapeRegistry;
import com.captcha.toolkit.store.InMemoryCaptchaSessionStore;
import com.captcha.toolkit.type.CaptchaType;
import com.captcha.toolkit.word.WordFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 引擎端到端测试：滑块与点选都走“生成 → 校验 → 一次性会话”的完整流程。
 * 测试环境把最短耗时置 0，避免真实等待。
 */
class CaptchaEngineTest {

    /** 最近一次引擎使用的会话存储，测试可通过会话读取服务端形状 */
    private InMemoryCaptchaSessionStore sessionStore;

    private CaptchaEngine newEngine() {
        CaptchaConfig config = new CaptchaConfig();
        config.setDebugEnabled(true);
        config.getSlider().setMinElapsedMs(0);
        config.getClick().setMinElapsedMs(0);
        config.getRotate().setMinElapsedMs(0);
        config.getAngle().setMinElapsedMs(0);
        config.getScratch().setMinElapsedMs(0);
        config.getCurve().setMinElapsedMs(0);
        config.getSlideCurve().setMinElapsedMs(0);
        config.getSwingTile().setMinElapsedMs(0);
        CaptchaImageCodec codec = new DataUriImageCodec();
        FallbackBackgroundProvider background = new FallbackBackgroundProvider(
                List.of(new SceneBackgroundProvider()));
        sessionStore = new InMemoryCaptchaSessionStore();
        return CaptchaEngine.of(config, sessionStore, codec,
                List.of(), background);
    }

    /** 开启设备限流的引擎 */
    private CaptchaEngine newRateLimitedEngine(int maxRequests, long windowSeconds) {
        CaptchaConfig config = new CaptchaConfig();
        config.setDebugEnabled(true);
        config.getSlider().setMinElapsedMs(0);
        config.getClick().setMinElapsedMs(0);
        config.getRotate().setMinElapsedMs(0);
        config.getAngle().setMinElapsedMs(0);
        config.getScratch().setMinElapsedMs(0);
        config.getCurve().setMinElapsedMs(0);
        config.getSlideCurve().setMinElapsedMs(0);
        config.getSwingTile().setMinElapsedMs(0);
        config.getRateLimit().setEnabled(true);
        config.getRateLimit().setMaxRequests(maxRequests);
        config.getRateLimit().setWindowSeconds(windowSeconds);
        return CaptchaEngine.of(config, new InMemoryCaptchaSessionStore(),
                new DataUriImageCodec(), List.of(),
                new FallbackBackgroundProvider(List.of(new SceneBackgroundProvider())));
    }

    @Test
    void rateLimitDisabledByDefaultAllowsRepeatedCreates() {
        CaptchaEngine engine = newEngine();
        for (int i = 0; i < 3; i++) {
            engine.create(CaptchaType.SLIDER, Map.of("shape", "classic"), true, "device-a");
        }
    }

    @Test
    void createRejectsHighFrequencyDevice() {
        CaptchaEngine engine = newRateLimitedEngine(2, 60);
        engine.create(CaptchaType.SLIDER, Map.of("shape", "classic"), true, "device-a");
        engine.create(CaptchaType.SLIDER, Map.of("shape", "classic"), true, "device-a");
        assertThrows(RateLimitExceededException.class,
                () -> engine.create(CaptchaType.SLIDER,
                        Map.of("shape", "classic"), true, "device-a"));
    }

    @Test
    void verifyRejectsHighFrequencyDeviceWithoutConsumingSession() {
        CaptchaEngine engine = newRateLimitedEngine(1, 60);

        // 第一次校验携带指纹，占用设备额度
        CaptchaChallenge first = engine.create(CaptchaType.SLIDER,
                Map.of("shape", "classic"), true);
        CaptchaAnswer firstAnswer = CaptchaAnswer.slider(
                sliderData(first).debugX() / (double) first.getWidth());
        firstAnswer.setDeviceFingerprint("device-a");
        assertTrue(engine.verify(first.getId(), firstAnswer).isSuccess());

        // 同设备第二次校验被限流
        CaptchaChallenge second = engine.create(CaptchaType.SLIDER,
                Map.of("shape", "classic"), true);
        CaptchaAnswer secondAnswer = CaptchaAnswer.slider(
                sliderData(second).debugX() / (double) second.getWidth());
        secondAnswer.setDeviceFingerprint("device-a");
        VerifyResult rejected = engine.verify(second.getId(), secondAnswer);
        assertFalse(rejected.isSuccess());
        assertEquals("RATE_LIMITED", rejected.getCode());

        // 会话未被销毁：不带指纹重新校验同一会话仍可成功
        CaptchaAnswer retry = CaptchaAnswer.slider(
                sliderData(second).debugX() / (double) second.getWidth());
        assertTrue(engine.verify(second.getId(), retry).isSuccess());
    }

    @Test
    void verifyAllowsDifferentDevicesIndependently() {
        CaptchaEngine engine = newRateLimitedEngine(1, 60);
        for (int i = 0; i < 2; i++) {
            CaptchaChallenge challenge = engine.create(CaptchaType.SLIDER,
                    Map.of("shape", "classic"), true);
            CaptchaAnswer answer = CaptchaAnswer.slider(
                    sliderData(challenge).debugX() / (double) challenge.getWidth());
            answer.setDeviceFingerprint("device-" + i);
            assertTrue(engine.verify(challenge.getId(), answer).isSuccess());
        }
    }

    @Test
    void missingFingerprintSkipsRateLimit() {
        CaptchaEngine engine = newRateLimitedEngine(1, 60);
        engine.create(CaptchaType.SLIDER, Map.of("shape", "classic"), true);
        engine.create(CaptchaType.SLIDER, Map.of("shape", "classic"), true);
    }

    @Test
    void sliderGeneratesAndVerifiesOnce() {
        CaptchaEngine engine = newEngine();
        CaptchaChallenge challenge = engine.create(CaptchaType.SLIDER,
                Map.of("shape", "classic"), true);

        assertNotNull(challenge.getImage1());
        assertNotNull(challenge.getImage2());
        assertNotNull(sliderData(challenge).debugX());
        assertEquals("slider", challenge.getType());

        VerifyResult ok = engine.verify(challenge.getId(),
                CaptchaAnswer.slider(sliderData(challenge).debugX() / (double) challenge.getWidth()));
        assertTrue(ok.isSuccess(), ok.getMessage());

        // 会话一次性：重复提交必须失败
        VerifyResult again = engine.verify(challenge.getId(),
                CaptchaAnswer.slider(sliderData(challenge).debugX() / (double) challenge.getWidth()));
        assertFalse(again.isSuccess());
    }

    @Test
    void sliderAcceptsNormalizedAnswer() {
        CaptchaEngine engine = newEngine();
        CaptchaChallenge challenge = engine.create(CaptchaType.SLIDER,
                Map.of("shape", "classic"), true);

        // 归一化答案与渲染尺寸无关：直接按服务端图片宽度归一化
        double xNorm = sliderData(challenge).debugX() / (double) challenge.getWidth();
        VerifyResult ok = engine.verify(challenge.getId(), CaptchaAnswer.slider(xNorm));
        assertTrue(ok.isSuccess(), ok.getMessage());
    }

    @Test
    void clickGeneratesAndVerifiesInOrder() {
        CaptchaEngine engine = newEngine();
        CaptchaChallenge challenge = engine.create(CaptchaType.CLICK, Map.of(), true);

        assertNotNull(challenge.getImage1());
        // 提示词以单张透明背景图片下发
        assertNotNull(clickData(challenge).promptImage());
        assertTrue(clickData(challenge).promptImage().startsWith("data:image/png;base64,"));
        assertEquals(3, clickData(challenge).targetCount());
        assertNotNull(clickData(challenge).debugTargets());

        List<NormalizedPoint> points = clickData(challenge).debugTargets().stream()
                .map(p -> new NormalizedPoint(
                        p.getX() / (double) challenge.getWidth(),
                        p.getY() / (double) challenge.getHeight()))
                .toList();
        VerifyResult ok = engine.verify(challenge.getId(), CaptchaAnswer.click(points));
        assertTrue(ok.isSuccess(), ok.getMessage());
    }

    @Test
    void clickAcceptsNormalizedPoints() {
        CaptchaEngine engine = newEngine();
        CaptchaChallenge challenge = engine.create(CaptchaType.CLICK, Map.of(), true);

        List<NormalizedPoint> points = clickData(challenge).debugTargets().stream()
                .map(p -> new NormalizedPoint(
                        p.getX() / (double) challenge.getWidth(),
                        p.getY() / (double) challenge.getHeight()))
                .toList();
        VerifyResult ok = engine.verify(challenge.getId(), CaptchaAnswer.click(points));
        assertTrue(ok.isSuccess(), ok.getMessage());
    }

    @Test
    void clickRejectsWrongPoint() {
        CaptchaEngine engine = newEngine();
        CaptchaChallenge challenge = engine.create(CaptchaType.CLICK, Map.of(), true);

        List<NormalizedPoint> points = clickData(challenge).debugTargets().stream()
                .map(p -> new NormalizedPoint(
                        (p.getX() + 50) / (double) challenge.getWidth(),
                        (p.getY() + 50) / (double) challenge.getHeight()))
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
        // 每次从数组中随机选一个词组，提示词以单张图片下发
        assertNotNull(clickData(challenge).promptImage());
        assertEquals(3, clickData(challenge).targetCount());
        assertEquals(3, clickData(challenge).debugTargets().size());

        List<NormalizedPoint> points = clickData(challenge).debugTargets().stream()
                .map(p -> new NormalizedPoint(
                        p.getX() / (double) challenge.getWidth(),
                        p.getY() / (double) challenge.getHeight()))
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
        assertNotNull(clickData(challenge).promptImage());
        assertEquals(3, clickData(challenge).targetCount());

        List<NormalizedPoint> points = clickData(challenge).debugTargets().stream()
                .map(p -> new NormalizedPoint(
                        p.getX() / (double) challenge.getWidth(),
                        p.getY() / (double) challenge.getHeight()))
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
                CaptchaAnswer.slider(sliderData(challenge).debugX() / (double) challenge.getWidth()));
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
            shapes.add(sliderShape(challenge));
        }
        assertTrue(shapes.size() > 1, "shape=random 应出现多种形状: " + shapes);
    }

    @Test
    void sliderShapeRequiresBothDebugModes() {
        // 前后端都 debug：允许显式指定形状
        CaptchaEngine debugEngine = newEngine();
        CaptchaChallenge explicit = debugEngine.create(CaptchaType.SLIDER,
                Map.of("shape", "classic"), true);
        assertEquals("classic", sliderShape(explicit));

        // 仅前端 debug、后端未开启 debug：形状由后端随机决定，忽略前端指定
        CaptchaConfig config = new CaptchaConfig();
        config.setDebugEnabled(false);
        config.getSlider().setMinElapsedMs(0);
        BackgroundProvider background = new FallbackBackgroundProvider(
                List.of(new SceneBackgroundProvider()));
        InMemoryCaptchaSessionStore nonDebugStore = new InMemoryCaptchaSessionStore();
        CaptchaEngine nonDebugEngine = CaptchaEngine.of(config,
                nonDebugStore, new DataUriImageCodec(),
                List.of(), background);
        Set<String> shapes = new HashSet<>();
        for (int i = 0; i < 12; i++) {
            CaptchaChallenge challenge = nonDebugEngine.create(CaptchaType.SLIDER,
                    Map.of("shape", "classic"), true);
            shapes.add(sliderShape(nonDebugStore, challenge));
        }
        assertTrue(shapes.size() > 1,
                "后端非 debug 时应忽略前端指定形状: " + shapes);
    }

    @Test
    void disabledDebugNeverExposesAnswerFieldsEvenWhenRequested() {
        // 引擎关闭 debug-enabled 时，即使调用方传 debug=true，
        // 所有类型都不能返回答案/提示字段
        CaptchaConfig config = new CaptchaConfig();
        config.setDebugEnabled(false);
        config.getSlider().setMinElapsedMs(0);
        config.getClick().setMinElapsedMs(0);
        config.getRotate().setMinElapsedMs(0);
        config.getAngle().setMinElapsedMs(0);
        config.getScratch().setMinElapsedMs(0);
        config.getCurve().setMinElapsedMs(0);
        config.getSlideCurve().setMinElapsedMs(0);
        config.getSwingTile().setMinElapsedMs(0);
        CaptchaEngine engine = CaptchaEngine.of(config, new InMemoryCaptchaSessionStore(),
                new DataUriImageCodec(), List.of(),
                new FallbackBackgroundProvider(List.of(new SceneBackgroundProvider())));

        CaptchaChallenge<?> slider = engine.create(CaptchaType.SLIDER, Map.of(), true);
        assertNull(sliderData(slider).debugX());
        assertNull(sliderData(slider).debugFakeTargets());

        CaptchaChallenge<?> click = engine.create(CaptchaType.CLICK, Map.of(), true);
        assertNull(clickData(click).debugTargets());
        assertNull(clickData(click).debugFakeTargets());

        CaptchaChallenge<?> rotate = engine.create(CaptchaType.ROTATE, Map.of(), true);
        assertNull(rotateData(rotate).debugAngle());

        CaptchaChallenge<?> angle = engine.create(CaptchaType.ANGLE, Map.of(), true);
        assertNull(angleData(angle).debugAngle());

        CaptchaChallenge<?> scratch = engine.create(CaptchaType.SCRATCH, Map.of(), true);
        assertNull(scratchData(scratch).debugX());
        assertNull(scratchData(scratch).debugTargets());
        assertNull(scratchData(scratch).debugPatterns());

        CaptchaChallenge<?> curve = engine.create(CaptchaType.CURVE, Map.of(), true);
        assertNull(curveData(curve).debugCurve());

        CaptchaChallenge<?> slideCurve = engine.create(CaptchaType.SLIDE_CURVE, Map.of(), true);
        assertNull(slideCurveData(slideCurve).debugSwing());
        assertNull(slideCurveData(slideCurve).debugFakeTargets());

        CaptchaChallenge<?> swingTile = engine.create(CaptchaType.SWING_TILE, Map.of(), true);
        assertNull(swingTileData(swingTile).debugT());
        assertNull(swingTileData(swingTile).debugFakeTargets());
    }

    @Test
    void nonDebugRequestNeverExposesAnswerFields() {
        // 前端未传 debug=1：即使引擎开启了 debug-enabled，也不返回答案字段
        CaptchaEngine engine = newEngine();

        CaptchaChallenge<?> slider = engine.create(CaptchaType.SLIDER, Map.of(), false);
        assertNull(sliderData(slider).debugX());

        CaptchaChallenge<?> click = engine.create(CaptchaType.CLICK, Map.of(), false);
        assertNull(clickData(click).debugTargets());

        CaptchaChallenge<?> rotate = engine.create(CaptchaType.ROTATE, Map.of(), false);
        assertNull(rotateData(rotate).debugAngle());

        CaptchaChallenge<?> angle = engine.create(CaptchaType.ANGLE, Map.of(), false);
        assertNull(angleData(angle).debugAngle());

        CaptchaChallenge<?> scratch = engine.create(CaptchaType.SCRATCH, Map.of(), false);
        assertNull(scratchData(scratch).debugX());

        CaptchaChallenge<?> curve = engine.create(CaptchaType.CURVE, Map.of(), false);
        assertNull(curveData(curve).debugCurve());

        CaptchaChallenge<?> slideCurve = engine.create(CaptchaType.SLIDE_CURVE, Map.of(), false);
        assertNull(slideCurveData(slideCurve).debugSwing());

        CaptchaChallenge<?> swingTile = engine.create(CaptchaType.SWING_TILE, Map.of(), false);
        assertNull(swingTileData(swingTile).debugT());
    }

    @Test
    void nonDebugResponseJsonOmitsNullDebugFields() throws Exception {
        // 非 debug 下发序列化后不应出现任何 debug 键（空值也不能保留）
        CaptchaEngine engine = newEngine();
        ObjectMapper mapper = new ObjectMapper();
        List<CaptchaType> types = List.of(CaptchaType.SLIDER, CaptchaType.CLICK,
                CaptchaType.ROTATE, CaptchaType.ANGLE, CaptchaType.SCRATCH,
                CaptchaType.CURVE, CaptchaType.SLIDE_CURVE, CaptchaType.SWING_TILE);
        for (CaptchaType type : types) {
            CaptchaChallenge<?> challenge = engine.create(type, Map.of(), false);
            String json = mapper.writeValueAsString(challenge);
            assertFalse(json.contains("\"debug"), type + " 非 debug 响应不应包含 debug 键: " + json);
        }
    }

    @Test
    void rotateGeneratesAndVerifies() {
        CaptchaEngine engine = newEngine();
        CaptchaChallenge challenge = engine.create(CaptchaType.ROTATE, Map.of(), true);

        assertNotNull(challenge.getImage1());
        assertNotNull(challenge.getImage2());
        assertNotNull(rotateData(challenge).debugAngle());

        VerifyResult ok = engine.verify(challenge.getId(),
                CaptchaAnswer.rotate(rotateData(challenge).debugAngle()));
        assertTrue(ok.isSuccess(), ok.getMessage());

        // 错误角度应失败
        CaptchaChallenge wrongChallenge = engine.create(CaptchaType.ROTATE, Map.of(), true);
        VerifyResult wrong = engine.verify(wrongChallenge.getId(),
                CaptchaAnswer.rotate(rotateData(wrongChallenge).debugAngle() + 30));
        assertFalse(wrong.isSuccess());
    }

    @Test
    void angleGeneratesAndVerifies() {
        CaptchaEngine engine = newEngine();
        CaptchaChallenge challenge = engine.create(CaptchaType.ANGLE, Map.of(), true);

        assertEquals(null, challenge.getImage1(), "角度验证不再下发背景图");
        assertNotNull(challenge.getImage2());
        assertNotNull(angleData(challenge).debugAngle());
        assertNotNull(angleData(challenge).discSize());
        assertEquals("angle", challenge.getType());

        VerifyResult ok = engine.verify(challenge.getId(),
                CaptchaAnswer.rotate(angleData(challenge).debugAngle()));
        assertTrue(ok.isSuccess(), ok.getMessage());

        // 错误角度应失败
        CaptchaChallenge wrongChallenge = engine.create(CaptchaType.ANGLE, Map.of(), true);
        VerifyResult wrong = engine.verify(wrongChallenge.getId(),
                CaptchaAnswer.rotate(angleData(wrongChallenge).debugAngle() + 30));
        assertFalse(wrong.isSuccess());
    }

    @Test
    void scratchGeneratesAndVerifiesOnlyTargetShapes() {
        CaptchaEngine engine = newEngine();
        CaptchaChallenge challenge = engine.create(CaptchaType.SCRATCH, Map.of(), true);

        assertNotNull(challenge.getImage1());
        assertEquals("scratch", challenge.getType());
        ScratchChallengeData data = scratchData(challenge);
        assertNotNull(data.debugX());
        // 目标数量随机（1~3），且与 debugTargets 一致
        assertTrue(data.targetCount() >= 1 && data.targetCount() <= 3);
        assertEquals(data.targetCount(), data.debugTargets().size());
        assertEquals(6, data.debugPatterns().size());

        // 滑块停在答案位置 → 通过
        VerifyResult ok = engine.verify(challenge.getId(),
                CaptchaAnswer.slider(data.debugX()));
        assertTrue(ok.isSuccess(), ok.getMessage());

        // 停早了（提示图形未出全）→ 失败
        CaptchaChallenge earlyChallenge = engine.create(CaptchaType.SCRATCH, Map.of(), true);
        VerifyResult early = engine.verify(earlyChallenge.getId(),
                CaptchaAnswer.slider(scratchData(earlyChallenge).debugX() - 0.1));
        assertFalse(early.isSuccess());

        // 停晚了（目标出现后继续右移）→ 失败
        CaptchaChallenge lateChallenge = engine.create(CaptchaType.SCRATCH, Map.of(), true);
        VerifyResult wrong = engine.verify(lateChallenge.getId(),
                CaptchaAnswer.slider(scratchData(lateChallenge).debugX() + 0.1));
        assertFalse(wrong.isSuccess());
    }

    @Test
    void curveGeneratesAndVerifiesWithDebugCurve() {
        CaptchaEngine engine = newEngine();
        CaptchaChallenge challenge = engine.create(CaptchaType.CURVE, Map.of(), true);

        assertNotNull(challenge.getImage1());
        assertNotNull(curveData(challenge).debugCurve());
        assertEquals("curve", challenge.getType());

        List<NormalizedPoint> curve = curveData(challenge).debugCurve().stream()
                .map(p -> new NormalizedPoint(
                        p.getX() / (double) challenge.getWidth(),
                        p.getY() / (double) challenge.getHeight()))
                .toList();
        VerifyResult ok = engine.verify(challenge.getId(), CaptchaAnswer.curve(curve));
        assertTrue(ok.isSuccess(), ok.getMessage());

        // 会话一次性：同一 id 再次提交失败
        VerifyResult again = engine.verify(challenge.getId(), CaptchaAnswer.curve(curve));
        assertFalse(again.isSuccess());
    }

    @Test
    void curveRejectsWrongEndPoint() {
        CaptchaEngine engine = newEngine();
        CaptchaChallenge challenge = engine.create(CaptchaType.CURVE, Map.of(), true);
        List<NormalizedPoint> curve = curveData(challenge).debugCurve().stream()
                .map(p -> new NormalizedPoint(
                        p.getX() / (double) challenge.getWidth(),
                        p.getY() / (double) challenge.getHeight()))
                .toList();
        List<NormalizedPoint> wrong = new java.util.ArrayList<>(curve);
        wrong.set(wrong.size() - 1, new NormalizedPoint(0.05, 0.05));

        VerifyResult result = engine.verify(challenge.getId(), CaptchaAnswer.curve(wrong));
        assertFalse(result.isSuccess());
        assertEquals("WRONG", result.getCode());
    }

    @Test
    void slideCurveGeneratesAndVerifiesWithDebugX() {
        CaptchaEngine engine = newEngine();
        CaptchaChallenge challenge = engine.create(CaptchaType.SLIDE_CURVE, Map.of(), true);

        assertNotNull(challenge.getImage1());
        assertNotNull(slideCurveData(challenge).debugSwing());
        assertEquals("slide-curve", challenge.getType());

        double xNorm = slideCurveData(challenge).debugSwing();
        VerifyResult ok = engine.verify(challenge.getId(), CaptchaAnswer.slider(xNorm));
        assertTrue(ok.isSuccess(), ok.getMessage());

        // 会话一次性：同一 id 再次提交失败
        VerifyResult again = engine.verify(challenge.getId(), CaptchaAnswer.slider(xNorm));
        assertFalse(again.isSuccess());
    }

    @Test
    void slideCurveRejectsWrongDisplacement() {
        CaptchaEngine engine = newEngine();
        CaptchaChallenge challenge = engine.create(CaptchaType.SLIDE_CURVE, Map.of(), true);
        double wrongX = slideCurveData(challenge).debugSwing() + 0.2;

        VerifyResult result = engine.verify(challenge.getId(), CaptchaAnswer.slider(wrongX));
        assertFalse(result.isSuccess());
        assertEquals("WRONG", result.getCode());
    }

    @Test
    void swingTileGeneratesAndVerifiesAtAnswerPosition() {
        CaptchaEngine engine = newEngine();
        CaptchaChallenge challenge = engine.create(CaptchaType.SWING_TILE, Map.of(), true);

        assertNotNull(challenge.getImage1());
        assertNotNull(challenge.getImage2());
        assertEquals("swing-tile", challenge.getType());
        assertEquals(4, swingTileData(challenge).path().size());

        VerifyResult ok = engine.verify(challenge.getId(),
                CaptchaAnswer.slider(swingTileData(challenge).debugT()));
        assertTrue(ok.isSuccess(), ok.getMessage());

        // 会话一次性：同一 id 再次提交失败
        VerifyResult again = engine.verify(challenge.getId(),
                CaptchaAnswer.slider(swingTileData(challenge).debugT()));
        assertFalse(again.isSuccess());
    }

    @Test
    void swingTileRejectsWrongPosition() {
        CaptchaEngine engine = newEngine();
        CaptchaChallenge challenge = engine.create(CaptchaType.SWING_TILE, Map.of(), true);

        VerifyResult result = engine.verify(challenge.getId(),
                CaptchaAnswer.slider(swingTileData(challenge).debugT() + 0.2));
        assertFalse(result.isSuccess());
        assertEquals("WRONG", result.getCode());
    }

    /** 读取滑块类型特定化载荷 */
    private static SliderChallengeData sliderData(CaptchaChallenge<?> challenge) {
        return (SliderChallengeData) challenge.getData();
    }

    /** 读取服务端会话中保存的拼图形状（不下发给前端） */
    private String sliderShape(CaptchaChallenge<?> challenge) {
        return sliderShape(sessionStore, challenge);
    }

    /** 从指定会话存储读取拼图形状（服务端内部信息，不通过下发载荷暴露） */
    private static String sliderShape(InMemoryCaptchaSessionStore store,
                                      CaptchaChallenge<?> challenge) {
        return store.get(challenge.getId()).getShape();
    }

    /** 读取点选类型特定化载荷 */
    private static ClickChallengeData clickData(CaptchaChallenge<?> challenge) {
        return (ClickChallengeData) challenge.getData();
    }

    /** 读取旋转类型特定化载荷 */
    private static RotateChallengeData rotateData(CaptchaChallenge<?> challenge) {
        return (RotateChallengeData) challenge.getData();
    }

    /** 读取角度验证类型特定化载荷 */
    private static AngleChallengeData angleData(CaptchaChallenge<?> challenge) {
        return (AngleChallengeData) challenge.getData();
    }

    /** 读取刮刮乐类型特定化载荷 */
    private static ScratchChallengeData scratchData(CaptchaChallenge<?> challenge) {
        return (ScratchChallengeData) challenge.getData();
    }

    /** 读取曲线类型特定化载荷 */
    private static CurveChallengeData curveData(CaptchaChallenge<?> challenge) {
        return (CurveChallengeData) challenge.getData();
    }

    /** 读取滑动曲线类型特定化载荷 */
    private static SlideCurveChallengeData slideCurveData(CaptchaChallenge<?> challenge) {
        return (SlideCurveChallengeData) challenge.getData();
    }

    /** 读取滑块摆动图块类型特定化载荷 */
    private static SwingTileChallengeData swingTileData(CaptchaChallenge<?> challenge) {
        return (SwingTileChallengeData) challenge.getData();
    }
}
