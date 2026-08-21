package com.captcha.toolkit.limit;

import com.captcha.toolkit.config.RateLimitConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 内存限流器测试：窗口内计数、设备隔离、窗口过期重置。
 */
class InMemoryDeviceRequestLimiterTest {

    @Test
    void allowsUpToMaxThenRejects() {
        RateLimitConfig config = new RateLimitConfig();
        config.setMaxRequests(3);
        config.setWindowSeconds(60);
        InMemoryDeviceRequestLimiter limiter = new InMemoryDeviceRequestLimiter(config);

        assertTrue(limiter.allow("device-a"));
        assertTrue(limiter.allow("device-a"));
        assertTrue(limiter.allow("device-a"));
        assertFalse(limiter.allow("device-a"));
    }

    @Test
    void differentDevicesAreIndependent() {
        RateLimitConfig config = new RateLimitConfig();
        config.setMaxRequests(1);
        config.setWindowSeconds(60);
        InMemoryDeviceRequestLimiter limiter = new InMemoryDeviceRequestLimiter(config);

        assertTrue(limiter.allow("device-a"));
        assertTrue(limiter.allow("device-b"));
        assertFalse(limiter.allow("device-a"));
    }

    @Test
    void windowResetsAfterExpiry() throws InterruptedException {
        RateLimitConfig config = new RateLimitConfig();
        config.setMaxRequests(1);
        config.setWindowSeconds(1);
        InMemoryDeviceRequestLimiter limiter = new InMemoryDeviceRequestLimiter(config);

        assertTrue(limiter.allow("device-a"));
        assertFalse(limiter.allow("device-a"));
        Thread.sleep(1_100);
        assertTrue(limiter.allow("device-a"));
    }
}
