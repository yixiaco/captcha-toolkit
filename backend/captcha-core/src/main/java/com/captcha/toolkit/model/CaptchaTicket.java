package com.captcha.toolkit.model;

import com.captcha.toolkit.CaptchaType;

/**
 * 验证通过后发放的一次性票据。
 *
 * <p>票据只保存在服务端，登录等业务接口提交 ticket 后由引擎校验并消费，
 * 校验成功即从存储中移除，防止重放。</p>
 */
public class CaptchaTicket {

    private final String ticket;
    private final CaptchaType type;
    private final long createdAt;
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

    public String getTicket() {
        return ticket;
    }

    public CaptchaType getType() {
        return type;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }
}
