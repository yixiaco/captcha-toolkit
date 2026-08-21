package com.captcha.toolkit.i18n;

import java.util.Locale;

/**
 * 用户提示消息提供者：按消息编码与语言加载文本。
 *
 * <p>核心引擎不依赖 Spring，默认使用 {@link ResourceBundleMessageProvider}
 * 加载 classpath 下的 {@code captcha-messages*.properties}；Spring 宿主可通过
 * 自定义 {@code MessageProvider} Bean 替换为任意实现（如对接 Spring MessageSource）。</p>
 */
public interface MessageProvider {

    /** 返回当前提供者的默认语言 */
    Locale defaultLocale();

    /** 按默认语言获取消息；编码缺失时原样返回编码 */
    default String get(String code) {
        return get(defaultLocale(), code);
    }

    /**
     * 按指定语言获取消息；编码缺失时原样返回编码，避免返回 null。
     *
     * @param locale 目标语言，null 时使用默认语言
     * @param code   消息编码（见 {@link CaptchaMessages}）
     * @return 本地化后的消息文本
     */
    String get(Locale locale, String code);
}
