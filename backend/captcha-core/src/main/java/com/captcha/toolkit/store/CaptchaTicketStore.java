package com.captcha.toolkit.store;

import com.captcha.toolkit.model.CaptchaTicket;

/**
 * 验证码票据存储适配器（策略模式）。
 *
 * <p>默认提供内存实现；多实例部署时实现本接口并注册 Bean（如 Redis），
 * 登录等业务接口即可跨实例校验票据。</p>
 */
public interface CaptchaTicketStore {

    void put(CaptchaTicket ticket);

    /** 获取票据；已过期时返回 null 并顺手清理 */
    CaptchaTicket get(String ticket);

    void remove(String ticket);

    void clearExpired();
}
