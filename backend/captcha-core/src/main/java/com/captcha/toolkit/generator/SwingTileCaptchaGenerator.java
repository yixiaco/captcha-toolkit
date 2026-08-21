package com.captcha.toolkit.generator;

import com.captcha.toolkit.behavior.BehaviorValidator;
import com.captcha.toolkit.behavior.SwingTileBehaviorValidator;
import com.captcha.toolkit.config.BehaviorConfig;
import com.captcha.toolkit.config.SwingTileConfig;
import com.captcha.toolkit.i18n.CaptchaMessages;
import com.captcha.toolkit.i18n.MessageProvider;
import com.captcha.toolkit.i18n.ResourceBundleMessageProvider;
import com.captcha.toolkit.model.CaptchaAnswer;
import com.captcha.toolkit.model.CaptchaSession;
import com.captcha.toolkit.model.GeneratedCaptcha;
import com.captcha.toolkit.model.PointVo;
import com.captcha.toolkit.model.SwingTileChallengeData;
import com.captcha.toolkit.model.VerifyResult;
import com.captcha.toolkit.render.BackgroundProvider;
import com.captcha.toolkit.render.SwingTileRenderer;
import com.captcha.toolkit.shape.PuzzleShapeRegistry;
import com.captcha.toolkit.type.CaptchaType;

import java.util.Optional;

/**
 * 滑块摆动图块验证码生成器：用户拖动滑块让图块沿多阶贝塞尔曲线运动到目标凹槽，
 * 方向随路径摆动，终点方向与真凹槽一致；答案固定为滑块终点（归一化位置 1）。
 */
public class SwingTileCaptchaGenerator
        extends AbstractCaptchaGenerator<SwingTileChallengeData> {

    /** 滑块摆动图块配置 */
    private final SwingTileConfig options;

    /** 背景图提供者 */
    private final BackgroundProvider backgroundProvider;

    /** 拼图形状注册表 */
    private final PuzzleShapeRegistry shapeRegistry;

    /** 滑块摆动图块行为轨迹校验器 */
    private final BehaviorValidator behaviorValidator;

    /** 使用默认（关闭）行为校验构造生成器 */
    public SwingTileCaptchaGenerator(SwingTileConfig options,
                                     BackgroundProvider backgroundProvider,
                                     PuzzleShapeRegistry shapeRegistry) {
        this(options, backgroundProvider, shapeRegistry,
                new SwingTileBehaviorValidator(new BehaviorConfig()),
                new ResourceBundleMessageProvider());
    }

    /**
     * @param options            滑块摆动图块配置
     * @param backgroundProvider 背景图提供者
     * @param shapeRegistry      拼图形状注册表
     * @param behaviorValidator  行为轨迹校验器
     */
    public SwingTileCaptchaGenerator(SwingTileConfig options,
                                     BackgroundProvider backgroundProvider,
                                     PuzzleShapeRegistry shapeRegistry,
                                     BehaviorValidator behaviorValidator) {
        this(options, backgroundProvider, shapeRegistry, behaviorValidator,
                new ResourceBundleMessageProvider());
    }

    /**
     * @param options            滑块摆动图块配置
     * @param backgroundProvider 背景图提供者
     * @param shapeRegistry      拼图形状注册表
     * @param behaviorValidator  行为轨迹校验器
     * @param messages           用户提示消息提供者
     */
    public SwingTileCaptchaGenerator(SwingTileConfig options,
                                     BackgroundProvider backgroundProvider,
                                     PuzzleShapeRegistry shapeRegistry,
                                     BehaviorValidator behaviorValidator,
                                     MessageProvider messages) {
        super(messages);
        this.options = options;
        this.backgroundProvider = backgroundProvider;
        this.shapeRegistry = shapeRegistry;
        this.behaviorValidator = behaviorValidator;
    }

    @Override
    public CaptchaType type() {
        return CaptchaType.SWING_TILE;
    }

    @Override
    protected GeneratedCaptcha<SwingTileChallengeData> doGenerate(GenerateRequest request) {
        SwingTileRenderer renderer = new SwingTileRenderer(
                options, backgroundProvider, shapeRegistry);
        renderer.run();

        // 真凹槽在路径上的位置（0~1）放大 10000 倍存进会话 x
        int scaledAnswer = (int) Math.round(renderer.getAnswerT() * 10000);
        CaptchaSession session = CaptchaSession.swingTile(
                request.getId(), renderer.getWidth(), renderer.getHeight(),
                scaledAnswer, options.getExpireSeconds() * 1000);
        GeneratedCaptcha<SwingTileChallengeData> result = new GeneratedCaptcha<>();
        result.setSession(session);
        result.setImage1(renderer.getArtwork());
        result.setImage2(renderer.getPiece());
        result.setWidth(renderer.getWidth());
        result.setHeight(renderer.getHeight());
        result.setData(new SwingTileChallengeData(
                renderer.getPath(),
                renderer.getStartRotation(),
                renderer.getEndRotation(),
                renderer.getSwingAmplitude(),
                renderer.getPieceImageSize(),
                request.isDebug() ? renderer.getAnswerT() : null,
                request.isDebug() ? renderer.getFakeTargets().stream()
                        .map(f -> new PointVo(f.getX(), f.getY()))
                        .toList() : null));
        return result;
    }

    @Override
    protected VerifyResult doVerify(CaptchaSession session, CaptchaAnswer answer) {
        if (answer == null || answer.getXNorm() == null) {
            return VerifyResult.badRequest(CaptchaMessages.SLIDER_MISSING_X_NORM, messages);
        }
        Optional<String> behaviorError = behaviorValidator.validate(
                answer.getTd(), answer, session);
        if (behaviorError.isPresent()) {
            return VerifyResult.fail(behaviorError.get(), "BEHAVIOR", messages);
        }
        // 图块必须停在真凹槽对应的路径位置（随机 answerT）才能对准
        double expected = session.getX() / 10000.0;
        if (Math.abs(answer.getXNorm() - expected) <= options.getTolerance()) {
            return VerifyResult.ok(CaptchaMessages.VERIFY_OK, messages);
        }
        return VerifyResult.fail(CaptchaMessages.VERIFY_WRONG, "WRONG", messages);
    }

    @Override
    protected long minElapsedMs() {
        return options.getMinElapsedMs();
    }
}
