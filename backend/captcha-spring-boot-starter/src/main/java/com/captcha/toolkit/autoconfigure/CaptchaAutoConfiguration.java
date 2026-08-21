package com.captcha.toolkit.autoconfigure;

import com.captcha.toolkit.CaptchaEngine;
import com.captcha.toolkit.config.CaptchaConfig;
import com.captcha.toolkit.factory.CaptchaFactory;
import com.captcha.toolkit.image.CaptchaImageCodec;
import com.captcha.toolkit.image.DataUriImageCodec;
import com.captcha.toolkit.i18n.MessageProvider;
import com.captcha.toolkit.i18n.ResourceBundleMessageProvider;
import com.captcha.toolkit.limit.DeviceRequestLimiter;
import com.captcha.toolkit.limit.InMemoryDeviceRequestLimiter;
import com.captcha.toolkit.render.BackgroundProvider;
import com.captcha.toolkit.render.FallbackBackgroundProvider;
import com.captcha.toolkit.store.CaptchaSessionStore;
import com.captcha.toolkit.store.CaptchaTicketStore;
import com.captcha.toolkit.store.InMemoryCaptchaSessionStore;
import com.captcha.toolkit.store.InMemoryCaptchaTicketStore;
import com.captcha.toolkit.word.ConfigWordFactory;
import com.captcha.toolkit.word.WordFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 验证码自动配置：
 *
 * <ul>
 *   <li>宿主不提供 {@link CaptchaImageCodec} / {@link BackgroundProvider} /
 *       {@link CaptchaSessionStore} / {@link CaptchaFactory} / {@link CaptchaEngine} 时，
 *       全部使用默认实现；宿主自定义任意一个 Bean 即可整体替换对应策略。</li>
 *   <li>宿主自定义 {@link CaptchaFactory} 后，对应类型优先使用宿主工厂，
 *       未覆盖的类型继续使用内置工厂。</li>
 * </ul>
 */
@AutoConfiguration
@EnableConfigurationProperties(CaptchaProperties.class)
@ConditionalOnClass(CaptchaEngine.class)
public class CaptchaAutoConfiguration {

    /** 默认图片编码器：Base64 Data URI */
    @Bean
    @ConditionalOnMissingBean
    public CaptchaImageCodec captchaImageCodec() {
        return new DataUriImageCodec();
    }

    /** 默认背景提供者：配置素材 + 程序生成兜底 */
    @Bean
    @ConditionalOnMissingBean
    public BackgroundProvider captchaBackgroundProvider(CaptchaProperties properties) {
        return FallbackBackgroundProvider.of(
                properties.getBackground().getSources(),
                properties.getBackground().isGenerateFallback());
    }

    /** 默认词组工厂：读取 click.target-text 配置 */
    @Bean
    @ConditionalOnMissingBean
    public WordFactory captchaWordFactory(CaptchaProperties properties) {
        // 默认从 click.target-text 配置读取词组；宿主自定义 WordFactory Bean 后自动替换
        return new ConfigWordFactory(new ArrayList<>(properties.getClick().getTargetText()));
    }

    /** 默认会话存储：内存实现 */
    @Bean
    @ConditionalOnMissingBean
    public CaptchaSessionStore captchaSessionStore() {
        return new InMemoryCaptchaSessionStore();
    }

    /** 默认票据存储：内存实现 */
    @Bean
    @ConditionalOnMissingBean
    public CaptchaTicketStore captchaTicketStore() {
        return new InMemoryCaptchaTicketStore();
    }

    /** 默认消息提供者：加载 captcha-messages*.properties，语言由 captcha.locale 控制 */
    @Bean
    @ConditionalOnMissingBean
    public MessageProvider captchaMessageProvider(CaptchaProperties properties) {
        return new ResourceBundleMessageProvider(parseLocale(properties.getLocale()));
    }

    /** 默认设备限流器：内存固定窗口实现，多实例时宿主可替换为 Redis 等共享实现 */
    @Bean
    @ConditionalOnMissingBean
    public DeviceRequestLimiter captchaDeviceRequestLimiter(CaptchaProperties properties) {
        return new InMemoryDeviceRequestLimiter(properties.getRateLimit());
    }

    /** 把 Spring 配置属性转换为核心引擎配置 */
    @Bean
    @ConditionalOnMissingBean
    public CaptchaConfig captchaConfig(CaptchaProperties properties,
                                       MessageProvider messageProvider,
                                       DeviceRequestLimiter deviceRequestLimiter) {
        CaptchaConfig config = properties.toConfig();
        config.setMessageProvider(messageProvider);
        config.setDeviceRequestLimiter(deviceRequestLimiter);
        return config;
    }

    /** 组装验证码引擎：宿主自定义任意依赖 Bean 后自动替换对应策略 */
    @Bean
    @ConditionalOnMissingBean
    public CaptchaEngine captchaEngine(CaptchaConfig config,
                                       CaptchaSessionStore store,
                                       CaptchaImageCodec codec,
                                       BackgroundProvider backgroundProvider,
                                       List<CaptchaFactory> userFactories,
                                       CaptchaProperties properties,
                                       WordFactory wordFactory,
                                       CaptchaTicketStore ticketStore) {
        // 滑块使用 background.sources 素材 + 生成兜底；点选默认只使用程序生成风景图，
        // 让字在简单背景上更醒目；宿主可通过 click.background.* 为点选单独配置素材。
        BackgroundProvider clickBackgroundProvider = FallbackBackgroundProvider.of(
                properties.getClick().getBackground().getSources(),
                properties.getClick().getBackground().isGenerateFallback());
        return CaptchaEngine.of(config, store, codec,
                userFactories == null ? List.of() : userFactories,
                backgroundProvider, clickBackgroundProvider, wordFactory, ticketStore);
    }

    /** 注册验证码 HTTP 控制器 */
    @Bean
    @ConditionalOnWebApplication
    @ConditionalOnProperty(prefix = "captcha", name = "enabled", havingValue = "true", matchIfMissing = true)
    public CaptchaController captchaController(CaptchaEngine engine,
                                               CaptchaProperties properties,
                                               MessageProvider messageProvider) {
        return new CaptchaController(engine, properties, messageProvider);
    }

    /** 把配置的 locale 字符串解析为 Locale；非法/为空时回退中文 */
    private static Locale parseLocale(String value) {
        if (value == null || value.isBlank()) {
            return Locale.SIMPLIFIED_CHINESE;
        }
        return Locale.forLanguageTag(value.trim().replace('_', '-'));
    }
}
