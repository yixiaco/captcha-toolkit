package com.captcha.toolkit.generator;

import com.captcha.toolkit.model.CaptchaAnswer;
import com.captcha.toolkit.model.CaptchaSession;
import com.captcha.toolkit.model.GeneratedCaptcha;
import com.captcha.toolkit.model.VerifyResult;

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

    /** 生成阶段：固定调用子类实现，不对外暴露模板细节 */
    @Override
    public final GeneratedCaptcha generate(GenerateRequest request) {
        return doGenerate(request);
    }

    /** 校验阶段：统一做类型、过期、最短耗时检查后再交给子类 */
    @Override
    public final VerifyResult verify(CaptchaSession session, CaptchaAnswer answer) {
        if (session == null || session.getType() != type()) {
            return VerifyResult.badRequest("验证码类型不匹配");
        }
        if (session.isExpired()) {
            return VerifyResult.expired("验证码已过期，请刷新重试");
        }
        long elapsed = System.currentTimeMillis() - session.getCreatedAt();
        if (elapsed < minElapsedMs()) {
            return VerifyResult.tooFast("验证速度异常");
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
