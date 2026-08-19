package com.captcha.toolkit;

import com.captcha.toolkit.generator.CaptchaGenerator;
import com.captcha.toolkit.generator.GenerateRequest;
import com.captcha.toolkit.generator.SliderCaptchaGenerator;
import com.captcha.toolkit.image.CaptchaImageCodec;
import com.captcha.toolkit.model.CaptchaAnswer;
import com.captcha.toolkit.model.CaptchaChallenge;
import com.captcha.toolkit.model.CaptchaException;
import com.captcha.toolkit.model.CaptchaSession;
import com.captcha.toolkit.model.GeneratedCaptcha;
import com.captcha.toolkit.model.VerifyResult;
import com.captcha.toolkit.render.BackgroundProvider;
import com.captcha.toolkit.store.CaptchaSessionStore;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 验证码引擎（门面模式）：对调用方屏蔽工厂、生成器、存储、编码细节。
 *
 * <p>既可以被 Spring 控制器调用，也可以被普通 Java 代码直接调用：
 * <pre>
 * CaptchaEngine engine = CaptchaEngine.of(config, store, codec, List.of(), backgroundProvider);
 * CaptchaChallenge challenge = engine.create(CaptchaType.SLIDER, Map.of("shape", "classic"), false);
 * VerifyResult result = engine.verify(challenge.getId(), "slider", CaptchaAnswer.slider(100.0, 340));
 * </pre>
 */
public class CaptchaEngine {

    private final Map<CaptchaType, CaptchaGenerator> generators;
    private final CaptchaSessionStore store;
    private final CaptchaImageCodec codec;
    private final boolean debugEnabled;

    public CaptchaEngine(List<CaptchaFactory> factories, CaptchaConfig config,
                         CaptchaSessionStore store, CaptchaImageCodec codec) {
        this(buildGenerators(factories, config), store, codec, config.isDebugEnabled());
    }

    /**
     * 推荐入口：用户自定义工厂优先，缺失的类型用内置工厂补齐。
     */
    public static CaptchaEngine of(CaptchaConfig config,
                                   CaptchaSessionStore store,
                                   CaptchaImageCodec codec,
                                   List<CaptchaFactory> userFactories,
                                   BackgroundProvider defaultBackgroundProvider) {
        Map<CaptchaType, CaptchaGenerator> map = new EnumMap<>(CaptchaType.class);
        if (userFactories != null) {
            for (CaptchaFactory factory : userFactories) {
                map.put(factory.type(), factory.create(config));
            }
        }
        map.putIfAbsent(CaptchaType.SLIDER,
                new SliderCaptchaFactory(defaultBackgroundProvider).create(config));
        map.putIfAbsent(CaptchaType.CLICK,
                new ClickCaptchaFactory(defaultBackgroundProvider).create(config));
        return new CaptchaEngine(map, store, codec, config.isDebugEnabled());
    }

    private CaptchaEngine(Map<CaptchaType, CaptchaGenerator> generators,
                          CaptchaSessionStore store,
                          CaptchaImageCodec codec,
                          boolean debugEnabled) {
        this.generators = generators;
        this.store = store;
        this.codec = codec;
        this.debugEnabled = debugEnabled;
    }

    private static Map<CaptchaType, CaptchaGenerator> buildGenerators(List<CaptchaFactory> factories,
                                                                      CaptchaConfig config) {
        Map<CaptchaType, CaptchaGenerator> map = new EnumMap<>(CaptchaType.class);
        if (factories != null) {
            for (CaptchaFactory factory : factories) {
                map.put(factory.type(), factory.create(config));
            }
        }
        map.putIfAbsent(CaptchaType.SLIDER, new SliderCaptchaFactory().create(config));
        map.putIfAbsent(CaptchaType.CLICK, new ClickCaptchaFactory().create(config));
        return map;
    }

    /**
     * 下发一张验证码。
     *
     * @param type   验证码类型
     * @param params 扩展参数，例如滑块 shape
     * @param debug  是否尝试附加答案（最终受 captcha.debug-enabled 控制）
     */
    public CaptchaChallenge create(CaptchaType type, Map<String, String> params, boolean debug) {
        CaptchaGenerator generator = generators.get(type);
        if (generator == null) {
            throw new CaptchaException("不支持的验证码类型: " + type);
        }
        GenerateRequest request = new GenerateRequest(UUID.randomUUID().toString(), params, debug);
        GeneratedCaptcha generated = generator.generate(request);
        store.put(generated.getSession());

        CaptchaChallenge challenge = new CaptchaChallenge();
        challenge.setId(generated.getSession().getId());
        challenge.setType(type.getCode());
        challenge.setImage1(codec.encode(generated.getImage1(), "png"));
        if (generated.getImage2() != null) {
            challenge.setImage2(codec.encode(generated.getImage2(), "png"));
        }
        challenge.setWidth(generated.getWidth());
        challenge.setHeight(generated.getHeight());
        challenge.setShape(generated.getShape());
        challenge.setPrompt(generated.getPrompt());
        challenge.setPieceOffsetX(generated.getPieceOffsetX());
        challenge.setMetadata(generated.getMetadata());

        if (debug && debugEnabled) {
            challenge.setDebugX(generated.getDebugX());
            challenge.setDebugTargets(generated.getDebugTargets());
        }
        return challenge;
    }

    /**
     * 校验答案。无论成功失败都会销毁会话（一次性使用）。
     */
    public VerifyResult verify(String id, CaptchaAnswer answer) {
        CaptchaSession session = store.get(id);
        if (session == null) {
            return VerifyResult.expired("验证码已过期，请刷新重试");
        }
        CaptchaGenerator generator = generators.get(session.getType());
        if (generator == null) {
            return VerifyResult.badRequest("不支持的验证码类型");
        }
        VerifyResult result = generator.verify(session, answer);
        store.remove(id);
        return result;
    }

    public List<String> supportedTypes() {
        return generators.keySet().stream()
                .map(CaptchaType::getCode)
                .sorted()
                .toList();
    }

    public List<String> supportedShapes() {
        CaptchaGenerator generator = generators.get(CaptchaType.SLIDER);
        if (generator instanceof SliderCaptchaGenerator slider) {
            return new ArrayList<>(slider.getShapeNames());
        }
        return List.of();
    }

    public boolean remove(String id) {
        if (id == null) {
            return false;
        }
        CaptchaSession session = store.get(id);
        if (session == null) {
            return false;
        }
        store.remove(id);
        return true;
    }
}
