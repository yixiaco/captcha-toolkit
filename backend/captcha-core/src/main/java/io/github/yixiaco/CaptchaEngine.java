package io.github.yixiaco;

import io.github.yixiaco.config.CaptchaConfig;
import io.github.yixiaco.exception.CaptchaException;
import io.github.yixiaco.exception.RateLimitExceededException;
import io.github.yixiaco.factory.CaptchaFactory;
import io.github.yixiaco.factory.AngleCaptchaFactory;
import io.github.yixiaco.factory.ClickCaptchaFactory;
import io.github.yixiaco.factory.ClickShapeCaptchaFactory;
import io.github.yixiaco.factory.CurveCaptchaFactory;
import io.github.yixiaco.factory.RotateCaptchaFactory;
import io.github.yixiaco.factory.ScratchCaptchaFactory;
import io.github.yixiaco.factory.SlideCurveCaptchaFactory;
import io.github.yixiaco.factory.SliderCaptchaFactory;
import io.github.yixiaco.factory.SwingTileCaptchaFactory;
import io.github.yixiaco.generator.CaptchaGenerator;
import io.github.yixiaco.generator.GenerateRequest;
import io.github.yixiaco.generator.ScratchCaptchaGenerator;
import io.github.yixiaco.generator.SliderCaptchaGenerator;
import io.github.yixiaco.generator.SwingTileCaptchaGenerator;
import io.github.yixiaco.i18n.CaptchaMessages;
import io.github.yixiaco.i18n.MessageProvider;
import io.github.yixiaco.image.CaptchaImageCodec;
import io.github.yixiaco.limit.DeviceRequestLimiter;
import io.github.yixiaco.limit.InMemoryDeviceRequestLimiter;
import io.github.yixiaco.model.CaptchaAnswer;
import io.github.yixiaco.model.CaptchaChallenge;
import io.github.yixiaco.model.CaptchaSession;
import io.github.yixiaco.model.CaptchaTicket;
import io.github.yixiaco.model.GeneratedCaptcha;
import io.github.yixiaco.model.ShapeInfo;
import io.github.yixiaco.model.VerifyResult;
import io.github.yixiaco.render.BackgroundProvider;
import io.github.yixiaco.store.CaptchaSessionStore;
import io.github.yixiaco.store.CaptchaTicketStore;
import io.github.yixiaco.store.InMemoryCaptchaTicketStore;
import io.github.yixiaco.type.CaptchaType;
import io.github.yixiaco.util.FingerprintHasher;
import io.github.yixiaco.word.WordFactory;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

/**
 * 验证码引擎（门面模式）：对调用方屏蔽工厂、生成器、存储、编码细节。
 *
 * <p>既可以被 Spring 控制器调用，也可以被普通 Java 代码直接调用：
 * <pre>
 * CaptchaEngine engine = CaptchaEngine.of(config, store, codec, List.of(), backgroundProvider);
 * CaptchaChallenge challenge = engine.create(CaptchaType.SLIDER, Map.of("shape", "classic"), false);
 * // 面向 HTTP 客户端下发时用 createForClient：非 debug 下类型由后端类型池决定
 * CaptchaChallenge auto = engine.createForClient(null, Map.of(), false, null);
 * VerifyResult result = engine.verify(challenge.getId(),
 *         CaptchaAnswer.slider(100.0 / challenge.getWidth()));
 * </pre>
 */
public class CaptchaEngine {

    /** 验证码类型 → 生成器映射（各类型携带各自的泛型载荷） */
    private final Map<CaptchaType, CaptchaGenerator<?>> generators;

    /** 允许下发的类型池（非空，按 {@link CaptchaType} 声明顺序） */
    private final List<CaptchaType> typePool;

    /** 类型随机源；为 null 时每次选择使用 {@link ThreadLocalRandom} */
    private final RandomGenerator typeRandom;

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
        this(buildGenerators(factories, config), config, store, ticketStore, codec,
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
        Map<CaptchaType, CaptchaGenerator<?>> map = new EnumMap<>(CaptchaType.class);
        if (userFactories != null) {
            for (CaptchaFactory factory : userFactories) {
                map.put(factory.type(), factory.create(config));
            }
        }
        map.putIfAbsent(CaptchaType.SLIDER,
                new SliderCaptchaFactory(sliderBackgroundProvider).create(config));
        map.putIfAbsent(CaptchaType.CLICK,
                new ClickCaptchaFactory(clickBackgroundProvider, wordFactory).create(config));
        map.putIfAbsent(CaptchaType.SHAPE_CLICK,
                new ClickShapeCaptchaFactory(clickBackgroundProvider).create(config));
        map.putIfAbsent(CaptchaType.ROTATE,
                new RotateCaptchaFactory(sliderBackgroundProvider).create(config));
        map.putIfAbsent(CaptchaType.ANGLE,
                new AngleCaptchaFactory(sliderBackgroundProvider).create(config));
        map.putIfAbsent(CaptchaType.SCRATCH,
                new ScratchCaptchaFactory(sliderBackgroundProvider).create(config));
        map.putIfAbsent(CaptchaType.CURVE,
                new CurveCaptchaFactory(sliderBackgroundProvider).create(config));
        map.putIfAbsent(CaptchaType.SLIDE_CURVE,
                new SlideCurveCaptchaFactory(sliderBackgroundProvider).create(config));
        map.putIfAbsent(CaptchaType.SWING_TILE,
                new SwingTileCaptchaFactory(sliderBackgroundProvider).create(config));
        return new CaptchaEngine(map, config, store, ticketStore, codec,
                config.isDebugEnabled(), config.getTicketExpireSeconds() * 1000,
                config.getMessageProvider(), effectiveRateLimiter(config),
                config.getRateLimit().isEnabled(), config.getRateLimit().getFingerprintSalt());
    }

