package com.captcha.toolkit.generator;

import com.captcha.toolkit.CaptchaType;
import com.captcha.toolkit.behavior.BehaviorValidator;
import com.captcha.toolkit.behavior.SliderBehaviorValidator;
import com.captcha.toolkit.config.BehaviorConfig;
import com.captcha.toolkit.config.SliderConfig;
import com.captcha.toolkit.model.CaptchaAnswer;
import com.captcha.toolkit.model.CaptchaSession;
import com.captcha.toolkit.model.GeneratedCaptcha;
import com.captcha.toolkit.model.PointVo;
import com.captcha.toolkit.model.VerifyResult;
import com.captcha.toolkit.render.BackgroundProvider;
import com.captcha.toolkit.render.SliderRenderer;
import com.captcha.toolkit.shape.PuzzleShapeRegistry;

import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * 滑块拼图验证码生成器。
 */
public class SliderCaptchaGenerator extends AbstractCaptchaGenerator {

    /** 滑块配置 */
    private final SliderConfig options;

    /** 背景图提供者 */
    private final BackgroundProvider backgroundProvider;

    /** 拼图形状注册表 */
    private final PuzzleShapeRegistry shapeRegistry;
    /** 滑块行为轨迹校验器 */
    private final BehaviorValidator behaviorValidator;

    /** 随机数源 */
    private final Random random = new Random();

    /** 使用默认（关闭）行为校验构造生成器 */
    public SliderCaptchaGenerator(SliderConfig options,
                                  BackgroundProvider backgroundProvider,
                                  PuzzleShapeRegistry shapeRegistry) {
        this(options, backgroundProvider, shapeRegistry,
                new SliderBehaviorValidator(new BehaviorConfig()));
    }

    /**
     * @param options          滑块配置
     * @param backgroundProvider 背景图提供者
     * @param shapeRegistry      拼图形状注册表
     * @param behaviorValidator  行为轨迹校验器
     */
    public SliderCaptchaGenerator(SliderConfig options,
                                  BackgroundProvider backgroundProvider,
                                  PuzzleShapeRegistry shapeRegistry,
                                  BehaviorValidator behaviorValidator) {
        this.options = options;
        this.backgroundProvider = backgroundProvider;
        this.shapeRegistry = shapeRegistry;
        this.behaviorValidator = behaviorValidator;
    }

    @Override
    public CaptchaType type() {
        return CaptchaType.SLIDER;
    }

    @Override
    protected GeneratedCaptcha doGenerate(GenerateRequest request) {
        // 拼图形状默认由后端随机决定；只有 debug 模式下前端才能显式指定
        String requested = request.getParams().get("shape");
        String shape = request.isDebug() && requested != null && !requested.isBlank()
                ? resolveShape(requested)
                : resolveShape(null);
        SliderRenderer renderer = new SliderRenderer(options, backgroundProvider, shapeRegistry);
        renderer.setShape(shape);
        renderer.run();

        CaptchaSession session = CaptchaSession.slider(
                request.getId(), shape, renderer.getX(), renderer.getY(),
                renderer.getWidth(), renderer.getHeight(), options.getExpireSeconds() * 1000);

        GeneratedCaptcha result = new GeneratedCaptcha();
        result.setSession(session);
        result.setImage1(renderer.getArtwork());
        result.setImage2(renderer.getVacancy());
        result.setWidth(renderer.getWidth());
        result.setHeight(renderer.getHeight());
        result.setShape(shape);
        result.setPieceOffsetX(renderer.getPieceOffsetX());
        if (request.isDebug()) {
            result.setDebugX(renderer.getX());
            result.setDebugFakeTargets(renderer.getFakeTargets().stream()
                    .map(p -> new PointVo(p.getX(), p.getY()))
                    .toList());
        }
        return result;
    }

    @Override
    protected VerifyResult doVerify(CaptchaSession session, CaptchaAnswer answer) {
        if (answer == null || answer.getXNorm() == null) {
            return VerifyResult.badRequest("缺少滑块位移 xNorm");
        }
        Optional<String> behaviorError = behaviorValidator.validate(
                answer.getTd(), answer, session);
        if (behaviorError.isPresent()) {
            return VerifyResult.fail(behaviorError.get(), "BEHAVIOR");
        }
        // 答案是归一化位移，直接与服务端答案的归一化坐标对比，与渲染尺寸无关
        double expected = (double) session.getX() / session.getWidth();
        double tolerance = (double) options.getTolerance() / session.getWidth();
        if (Math.abs(answer.getXNorm() - expected) <= tolerance) {
            return VerifyResult.ok("验证通过");
        }
        return VerifyResult.fail("验证失败，请重试", "WRONG");
    }

    @Override
    protected long minElapsedMs() {
        return options.getMinElapsedMs();
    }

    /** 解析请求的形状名：未指定/random 时随机选择，非法时回退默认形状 */
    private String resolveShape(String requested) {
        // 未指定或 shape=random：从启用形状里随机挑一个（后端决定）
        if (requested == null || requested.isBlank() || "random".equalsIgnoreCase(requested)) {
            List<String> candidates = options.getEnabledShapes().stream()
                    .filter(shapeRegistry::contains)
                    .toList();
            if (!candidates.isEmpty()) {
                return candidates.get(random.nextInt(candidates.size()));
            }
        }
        if (requested != null && options.getEnabledShapes().contains(requested)
                && shapeRegistry.contains(requested)) {
            return requested;
        }
        String fallback = options.getDefaultShape();
        if (shapeRegistry.contains(fallback) && options.getEnabledShapes().contains(fallback)) {
            return fallback;
        }
        return "classic";
    }

    /** 返回启用且已注册的形状名称列表 */
    public List<String> getShapeNames() {
        return options.getEnabledShapes().stream()
                .filter(shapeRegistry::contains)
                .toList();
    }
}
