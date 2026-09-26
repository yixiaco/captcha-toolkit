package io.github.yixiaco.generator;

import io.github.yixiaco.type.CaptchaType;
import io.github.yixiaco.model.CaptchaAnswer;
import io.github.yixiaco.model.CaptchaSession;
import io.github.yixiaco.model.GeneratedCaptcha;
import io.github.yixiaco.model.VerifyResult;

/**
 * 验证码生成器：生成挑战 + 校验答案。
 *
 * <p>抽象模板 {@link AbstractCaptchaGenerator} 固定了“生成→会话→校验→最小耗时”的流程，
 * 具体验证码只需实现自己的生成与校验细节。</p>
 *
 * @param <T> 类型特定化数据（如 {@link io.github.yixiaco.model.SliderChallengeData}）
 */
public interface CaptchaGenerator<T> {

    /** 返回该生成器对应的验证码类型 */
    CaptchaType type();

    /** 生成一张验证码挑战 */
    GeneratedCaptcha<T> generate(GenerateRequest request);

    /** 校验用户提交的答案 */
    VerifyResult verify(CaptchaSession session, CaptchaAnswer answer);
}