    /** 私有构造：统一接收已组装好的生成器映射与依赖 */
    private CaptchaEngine(Map<CaptchaType, CaptchaGenerator<?>> generators,
                          CaptchaConfig config,
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
        this.typePool = buildTypePool(config.getTypes(), generators.keySet());
        this.typeRandom = config.getTypeRandom();
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
    private static Map<CaptchaType, CaptchaGenerator<?>> buildGenerators(
            List<CaptchaFactory> factories, CaptchaConfig config) {
        Map<CaptchaType, CaptchaGenerator<?>> map = new EnumMap<>(CaptchaType.class);
        if (factories != null) {
            for (CaptchaFactory factory : factories) {
                map.put(factory.type(), factory.create(config));
            }
        }
        map.putIfAbsent(CaptchaType.SLIDER, new SliderCaptchaFactory().create(config));
        map.putIfAbsent(CaptchaType.CLICK, new ClickCaptchaFactory().create(config));
        map.putIfAbsent(CaptchaType.SHAPE_CLICK, new ClickShapeCaptchaFactory().create(config));
        map.putIfAbsent(CaptchaType.ROTATE, new RotateCaptchaFactory().create(config));
        map.putIfAbsent(CaptchaType.ANGLE, new AngleCaptchaFactory().create(config));
        map.putIfAbsent(CaptchaType.SCRATCH, new ScratchCaptchaFactory().create(config));
        map.putIfAbsent(CaptchaType.CURVE, new CurveCaptchaFactory().create(config));
        map.putIfAbsent(CaptchaType.SLIDE_CURVE, new SlideCurveCaptchaFactory().create(config));
        map.putIfAbsent(CaptchaType.SWING_TILE, new SwingTileCaptchaFactory().create(config));
        return map;
    }

    /**
     * 解析允许下发的类型池：未配置时取全部已注册类型。
     *
     * <p>配置了未知编码或未注册类型时直接抛异常，避免把配置错误拖到线上请求才暴露。</p>
     */
    private static List<CaptchaType> buildTypePool(List<String> configured,
                                                  Set<CaptchaType> registered) {
        if (configured == null || configured.isEmpty()) {
            return List.copyOf(registered);
        }
        Set<CaptchaType> pool = new LinkedHashSet<>();
        for (String code : configured) {
            if (code == null || code.isBlank()) {
                continue;
            }
            CaptchaType type;
            try {
                type = CaptchaType.fromCode(code.trim());
            } catch (IllegalArgumentException e) {
                throw new CaptchaException("captcha.types 配置了未知的验证码类型: " + code
                        + "，可选值: " + Arrays.stream(CaptchaType.values())
                                .map(CaptchaType::getCode).toList());
            }
            if (!registered.contains(type)) {
                throw new CaptchaException("captcha.types 配置的验证码类型未注册: " + code);
            }
            pool.add(type);
        }
        if (pool.isEmpty()) {
            throw new CaptchaException("captcha.types 不能全部为空");
        }
        return List.copyOf(pool);
    }

    /**
     * 下发一张验证码。
     *
     * @param type   验证码类型
     * @param params 扩展参数，例如滑块 shape
     * @param debug  是否尝试附加答案（最终受 captcha.debug-enabled 控制）
     */
    public CaptchaChallenge<?> create(CaptchaType type, Map<String, String> params, boolean debug) {
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
    public CaptchaChallenge<?> create(CaptchaType type, Map<String, String> params, boolean debug,
                                      String deviceFingerprint) {
        enforceDeviceRateLimit(deviceFingerprint);
        CaptchaGenerator<?> generator = generators.get(type);
        if (generator == null) {
            throw new CaptchaException("不支持的验证码类型: " + type);
        }
        // debug 是否真正生效由引擎配置决定：debug-enabled 关闭时，
        // 即使调用方传入 debug=true，也不能返回任何答案/形状信息
        boolean effectiveDebug = debug && debugEnabled;
        Map<String, String> effectiveParams = params == null
                ? new java.util.LinkedHashMap<>()
                : new java.util.LinkedHashMap<>(params);
        if (!effectiveDebug) {
            effectiveParams.remove("shape");
        }
        GenerateRequest request = new GenerateRequest(
                UUID.randomUUID().toString(), effectiveParams, effectiveDebug, deviceFingerprint);
        GeneratedCaptcha<?> generated = generator.generate(request);
        store.put(generated.getSession());

        CaptchaChallenge<Object> challenge = new CaptchaChallenge<>();
        challenge.setId(generated.getSession().getId());
        challenge.setType(type.getCode());
        // 部分类型（如角度验证）可能没有主背景图，只下发独立小图
        if (generated.getImage1() != null) {
            challenge.setImage1(codec.encode(generated.getImage1(), "png"));
        }
        if (generated.getImage2() != null) {
            challenge.setImage2(codec.encode(generated.getImage2(), "png"));
        }
        challenge.setWidth(generated.getWidth());
        challenge.setHeight(generated.getHeight());
        challenge.setData(generated.getData());
        // 只有扩展元数据非空才下发，避免向前端暴露空的 metadata 对象
        if (!generated.getMetadata().isEmpty()) {
            challenge.setMetadata(generated.getMetadata());
        }
        return challenge;
    }

    /**
     * 面向客户端（HTTP）下发一张验证码：<b>类型由后端决定</b>。
     *
     * <p>只有 debug 请求且 {@code captcha.debug-enabled=true} 时，才允许调用方用
     * {@code requestedTypeCode} 指定类型；其余情况下该参数一律被忽略，
     * 由引擎从类型池（{@code captcha.types}，为空表示全部已注册类型）中随机挑选。</p>
     *
     * @param requestedTypeCode 客户端请求的类型编码（slider / click / ...；null、空串或 auto 表示交给后端）
     * @param params            扩展参数，例如滑块 shape（非 debug 时会被丢弃）
     * @param debug             客户端是否请求调试信息
     * @param deviceFingerprint 设备指纹（可为 null；限流开启且指纹缺失时不计数）
     * @return 下发的验证码，其中 {@code type} 为后端实际选择的类型
     * @throws IllegalArgumentException 客户端类型编码非法，或 debug 请求了类型池之外的类型
     */
    public CaptchaChallenge<?> createForClient(String requestedTypeCode,
                                               Map<String, String> params,
                                               boolean debug,
                                               String deviceFingerprint) {
        // debug 是否真正生效由引擎配置决定：debug-enabled 关闭时客户端无权指定类型
        boolean effectiveDebug = debug && debugEnabled;
        CaptchaType requested = effectiveDebug ? parseClientType(requestedTypeCode) : null;
        if (requested != null && !typePool.contains(requested)) {
            throw new IllegalArgumentException("验证码类型未开放: " + requested.getCode());
        }
        return create(requested != null ? requested : nextType(),
                params, effectiveDebug, deviceFingerprint);
    }

    /** 解析客户端请求的类型；null、空串与 auto 都表示“由后端决定” */
    private static CaptchaType parseClientType(String code) {
        if (code == null || code.isBlank() || "auto".equalsIgnoreCase(code.trim())) {
            return null;
        }
        return CaptchaType.fromCode(code.trim());
    }

    /** 从类型池中随机挑选一种类型 */
    private CaptchaType nextType() {
        if (typePool.size() == 1) {
            return typePool.get(0);
        }
        RandomGenerator random = typeRandom != null ? typeRandom : ThreadLocalRandom.current();
        return typePool.get(random.nextInt(typePool.size()));
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
        CaptchaGenerator<?> generator = generators.get(session.getType());
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


    /** 返回后端允许下发的验证码类型编码（升序，即类型池内容） */
    public List<String> supportedTypes() {
        return typePool.stream()
                .map(CaptchaType::getCode)
                .sorted()
                .toList();
    }

    /**
     * 返回各类型支持的拼图形状信息列表（key 为验证码类型编码）。
     * 未纳入类型池的类型返回空列表。
     *
     * @param debug 是否返回完整形状列表；非 debug 时返回空列表，
     *              避免把可用图形白名单暴露给前端
     */
    public Map<String, List<ShapeInfo>> supportedShapes(boolean debug) {
        Map<String, List<ShapeInfo>> shapes = new LinkedHashMap<>();
        List<ShapeInfo> slider = List.of();
        List<ShapeInfo> swingTile = List.of();
        if (debug) {
            CaptchaGenerator<?> sliderGenerator = typePool.contains(CaptchaType.SLIDER)
                    ? generators.get(CaptchaType.SLIDER) : null;
            if (sliderGenerator instanceof SliderCaptchaGenerator generator) {
                slider = generator.getShapeOptions();
            }
            CaptchaGenerator<?> swingTileGenerator = typePool.contains(CaptchaType.SWING_TILE)
                    ? generators.get(CaptchaType.SWING_TILE) : null;
            if (swingTileGenerator instanceof SwingTileCaptchaGenerator generator) {
                swingTile = generator.getShapeOptions();
            }
        }
        shapes.put("slider", slider);
        shapes.put("swing-tile", swingTile);
        return shapes;
    }

    /** 返回后端是否开启 debug 模式（决定是否下发答案与形状白名单） */
    public boolean isDebugEnabled() {
        return debugEnabled;
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
