package com.captcha.toolkit.generator;

import com.captcha.toolkit.model.CaptchaAnswer;
import com.captcha.toolkit.model.CaptchaSession;
import com.captcha.toolkit.model.GeneratedCaptcha;
import com.captcha.toolkit.model.VerifyResult;
import com.captcha.toolkit.i18n.CaptchaMessages;
import com.captcha.toolkit.i18n.MessageProvider;
import com.captcha.toolkit.i18n.ResourceBundleMessageProvider;

/**
 * 生成/校验的模板方法骨架：
 *
 * <ol>
 *   <li>生成阶段：调用子类 {@link #doGenerate}</li>
 *   <li>校验阶段：统一做类型检查、过期检查、最短耗时检查，再交给子类 {@link #doVerify}</li>
 * </ol>
 *
 * <p>新增验证码时只需继承本类并实现三个抽象方法。</p>
 */
public abstract class AbstractCaptchaGenerator implements CaptchaGenerator {

    /** 用户提示消息提供者（多语言资源加载） */
    protected final MessageProvider messages;

    /**
     * @param messages 用户提示消息提供者
     */
    protected AbstractCaptchaGenerator(MessageProvider messages) {
        this.messages = messages;
    }

    /** 使用默认中文消息提供者构造 */
    protected AbstractCaptchaGenerator() {
        this(new ResourceBundleMessageProvider());
    }

    /** 生成阶段：固定调用子类实现，不对外暴露模板细节 */
    @Override
    public final GeneratedCaptcha generate(GenerateRequest request) {
        return doGenerate(request);
    }

    /** 校验阶段：统一做类型、过期、最短耗时检查后再交给子类 */
    @Override
    public final VerifyResult verify(CaptchaSession session, CaptchaAnswer answer) {
        if (session == null || session.getType() != type()) {
            return VerifyResult.badRequest(CaptchaMessages.VERIFY_TYPE_MISMATCH, messages);
        }
        if (session.isExpired()) {
            return VerifyResult.expired(CaptchaMessages.VERIFY_EXPIRED, messages);
        }
        long elapsed = System.currentTimeMillis() - session.getCreatedAt();
        if (elapsed < minElapsedMs()) {
            return VerifyResult.tooFast(CaptchaMessages.VERIFY_TOO_FAST, messages);
        }
        return doVerify(session, answer);
    }

    /** 子类实现具体的挑战生成逻辑 */
    protected abstract GeneratedCaptcha doGenerate(GenerateRequest request);

    /** 子类实现具体的答案校验逻辑 */
    protected abstract VerifyResult doVerify(CaptchaSession session, CaptchaAnswer answer);

    /** 该类型允许的最短验证耗时（毫秒） */
    protected abstract long minElapsedMs();
}
