package com.captcha.toolkit.generator;

import com.captcha.toolkit.behavior.BehaviorValidator;
import com.captcha.toolkit.behavior.ScratchBehaviorValidator;
import com.captcha.toolkit.config.BehaviorConfig;
import com.captcha.toolkit.config.ScratchConfig;
import com.captcha.toolkit.exception.CaptchaException;
import com.captcha.toolkit.i18n.CaptchaMessages;
import com.captcha.toolkit.i18n.MessageProvider;
import com.captcha.toolkit.i18n.ResourceBundleMessageProvider;
import com.captcha.toolkit.image.DataUriImageCodec;
import com.captcha.toolkit.model.CaptchaAnswer;
import com.captcha.toolkit.model.CaptchaSession;
import com.captcha.toolkit.model.GeneratedCaptcha;
import com.captcha.toolkit.model.ScratchChallengeData;
import com.captcha.toolkit.model.ScratchPatternSpec;
import com.captcha.toolkit.model.VerifyResult;
import com.captcha.toolkit.render.BackgroundProvider;
import com.captcha.toolkit.render.ScratchCaptchaRenderer;
import com.captcha.toolkit.type.CaptchaType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * 刮刮乐（横扫）生成器。
 *
 * <p>背景图内埋入多个与背景融合的图形，用户拖动滑块从左往右揭开蒙版；
 * 答案滑块位置 = 全部提示图形“刚好完整出现”的最小位置
 * （目标图案最右侧边缘的最大值）。停早则提示图形未出全，停晚则判定“未立即停止”。</p>
 */
public class ScratchCaptchaGenerator extends AbstractCaptchaGenerator<ScratchChallengeData> {

    /** 答案位置存入会话时的放大倍数（避免改动会话模型，与滑动曲线一致） */
    private static final int ANSWER_SCALE = 10_000;

    /** 刮刮乐配置 */
    private final ScratchConfig options;

    /** 背景图提供者 */
    private final BackgroundProvider backgroundProvider;

    /** 刮刮乐行为轨迹校验器 */
    private final BehaviorValidator behaviorValidator;

    /** 随机数源 */
    private final Random random = new Random();

    /** 使用默认（关闭）行为校验构造生成器 */
    public ScratchCaptchaGenerator(ScratchConfig options, BackgroundProvider backgroundProvider) {
        this(options, backgroundProvider,
                new ScratchBehaviorValidator(new BehaviorConfig()),
                new ResourceBundleMessageProvider());
    }

    /**
     * @param options            刮刮乐配置
     * @param backgroundProvider 背景图提供者
     * @param behaviorValidator  行为轨迹校验器
     */
    public ScratchCaptchaGenerator(ScratchConfig options,
                                   BackgroundProvider backgroundProvider,
                                   BehaviorValidator behaviorValidator) {
        this(options, backgroundProvider, behaviorValidator,
                new ResourceBundleMessageProvider());
    }

    /**
     * @param options            刮刮乐配置
     * @param backgroundProvider 背景图提供者
     * @param behaviorValidator  行为轨迹校验器
     * @param messages           用户提示消息提供者
     */
    public ScratchCaptchaGenerator(ScratchConfig options,
                                   BackgroundProvider backgroundProvider,
                                   BehaviorValidator behaviorValidator,
                                   MessageProvider messages) {
        super(messages);
        this.options = options;
        this.backgroundProvider = backgroundProvider;
        this.behaviorValidator = behaviorValidator;
    }

    @Override
    public CaptchaType type() {
        return CaptchaType.SCRATCH;
    }

