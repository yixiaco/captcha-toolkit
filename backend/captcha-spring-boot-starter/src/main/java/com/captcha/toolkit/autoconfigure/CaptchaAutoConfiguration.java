package com.captcha.toolkit.autoconfigure;

import com.captcha.toolkit.CaptchaConfig;
import com.captcha.toolkit.CaptchaEngine;
import com.captcha.toolkit.CaptchaFactory;
import com.captcha.toolkit.image.CaptchaImageCodec;
import com.captcha.toolkit.image.DataUriImageCodec;
import com.captcha.toolkit.render.BackgroundProvider;
import com.captcha.toolkit.render.FallbackBackgroundProvider;
import com.captcha.toolkit.store.CaptchaSessionStore;
import com.captcha.toolkit.store.InMemoryCaptchaSessionStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

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

    @Bean
    @ConditionalOnMissingBean
    public CaptchaImageCodec captchaImageCodec() {
        return new DataUriImageCodec();
    }

    @Bean
    @ConditionalOnMissingBean
    public BackgroundProvider captchaBackgroundProvider(CaptchaProperties properties) {
        return FallbackBackgroundProvider.of(
                properties.getBackground().getSources(),
                properties.getBackground().isGenerateFallback());
    }

    @Bean
    @ConditionalOnMissingBean
    public CaptchaSessionStore captchaSessionStore() {
        return new InMemoryCaptchaSessionStore();
    }

    @Bean
    @ConditionalOnMissingBean
    public CaptchaConfig captchaConfig(CaptchaProperties properties) {
        return properties.toConfig();
    }

    @Bean
    @ConditionalOnMissingBean
    public CaptchaEngine captchaEngine(CaptchaConfig config,
                                       CaptchaSessionStore store,
                                       CaptchaImageCodec codec,
                                       BackgroundProvider backgroundProvider,
                                       List<CaptchaFactory> userFactories,
                                       CaptchaProperties properties) {
        // 滑块使用 background.sources 素材 + 生成兜底；点选默认只使用程序生成风景图，
        // 让字在简单背景上更醒目；宿主可通过 click.background.* 为点选单独配置素材。
        BackgroundProvider clickBackgroundProvider = FallbackBackgroundProvider.of(
                properties.getClick().getBackground().getSources(),
                properties.getClick().getBackground().isGenerateFallback());
        return CaptchaEngine.of(config, store, codec,
                userFactories == null ? List.of() : userFactories,
                backgroundProvider, clickBackgroundProvider);
    }

    @Bean
    @ConditionalOnWebApplication
    @ConditionalOnProperty(prefix = "captcha", name = "enabled", havingValue = "true", matchIfMissing = true)
    public CaptchaController captchaController(CaptchaEngine engine, CaptchaProperties properties) {
        return new CaptchaController(engine, properties);
    }
}
