package com.captcha.toolkit.generator;

import com.captcha.toolkit.CaptchaType;
import com.captcha.toolkit.model.CaptchaAnswer;
import com.captcha.toolkit.model.CaptchaSession;
import com.captcha.toolkit.model.GeneratedCaptcha;
import com.captcha.toolkit.model.VerifyResult;

/**
 * 验证码生成器：生成挑战 + 校验答案。
 *
 * <p>抽象模板 {@link AbstractCaptchaGenerator} 固定了“生成→会话→校验→最小耗时”的流程，
 * 具体验证码只需实现自己的生成与校验细节。</p>
 */
public interface CaptchaGenerator {

    CaptchaType type();

    GeneratedCaptcha generate(GenerateRequest request);

    VerifyResult verify(CaptchaSession session, CaptchaAnswer answer);
}
