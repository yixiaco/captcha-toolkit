package com.captcha.toolkit;

import com.captcha.toolkit.generator.CaptchaGenerator;
import com.captcha.toolkit.generator.GenerateRequest;
import com.captcha.toolkit.generator.SliderCaptchaGenerator;
import com.captcha.toolkit.image.CaptchaImageCodec;
import com.captcha.toolkit.config.CaptchaConfig;
import com.captcha.toolkit.config.RateLimitConfig;
import com.captcha.toolkit.exception.RateLimitExceededException;
import com.captcha.toolkit.i18n.CaptchaMessages;
import com.captcha.toolkit.i18n.MessageProvider;
import com.captcha.toolkit.limit.DeviceRequestLimiter;
import com.captcha.toolkit.limit.InMemoryDeviceRequestLimiter;
import com.captcha.toolkit.model.CaptchaAnswer;
import com.captcha.toolkit.model.CaptchaChallenge;
import com.captcha.toolkit.exception.CaptchaException;
import com.captcha.toolkit.model.CaptchaSession;
import com.captcha.toolkit.model.CaptchaTicket;
import com.captcha.toolkit.model.GeneratedCaptcha;
import com.captcha.toolkit.model.VerifyResult;
import com.captcha.toolkit.render.BackgroundProvider;
import com.captcha.toolkit.store.CaptchaSessionStore;
import com.captcha.toolkit.store.CaptchaTicketStore;
import com.captcha.toolkit.store.InMemoryCaptchaTicketStore;
import com.captcha.toolkit.util.FingerprintHasher;
import com.captcha.toolkit.word.WordFactory;

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
 * VerifyResult result = engine.verify(challenge.getId(),
 *         CaptchaAnswer.slider(100.0 / challenge.getWidth()));
 * </pre>
 */
public class CaptchaEngine {

    /** 验证码类型 → 生成器映射 */
    private final Map<CaptchaType, CaptchaGenerator> generators;

    /** 验证码会话存储 */
    private final CaptchaSessionStore store;

    /** 验证通过后的票据存储 */
    private final CaptchaTicketStore ticketStore;

    /** 图片编码器（输出 data URI 等格式） */
    private final CaptchaImageCodec codec;

    /** 是否允许 debug 模式返回答案（受配置控制） */
    private final boolean debugEnabled;

    /** 票据有效期（毫秒） */
    private final long ticketTtlMillis;

    /** 用户提示消息提供者（多语言资源加载） */
    private final MessageProvider messages;

    /** 设备维度限流器（未开启限流时不会被调用） */
    private final DeviceRequestLimiter rateLimiter;

    /** 设备维度限流是否开启 */
    private final boolean deviceRateLimitEnabled;

    /** 设备指纹脱敏盐 */
    private final String fingerprintSalt;

    /**
     * 使用默认内存票据存储构造引擎。
     */
    public CaptchaEngine(List<CaptchaFactory> factories, CaptchaConfig config,
                         CaptchaSessionStore store, CaptchaImageCodec codec) {
        this(factories, config, store, new InMemoryCaptchaTicketStore(), codec);
    }

    /**
     * 完整构造：自定义工厂优先，缺失类型用内置工厂补齐。
     */
    public CaptchaEngine(List<CaptchaFactory> factories, CaptchaConfig config,
                         CaptchaSessionStore store, CaptchaTicketStore ticketStore,
                         CaptchaImageCodec codec) {
        this(buildGenerators(factories, config), store, ticketStore, codec,
                config.isDebugEnabled(), config.getTicketExpireSeconds() * 1000,
                config.getMessageProvider(), effectiveRateLimiter(config),
                config.getRateLimit().isEnabled(), config.getRateLimit().getFingerprintSalt());
    }

    /**
     * 推荐入口：用户自定义工厂优先，缺失的类型用内置工厂补齐。
     */
    public static CaptchaEngine of(CaptchaConfig config,
                                   CaptchaSessionStore store,
                                   CaptchaImageCodec codec,
                                   List<CaptchaFactory> userFactories,
                                   BackgroundProvider defaultBackgroundProvider) {
        return of(config, store, codec, userFactories,
                defaultBackgroundProvider, defaultBackgroundProvider);
    }

