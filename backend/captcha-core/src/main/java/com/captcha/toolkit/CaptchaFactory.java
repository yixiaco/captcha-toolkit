package com.captcha.toolkit;

import com.captcha.toolkit.config.CaptchaConfig;
import com.captcha.toolkit.generator.CaptchaGenerator;

/**
 * 验证码工厂（工厂模式）。
 *
 * <p>每种验证码类型对应一个工厂，工厂负责根据全局配置组装好生成器。
 * 引擎只依赖本接口，新增验证码时注册一个新工厂即可，不改动引擎与控制器。</p>
 */
public interface CaptchaFactory {

    /** 工厂生产的验证码类型 */
    CaptchaType type();

    /** 根据全局配置创建生成器 */
    CaptchaGenerator create(CaptchaConfig config);
}
