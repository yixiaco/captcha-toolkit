package io.github.yixiaco;

import io.github.yixiaco.config.CaptchaConfig;
import io.github.yixiaco.exception.CaptchaException;
import io.github.yixiaco.image.DataUriImageCodec;
import io.github.yixiaco.model.CaptchaChallenge;
import io.github.yixiaco.render.FallbackBackgroundProvider;
import io.github.yixiaco.render.SceneBackgroundProvider;
import io.github.yixiaco.store.InMemoryCaptchaSessionStore;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 类型下发策略测试：非 debug 下客户端指定的类型必须被忽略，
 * 由后端从类型池（{@code captcha.types}）中决定；只有 debug 且引擎开启 debug-enabled
 * 时才允许客户端指定类型。
 */
class CaptchaTypeSelectionTest {

    /** 按给定类型池构建引擎 */
    private static CaptchaEngine engine(List<String> types, boolean debugEnabled) {
        CaptchaConfig config = new CaptchaConfig();
        config.setDebugEnabled(debugEnabled);
        config.setTypes(new ArrayList<>(types));
        return CaptchaEngine.of(config, new InMemoryCaptchaSessionStore(),
                new DataUriImageCodec(), List.of(),
                new FallbackBackgroundProvider(List.of(new SceneBackgroundProvider())));
    }

    /** 全部内置类型编码 */
    private static final List<String> ALL_TYPES = List.of("slider", "click", "click-shape",
            "rotate", "angle", "scratch", "curve", "slide-curve", "swing-tile");

    @Test
    void clientTypeIsIgnoredWithoutDebug() {
        // 类型池里没有 slider，客户端即使显式请求 slider 也只能拿到池内的类型
        CaptchaEngine engine = engine(List.of("click"), false);

        assertEquals("click", engine.createForClient("slider", Map.of(), false, null).getType());
        assertEquals("click", engine.createForClient("rotate", Map.of(), false, null).getType());
        assertEquals("click", engine.createForClient(null, Map.of(), false, null).getType());
    }

    @Test
    void debugRequestCannotPickTypeWhenDebugDisabled() throws Exception {
        CaptchaEngine engine = engine(List.of("click"), false);

        CaptchaChallenge<?> challenge = engine.createForClient("slider", Map.of(), true, null);

        assertEquals("click", challenge.getType());
        // debug 未生效时同样不能下发答案字段
        assertFalse(new com.fasterxml.jackson.databind.ObjectMapper()
                        .writeValueAsString(challenge).contains("\"debug"),
                "debug-enabled 关闭时不应下发答案字段");
    }

    @Test
    void clientTypeIsHonoredInDebug() {
        CaptchaEngine engine = engine(ALL_TYPES, true);

        assertEquals("rotate", engine.createForClient("rotate", Map.of(), true, null).getType());
        assertEquals("click-shape",
                engine.createForClient("CLICK-SHAPE", Map.of(), true, null).getType());
    }

    @Test
    void autoAndBlankTypeAreResolvedByServer() {
        CaptchaEngine engine = engine(List.of("rotate"), true);

        assertEquals("rotate", engine.createForClient("auto", Map.of(), true, null).getType());
        assertEquals("rotate", engine.createForClient("  ", Map.of(), true, null).getType());
    }

    @Test
    void serverPicksRandomlyInsideConfiguredPool() {
        List<String> pool = List.of("slider", "click", "rotate");
        CaptchaEngine engine = engine(pool, false);

        Set<String> picked = new HashSet<>();
        for (int i = 0; i < 30; i++) {
            String type = engine.createForClient(null, Map.of(), false, null).getType();
            assertTrue(pool.contains(type), "下发类型应落在类型池内，实际: " + type);
            picked.add(type);
        }
        assertTrue(picked.size() > 1, "多种类型时应随机下发，实际只出现: " + picked);
    }

    @Test
    void selectionIsReproducibleWithInjectedRandom() {
        // 同一个随机种子应得到同一串类型，证明选择逻辑使用注入的随机源
        assertEquals(pickWithSeed(2026L), pickWithSeed(2026L));
    }

    /** 用固定种子的随机源连续下发 8 次，返回类型序列 */
    private static List<String> pickWithSeed(long seed) {
        CaptchaConfig config = new CaptchaConfig();
        config.setTypes(new ArrayList<>(ALL_TYPES));
        config.setTypeRandom(new Random(seed));
        CaptchaEngine engine = CaptchaEngine.of(config, new InMemoryCaptchaSessionStore(),
                new DataUriImageCodec(), List.of(),
                new FallbackBackgroundProvider(List.of(new SceneBackgroundProvider())));
        List<String> picked = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            picked.add(engine.createForClient(null, Map.of(), false, null).getType());
        }
        return picked;
    }

    @Test
    void debugRequestOutsidePoolIsRejected() {
        CaptchaEngine engine = engine(List.of("slider"), true);

        assertThrows(IllegalArgumentException.class,
                () -> engine.createForClient("rotate", Map.of(), true, null));
    }

    @Test
    void unknownClientTypeIsRejectedInDebug() {
        CaptchaEngine engine = engine(ALL_TYPES, true);

        assertThrows(IllegalArgumentException.class,
                () -> engine.createForClient("nope", Map.of(), true, null));
    }

    @Test
    void unknownConfiguredTypeFailsFast() {
        CaptchaConfig config = new CaptchaConfig();
        config.setTypes(List.of("slider", "not-a-type"));

        assertThrows(CaptchaException.class, () -> CaptchaEngine.of(config,
                new InMemoryCaptchaSessionStore(), new DataUriImageCodec(), List.of(),
                new FallbackBackgroundProvider(List.of(new SceneBackgroundProvider()))));
    }

    @Test
    void supportedTypesAndShapesFollowTypePool() {
        CaptchaEngine engine = engine(List.of("rotate", "slider"), true);

        assertEquals(List.of("rotate", "slider"), engine.supportedTypes());
        assertFalse(engine.supportedShapes(true).get("slider").isEmpty(),
                "类型池包含 slider 时应返回形状列表");

        CaptchaEngine clickOnly = engine(List.of("click"), true);
        assertEquals(List.of("click"), clickOnly.supportedTypes());
        assertTrue(clickOnly.supportedShapes(true).get("slider").isEmpty(),
                "类型池不含 slider 时不应返回形状列表");
    }
}