    /**
     * 分类型指定背景策略：滑块与点选可以使用不同的背景来源。
     */
    public static CaptchaEngine of(CaptchaConfig config,
                                   CaptchaSessionStore store,
                                   CaptchaImageCodec codec,
                                   List<CaptchaFactory> userFactories,
                                   BackgroundProvider sliderBackgroundProvider,
                                   BackgroundProvider clickBackgroundProvider) {
        return of(config, store, codec, userFactories,
                sliderBackgroundProvider, clickBackgroundProvider, null);
    }

    /**
     * 分类型指定背景策略，并允许注入词组工厂（点选目标词组来源）。
     */
    public static CaptchaEngine of(CaptchaConfig config,
                                   CaptchaSessionStore store,
                                   CaptchaImageCodec codec,
                                   List<CaptchaFactory> userFactories,
                                   BackgroundProvider sliderBackgroundProvider,
                                   BackgroundProvider clickBackgroundProvider,
                                   WordFactory wordFactory) {
        return of(config, store, codec, userFactories,
                sliderBackgroundProvider, clickBackgroundProvider, wordFactory,
                new InMemoryCaptchaTicketStore());
    }

    /**
     * 完整入口：分类型背景 + 词组工厂 + 票据存储。
     */
    public static CaptchaEngine of(CaptchaConfig config,
                                   CaptchaSessionStore store,
                                   CaptchaImageCodec codec,
                                   List<CaptchaFactory> userFactories,
                                   BackgroundProvider sliderBackgroundProvider,
                                   BackgroundProvider clickBackgroundProvider,
                                   WordFactory wordFactory,
                                   CaptchaTicketStore ticketStore) {
        Map<CaptchaType, CaptchaGenerator> map = new EnumMap<>(CaptchaType.class);
        if (userFactories != null) {
            for (CaptchaFactory factory : userFactories) {
                map.put(factory.type(), factory.create(config));
            }
        }
        map.putIfAbsent(CaptchaType.SLIDER,
                new SliderCaptchaFactory(sliderBackgroundProvider).create(config));
        map.putIfAbsent(CaptchaType.CLICK,
                new ClickCaptchaFactory(clickBackgroundProvider, wordFactory).create(config));
        map.putIfAbsent(CaptchaType.ROTATE,
                new RotateCaptchaFactory(sliderBackgroundProvider).create(config));
        map.putIfAbsent(CaptchaType.CURVE,
                new CurveCaptchaFactory(sliderBackgroundProvider).create(config));
        return new CaptchaEngine(map, store, ticketStore, codec,
                config.isDebugEnabled(), config.getTicketExpireSeconds() * 1000,
                config.getMessageProvider(), effectiveRateLimiter(config),
                config.getRateLimit().isEnabled(), config.getRateLimit().getFingerprintSalt());
    }

    /** 私有构造：统一接收已组装好的生成器映射与依赖 */
    private CaptchaEngine(Map<CaptchaType, CaptchaGenerator> generators,
                          CaptchaSessionStore store,
                          CaptchaTicketStore ticketStore,
                          CaptchaImageCodec codec,
                          boolean debugEnabled,
                          long ticketTtlMillis,
                          MessageProvider messages,
                          DeviceRequestLimiter rateLimiter,
                          boolean deviceRateLimitEnabled,
                          String fingerprintSalt) {
        this.generators = generators;
        this.store = store;
        this.ticketStore = ticketStore;
        this.codec = codec;
        this.debugEnabled = debugEnabled;
        this.ticketTtlMillis = ticketTtlMillis;
        this.messages = messages;
        this.rateLimiter = rateLimiter;
        this.deviceRateLimitEnabled = deviceRateLimitEnabled;
        this.fingerprintSalt = fingerprintSalt;
    }

    /** 使用配置的限流器，未配置时按 rateLimit 创建内存实现 */
    private static DeviceRequestLimiter effectiveRateLimiter(CaptchaConfig config) {
        return config.getDeviceRequestLimiter() != null
                ? config.getDeviceRequestLimiter()
                : new InMemoryDeviceRequestLimiter(config.getRateLimit());
    }

