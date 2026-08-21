package com.captcha.toolkit.i18n;

import com.captcha.toolkit.model.VerifyResult;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 多语言消息提供者测试：默认中文、按 Locale 切换英文、缺失编码回退编码本身。
 */
class ResourceBundleMessageProviderTest {

    private final MessageProvider provider = new ResourceBundleMessageProvider();

    @Test
    void resolvesChineseByDefault() {
        assertEquals("验证通过", provider.get(CaptchaMessages.VERIFY_OK));
    }

    @Test
    void resolvesEnglishByLocale() {
        assertEquals("Verified", provider.get(Locale.ENGLISH, CaptchaMessages.VERIFY_OK));
    }

    @Test
    void missingCodeReturnsCodeAsFallback() {
        assertEquals("unknown.code", provider.get(Locale.ENGLISH, "unknown.code"));
    }

    @Test
    void rateLimitMessageLocalized() {
        assertEquals("请求过于频繁，请稍后再试",
                provider.get(CaptchaMessages.RATE_LIMIT_EXCEEDED));
        assertEquals("Too many requests, please try again later",
                provider.get(Locale.ENGLISH, CaptchaMessages.RATE_LIMIT_EXCEEDED));
    }

    @Test
    void verifyResultLocalizesPerRequest() {
        VerifyResult result = VerifyResult.fail(
                CaptchaMessages.VERIFY_WRONG, "WRONG", provider);
        assertEquals("验证失败，请重试", result.getMessage());
        assertEquals("WRONG", result.getCode());

        result.localize(Locale.ENGLISH, provider);
        assertEquals("Verification failed, please retry", result.getMessage());
        assertEquals("WRONG", result.getCode());
    }
}