    @Override
    protected GeneratedCaptcha<ScratchChallengeData> doGenerate(GenerateRequest request) {
        int w = options.getWidth();
        int h = options.getHeight();
        java.awt.image.BufferedImage raw = backgroundProvider.provide(w, h)
                .orElseThrow(() -> new CaptchaException(
                        "没有可用的背景图，请配置 captcha.background.sources 或开启 generate-fallback"));

        ScratchCaptchaRenderer renderer = new ScratchCaptchaRenderer(options);
        ScratchCaptchaRenderer.ScratchRenderResult rendered = renderer.render(raw);
        List<ScratchPatternSpec> patterns = rendered.patterns();
        List<Integer> targetIndices = pickTargets(patterns.size());
        List<String> prompt = new ArrayList<>();
        for (Integer index : targetIndices) {
            prompt.add(patterns.get(index).shape());
        }
        Collections.shuffle(prompt, random);

        // 答案：全部提示图形刚好完整出现的最小滑块位置（目标最右边缘的最大值）
        double answerX = answerX(patterns, targetIndices);
        CaptchaSession session = CaptchaSession.scratch(request.getId(), w, h,
                (int) Math.round(answerX * ANSWER_SCALE),
                patterns, targetIndices, options.getExpireSeconds() * 1000);

        GeneratedCaptcha<ScratchChallengeData> result = new GeneratedCaptcha<>();
        result.setSession(session);
        result.setImage1(rendered.background());
        result.setImage2(null);
        result.setWidth(w);
        result.setHeight(h);
        result.setData(new ScratchChallengeData(
                new DataUriImageCodec().encode(renderer.renderPromptImage(prompt), "png"),
                prompt.size(),
                request.isDebug() ? answerX : null,
                request.isDebug() ? targetIndices : null,
                request.isDebug()
                        ? patterns.stream()
                                .map(p -> new ScratchChallengeData.ScratchDebugPattern(
                                        p.shape(), p.x(), p.y()))
                                .toList()
                        : null));
        return result;
    }

    @Override
    protected VerifyResult doVerify(CaptchaSession session, CaptchaAnswer answer) {
        if (answer == null || answer.getXNorm() == null) {
            return VerifyResult.badRequest(CaptchaMessages.SCRATCH_MISSING_X_NORM, messages);
        }
        Optional<String> behaviorError = behaviorValidator.validate(
                answer.getTd(), answer, session);
        if (behaviorError.isPresent()) {
            return VerifyResult.fail(behaviorError.get(), "BEHAVIOR", messages);
        }
        double answerX = session.getX() / (double) ANSWER_SCALE;
        double diff = answer.getXNorm() - answerX;
        if (Math.abs(diff) <= options.getTolerance()) {
            return VerifyResult.ok(CaptchaMessages.VERIFY_OK, messages);
        }
        if (diff < 0) {
            // 停早了：提示图形尚未全部出现
            return VerifyResult.fail(CaptchaMessages.SCRATCH_MISMATCH, "WRONG", messages);
        }
        // 停晚了：目标图形已经全部出现却继续右移
        return VerifyResult.fail(
                CaptchaMessages.SCRATCH_CONTINUED_AFTER_DONE, "WRONG", messages);
    }

    @Override
    protected long minElapsedMs() {
        return options.getMinElapsedMs();
    }

    /** 随机选择目标图案下标：数量在 targetCountMin~targetCount 间随机，且不超过 patternCount */
    private List<Integer> pickTargets(int patternCount) {
        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < patternCount; i++) {
            candidates.add(i);
        }
        Collections.shuffle(candidates, random);
        int min = Math.max(0, options.getTargetCountMin());
        int max = Math.min(patternCount, Math.max(min, options.getTargetCount()));
        int count = min == max ? min : min + random.nextInt(max - min + 1);
        return new ArrayList<>(candidates.subList(0, count));
    }

    /** 全部目标图案刚好完整出现的最小滑块位置（目标最右边缘的最大值，归一化 0~1） */
    private static double answerX(
            List<ScratchPatternSpec> patterns, List<Integer> targetIndices) {
        double maxRight = 0;
        for (Integer index : targetIndices) {
            ScratchPatternSpec spec = patterns.get(index);
            maxRight = Math.max(maxRight, spec.x() + spec.size() / 2);
        }
        return Math.min(1, Math.max(0, maxRight));
    }
}
