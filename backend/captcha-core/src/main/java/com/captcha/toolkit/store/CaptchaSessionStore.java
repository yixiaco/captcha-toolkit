package com.captcha.toolkit.store;

import com.captcha.toolkit.model.CaptchaSession;

/**
 * 验证码会话存储适配器（策略模式）。
 *
 * <p>默认提供内存实现；接入 Redis / JDBC 时实现本接口并注册为 Bean 即可，
 * 引擎与控制器完全不用改动。</p>
 */
public interface CaptchaSessionStore {

    void put(CaptchaSession session);

    /** 获取会话；已过期时返回 null 并顺手清理 */
    CaptchaSession get(String id);

    void remove(String id);

    /** 清理所有过期会话 */
    void clearExpired();
}
