package com.captcha.toolkit.generator;

import com.captcha.toolkit.behavior.BehaviorValidator;
import com.captcha.toolkit.behavior.ClickBehaviorValidator;
import com.captcha.toolkit.config.BehaviorConfig;
import com.captcha.toolkit.config.ClickShapeConfig;
import com.captcha.toolkit.exception.CaptchaException;
import com.captcha.toolkit.i18n.MessageProvider;
import com.captcha.toolkit.i18n.ResourceBundleMessageProvider;
import com.captcha.toolkit.image.DataUriImageCodec;
import com.captcha.toolkit.model.CaptchaSession;
import com.captcha.toolkit.model.ClickChallengeData;
import com.captcha.toolkit.model.GeneratedCaptcha;
import com.captcha.toolkit.model.PointVo;
import com.captcha.toolkit.model.ScratchPatternSpec;
import com.captcha.toolkit.render.BackgroundProvider;
import com.captcha.toolkit.render.ClickShapeCaptchaRenderer;
import com.captcha.toolkit.render.ShapePromptRenderer;
import com.captcha.toolkit.shape.PuzzleShapeRegistry;
import com.captcha.toolkit.type.CaptchaType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 图形点选验证码生成器。
 *
 * <p>与文字点选同属点选类：背景图内埋入目标图形与干扰图形，
 * 用户按提示依次点击目标图形；目标坐标与提示顺序保存在会话中，
 * 校验复用 {@link AbstractClickCaptchaGenerator} 的公共点选规则。</p>
 */
public class ClickShapeCaptchaGenerator
        extends AbstractClickCaptchaGenerator<ClickChallengeData> {

    /** 图形点选配置 */
    private final ClickShapeConfig options;

    /** 背景图提供者 */
    private final BackgroundProvider backgroundProvider;

    /** 图形形状注册表 */
    private final PuzzleShapeRegistry shapeRegistry;

    /** 随机数源 */
    private final Random random = new Random();

    /** 使用默认（关闭）行为校验构造生成器 */
    public ClickShapeCaptchaGenerator(ClickShapeConfig options,
                                      BackgroundProvider backgroundProvider,
                                      PuzzleShapeRegistry shapeRegistry) {
        this(options, backgroundProvider, shapeRegistry,
                new ClickBehaviorValidator(new BehaviorConfig()),
                new ResourceBundleMessageProvider());
    }

    /**
     * @param options            图形点选配置
     * @param backgroundProvider 背景图提供者
     * @param shapeRegistry      图形形状注册表
     * @param behaviorValidator  点选行为轨迹校验器
     */
    public ClickShapeCaptchaGenerator(ClickShapeConfig options,
                                      BackgroundProvider backgroundProvider,
                                      PuzzleShapeRegistry shapeRegistry,
                                      BehaviorValidator behaviorValidator) {
        this(options, backgroundProvider, shapeRegistry, behaviorValidator,
                new ResourceBundleMessageProvider());
    }

    /**
     * @param options            图形点选配置
     * @param backgroundProvider 背景图提供者
     * @param shapeRegistry      图形形状注册表
     * @param behaviorValidator  点选行为轨迹校验器
     * @param messages           用户提示消息提供者
     */
    public ClickShapeCaptchaGenerator(ClickShapeConfig options,
                                      BackgroundProvider backgroundProvider,
                                      PuzzleShapeRegistry shapeRegistry,
                                      BehaviorValidator behaviorValidator,
                                      MessageProvider messages) {
        super(messages, behaviorValidator);
        this.options = options;
        this.backgroundProvider = backgroundProvider;
        this.shapeRegistry = shapeRegistry;
    }

    @Override
    public CaptchaType type() {
        return CaptchaType.SHAPE_CLICK;
    }

    @Override
    protected GeneratedCaptcha<ClickChallengeData> doGenerate(GenerateRequest request) {
        int w = options.getWidth();
        int h = options.getHeight();
        java.awt.image.BufferedImage raw = backgroundProvider.provide(w, h)
                .orElseThrow(() -> new CaptchaException(
                        "没有可用的背景图，请配置 captcha.background.sources 或开启 generate-fallback"));

        // 全部可用图形（排除 classic 拼图块外观，避免与滑块混淆）
        List<String> pool = shapeRegistry.names().stream()
                .filter(name -> !"classic".equals(name))
                .collect(java.util.stream.Collectors.toList());
        Collections.shuffle(pool, random);
        int targetCount = Math.min(options.getTargetCount(), pool.size());
        int distractorCount = Math.min(
                options.getDistractorCount(), pool.size() - targetCount);
        List<String> targets = new ArrayList<>(pool.subList(0, targetCount));
        List<String> allShapes = new ArrayList<>(targets);
        allShapes.addAll(pool.subList(targetCount, targetCount + distractorCount));

        ClickShapeCaptchaRenderer renderer =
                new ClickShapeCaptchaRenderer(options, shapeRegistry);
        ClickShapeCaptchaRenderer.RenderResult rendered = renderer.render(raw, allShapes);

        // 目标图形按点击顺序保存（干扰图形不参与校验）
        List<PointVo> targetPoints = rendered.patterns().subList(0, targetCount).stream()
                .map(spec -> new PointVo(
                        (int) Math.round(spec.x() * w),
                        (int) Math.round(spec.y() * h)))
                .toList();
        CaptchaSession session = CaptchaSession.shapeClick(
                request.getId(), w, h, targetPoints, targets,
                options.getExpireSeconds() * 1000);

        String promptImage = new DataUriImageCodec().encode(
                ShapePromptRenderer.render(shapeRegistry, targets), "png");
        GeneratedCaptcha<ClickChallengeData> result = new GeneratedCaptcha<>();
        result.setSession(session);
        result.setImage1(rendered.background());
        result.setWidth(w);
        result.setHeight(h);
        result.setData(new ClickChallengeData(
                promptImage,
                targetCount,
                request.isDebug() ? new ArrayList<>(targetPoints) : null,
                null));
        return result;
    }

    @Override
    protected long minElapsedMs() {
        return options.getMinElapsedMs();
    }

    @Override
    protected double tolerance() {
        return options.getTolerance();
    }
}
