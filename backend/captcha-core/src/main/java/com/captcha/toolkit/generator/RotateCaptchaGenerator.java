package com.captcha.toolkit.generator;

import com.captcha.toolkit.type.CaptchaType;
import com.captcha.toolkit.behavior.BehaviorValidator;
import com.captcha.toolkit.behavior.RotateBehaviorValidator;
import com.captcha.toolkit.config.BehaviorConfig;
import com.captcha.toolkit.config.RotateConfig;
import com.captcha.toolkit.i18n.CaptchaMessages;
import com.captcha.toolkit.i18n.MessageProvider;
import com.captcha.toolkit.i18n.ResourceBundleMessageProvider;
import com.captcha.toolkit.exception.CaptchaException;
import com.captcha.toolkit.model.CaptchaAnswer;
import com.captcha.toolkit.model.CaptchaSession;
import com.captcha.toolkit.model.GeneratedCaptcha;
import com.captcha.toolkit.model.RotateChallengeData;
import com.captcha.toolkit.model.VerifyResult;
import com.captcha.toolkit.render.BackgroundProvider;
import com.captcha.toolkit.util.ImageUtil;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.Optional;
import java.util.Random;

/**
 * 图片旋转验证码生成器。
 *
 * <p>背景中心放一个按随机角度旋转错位的圆盘，用户拖动滑块把圆盘转回
 * 与背景对齐的角度；答案角度只保存在服务端会话里。</p>
 */
public class RotateCaptchaGenerator extends AbstractCaptchaGenerator<RotateChallengeData> {

    /** 旋转配置 */
    private final RotateConfig options;

    /** 背景图提供者 */
    private final BackgroundProvider backgroundProvider;
    /** 旋转行为轨迹校验器 */
    private final BehaviorValidator behaviorValidator;

    /** 随机数源 */
    private final Random random = new Random();

    /** 使用默认（关闭）行为校验构造生成器 */
    public RotateCaptchaGenerator(RotateConfig options, BackgroundProvider backgroundProvider) {
        this(options, backgroundProvider,
                new RotateBehaviorValidator(new BehaviorConfig()),
                new ResourceBundleMessageProvider());
    }

    /**
     * @param options           旋转配置
     * @param backgroundProvider 背景图提供者
     * @param behaviorValidator  行为轨迹校验器
     */
    public RotateCaptchaGenerator(RotateConfig options,
                                  BackgroundProvider backgroundProvider,
                                  BehaviorValidator behaviorValidator) {
        this(options, backgroundProvider, behaviorValidator,
                new ResourceBundleMessageProvider());
    }

    /**
     * @param options           旋转配置
     * @param backgroundProvider 背景图提供者
     * @param behaviorValidator  行为轨迹校验器
     * @param messages           用户提示消息提供者
     */
    public RotateCaptchaGenerator(RotateConfig options,
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
        return CaptchaType.ROTATE;
    }

    @Override
    protected GeneratedCaptcha<RotateChallengeData> doGenerate(GenerateRequest request) {
        int w = options.getWidth();
        int h = options.getHeight();
        int scale = Math.max(1, options.getRenderScale());
        BufferedImage raw = backgroundProvider.provide(w, h)
                .orElseThrow(() -> new CaptchaException(
                        "没有可用的背景图，请配置 captcha.background.sources 或开启 generate-fallback"));

        int hiW = w * scale;
        int hiH = h * scale;
        BufferedImage thumb = ImageUtil.cover(raw, hiW, hiH);

        // 随机错位角度；用户需要转回的角度 = 360 - angle
        double angle = rand(options.getMinAngle(), options.getMaxAngle());
        double answer = normalize(360 - angle);

        int cx = hiW / 2;
        int cy = hiH / 2;
        int radius = (int) (Math.min(hiW, hiH) * 0.30);

        // 圆盘：背景按 angle 旋转后裁成圆形
        BufferedImage disc = new BufferedImage(hiW, hiH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D dg = disc.createGraphics();
        dg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        dg.rotate(Math.toRadians(angle), cx, cy);
        dg.setClip(new Ellipse2D.Double(cx - radius, cy - radius, radius * 2, radius * 2));
        dg.drawImage(thumb, 0, 0, null);
        dg.dispose();

        // 大图：背景 + 错位圆盘 + 细描边提示旋转区域
        BufferedImage artworkHi = new BufferedImage(hiW, hiH, BufferedImage.TYPE_INT_RGB);
        Graphics2D ag = artworkHi.createGraphics();
        ag.drawImage(thumb, 0, 0, null);
        ag.drawImage(disc, 0, 0, null);
        ag.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        ag.setColor(new Color(255, 255, 255, 150));
        ag.setStroke(new BasicStroke(Math.max(1f, scale * 1.5f)));
        ag.draw(new Ellipse2D.Double(cx - radius, cy - radius, radius * 2, radius * 2));
        ag.dispose();

        BufferedImage artwork = ImageUtil.scaleDown(artworkHi, w, h);
        BufferedImage piece = ImageUtil.scaleDown(disc, w, h);

        CaptchaSession session = CaptchaSession.rotate(request.getId(), w, h,
                answer, options.getExpireSeconds() * 1000);
        GeneratedCaptcha<RotateChallengeData> result = new GeneratedCaptcha<>();
        result.setSession(session);
        result.setImage1(artwork);
        result.setImage2(piece);
        result.setWidth(w);
        result.setHeight(h);
        result.setData(new RotateChallengeData(request.isDebug() ? answer : null));
        return result;
    }

    @Override
    protected VerifyResult doVerify(CaptchaSession session, CaptchaAnswer answer) {
        if (answer == null || answer.getAngle() == null) {
            return VerifyResult.badRequest(CaptchaMessages.ROTATE_MISSING_ANGLE, messages);
        }
        Optional<String> behaviorError = behaviorValidator.validate(
                answer.getTd(), answer, session);
        if (behaviorError.isPresent()) {
            return VerifyResult.fail(behaviorError.get(), "BEHAVIOR", messages);
        }
        double diff = normalize(answer.getAngle() - session.getRotation());
        if (Math.abs(diff) <= options.getTolerance()) {
            return VerifyResult.ok(CaptchaMessages.VERIFY_OK, messages);
        }
        return VerifyResult.fail(CaptchaMessages.VERIFY_WRONG, "WRONG", messages);
    }

    @Override
    protected long minElapsedMs() {
        return options.getMinElapsedMs();
    }

    /** 把角度归一化到 [-180, 180) */
    private static double normalize(double degrees) {
        double value = degrees % 360;
        if (value > 180) {
            value -= 360;
        } else if (value < -180) {
            value += 360;
        }
        return value;
    }

    /** 返回 [min, max] 区间内的随机浮点数 */
    private double rand(double min, double max) {
        return min + random.nextDouble() * (max - min);
    }
}
