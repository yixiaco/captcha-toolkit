package com.captcha.toolkit.i18n;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * 基于 {@link ResourceBundle} 的消息实现：从 classpath 加载
 * {@code captcha-messages*.properties}（默认中文，含英文资源）。
 *
 * <p>语言解析顺序：指定 Locale → 默认语言 → 基础文件；编码缺失时原样返回编码，
 * 避免把 null 返回给前端。</p>
 */
public class ResourceBundleMessageProvider implements MessageProvider {

    /** 消息资源基础名 */
    private final String baseName;

    /** 默认语言 */
    private final Locale defaultLocale;

    /** 使用默认资源名与中文构造 */
    public ResourceBundleMessageProvider() {
        this("captcha-messages", Locale.SIMPLIFIED_CHINESE);
    }

    /**
     * @param defaultLocale 默认语言（如 {@link Locale#SIMPLIFIED_CHINESE} / {@link Locale#ENGLISH}）
     */
    public ResourceBundleMessageProvider(Locale defaultLocale) {
        this("captcha-messages", defaultLocale);
    }

    /**
     * @param baseName      消息资源基础名
     * @param defaultLocale 默认语言
     */
    public ResourceBundleMessageProvider(String baseName, Locale defaultLocale) {
        this.baseName = baseName;
        this.defaultLocale = defaultLocale == null ? Locale.SIMPLIFIED_CHINESE : defaultLocale;
    }

    @Override
    public Locale defaultLocale() {
        return defaultLocale;
    }

    @Override
    public String get(Locale locale, String code) {
        if (code == null) {
            return null;
        }
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(
                    baseName, locale == null ? defaultLocale : locale);
            return bundle.getString(code);
        } catch (MissingResourceException e) {
            return code;
        }
    }
}