    /** 构建生成器映射：用户工厂优先，缺失类型用内置工厂补齐 */
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
        map.putIfAbsent(CaptchaType.ROTATE, new RotateCaptchaFactory().create(config));
        map.putIfAbsent(CaptchaType.CURVE, new CurveCaptchaFactory().create(config));
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
        return create(type, params, debug, null);
    }

    /**
     * 下发一张验证码（带设备指纹，开启限流时按设备计数）。
     *
     * @param type               验证码类型
     * @param params             扩展参数，例如滑块 shape
     * @param debug              是否尝试附加答案（最终受 captcha.debug-enabled 控制）
     * @param deviceFingerprint  设备指纹（可为 null；限流开启且指纹缺失时不计数）
     */
    public CaptchaChallenge create(CaptchaType type, Map<String, String> params, boolean debug,
                                   String deviceFingerprint) {
        enforceDeviceRateLimit(deviceFingerprint);
        CaptchaGenerator generator = generators.get(type);
        if (generator == null) {
            throw new CaptchaException("不支持的验证码类型: " + type);
        }
        // 拼图形状只在前后端都处于 debug 模式时允许显式指定，否则由后端随机决定
        Map<String, String> effectiveParams = params == null
                ? new java.util.LinkedHashMap<>()
                : new java.util.LinkedHashMap<>(params);
        if (!(debug && debugEnabled)) {
            effectiveParams.remove("shape");
        }
        GenerateRequest request = new GenerateRequest(
                UUID.randomUUID().toString(), effectiveParams, debug, deviceFingerprint);
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
            challenge.setDebugFakeTargets(generated.getDebugFakeTargets());
            challenge.setDebugAngle(generated.getDebugAngle());
            challenge.setDebugCurve(generated.getDebugCurve());
        }
        return challenge;
    }

    /**
     * 校验答案。无论成功失败都会销毁会话（一次性使用）。
     */
    public VerifyResult verify(String id, CaptchaAnswer answer) {
        CaptchaSession session = store.get(id);
        if (session == null) {
            return VerifyResult.expired(CaptchaMessages.VERIFY_EXPIRED, messages);
        }
        if (!isDeviceAllowed(answer)) {
            return VerifyResult.fail(CaptchaMessages.RATE_LIMIT_EXCEEDED, "RATE_LIMITED", messages);
        }
        CaptchaGenerator generator = generators.get(session.getType());
        if (generator == null) {
            return VerifyResult.badRequest(CaptchaMessages.VERIFY_UNSUPPORTED_TYPE, messages);
        }
        VerifyResult result = generator.verify(session, answer);
        store.remove(id);
        if (result.isSuccess()) {
            String ticket = UUID.randomUUID().toString();
            ticketStore.put(new CaptchaTicket(ticket, session.getType(), ticketTtlMillis));
            result.setTicket(ticket);
        }
        return result;
    }

    /** 下发验证码前的设备限流检查：超限抛出异常，由 HTTP 层转换为 RATE_LIMITED */
    private void enforceDeviceRateLimit(String deviceFingerprint) {
        if (!deviceRateLimitEnabled || deviceFingerprint == null || deviceFingerprint.isBlank()) {
            return;
        }
        if (!rateLimiter.allow(FingerprintHasher.hash(deviceFingerprint, fingerprintSalt))) {
            throw new RateLimitExceededException();
        }
    }

    /** 校验阶段的设备限流检查：超限返回失败结果，且不销毁会话 */
    private boolean isDeviceAllowed(CaptchaAnswer answer) {
        if (!deviceRateLimitEnabled || answer == null
                || answer.getDeviceFingerprint() == null
                || answer.getDeviceFingerprint().isBlank()) {
            return true;
        }
        return rateLimiter.allow(FingerprintHasher.hash(
                answer.getDeviceFingerprint(), fingerprintSalt));
    }

    /**
     * 校验业务票据（登录等接口调用）：票据存在且未过期即有效，校验后立即消费（一次性）。
     */
    public VerifyResult consumeTicket(String ticket) {
        CaptchaTicket stored = ticketStore.get(ticket);
        if (stored == null) {
            return VerifyResult.fail(CaptchaMessages.TICKET_INVALID, "INVALID_TICKET", messages);
        }
        ticketStore.remove(ticket);
        return VerifyResult.ok(CaptchaMessages.TICKET_VALID, messages);
    }

    /** 返回引擎支持的所有验证码类型编码（升序） */
    public List<String> supportedTypes() {
        return generators.keySet().stream()
                .map(CaptchaType::getCode)
                .sorted()
                .toList();
    }

    /** 返回滑块支持的拼图形状名称列表 */
    public List<String> supportedShapes() {
        CaptchaGenerator generator = generators.get(CaptchaType.SLIDER);
        if (generator instanceof SliderCaptchaGenerator slider) {
            return new ArrayList<>(slider.getShapeNames());
        }
        return List.of();
    }

    /** 手动移除一个未使用/异常的验证码会话 */
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
