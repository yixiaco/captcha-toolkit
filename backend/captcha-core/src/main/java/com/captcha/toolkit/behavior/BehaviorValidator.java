package com.captcha.toolkit.behavior;

import com.captcha.toolkit.i18n.CaptchaMessages;
import com.captcha.toolkit.model.CaptchaAnswer;
import com.captcha.toolkit.model.CaptchaSession;

import java.util.Optional;

/**
 * 行为轨迹校验策略：校验通过返回 {@link Optional#empty()}，否则返回失败原因。
 */
public interface BehaviorValidator {

    /**
     * 校验行为轨迹。
     *
     * @param td      前端提交的行为报文（明文或 gzip+base64url）
     * @param answer  本次提交的答案
     * @param session 验证码会话（包含服务端答案）
     * @return 校验通过返回 {@link Optional#empty()}，否则返回失败原因的消息编码
     *         （见 {@link CaptchaMessages}）
     */
    Optional<String> validate(String td, CaptchaAnswer answer, CaptchaSession session);
}
