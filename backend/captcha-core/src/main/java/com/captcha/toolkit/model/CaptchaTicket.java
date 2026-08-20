package com.captcha.toolkit.model;

import com.captcha.toolkit.CaptchaType;
import lombok.Data;

/**
 * 验证通过后发放的一次性票据。
 *
 * <p>票据只保存在服务端，登录等业务接口提交 ticket 后由引擎校验并消费，
 * 校验成功即从存储中移除，防止重放。</p>
 */
@Data
public class CaptchaTicket {

    /** 票据唯一标识，业务接口凭此校验 */
    private final String ticket;

    /** 发放票据的验证码类型 */
    private final CaptchaType type;

    /** 票据创建时间戳（毫秒） */
    private final long createdAt;

    /** 票据过期时间戳（毫秒） */
    private final long expiresAt;

    public CaptchaTicket(String ticket, CaptchaType type, long ttlMillis) {
        this.ticket = ticket;
        this.type = type;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.expiresAt = now + ttlMillis;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

}
