package com.captcha.toolkit.limit;

import com.captcha.toolkit.config.RateLimitConfig;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于固定窗口的内存限流实现：每个设备一个窗口，窗口内计数达到上限后拒绝。
 *
 * <p>窗口以设备第一次请求的时间为起点，超时后自动重置；为了控制内存占用，
 * 当设备数量超过 {@link #MAX_DEVICES} 时惰性清理长期未活动的窗口。</p>
 *
 * <p>注意：仅适合单实例，多实例部署请用 Redis 等共享实现替换。</p>
 */
public class InMemoryDeviceRequestLimiter implements DeviceRequestLimiter {

    /** 内存中最多缓存的设备窗口数，超出后触发惰性清理 */
    private static final int MAX_DEVICES = 10_000;

    /** 窗口状态：窗口起点 + 窗口内请求计数 + 最近访问时间 */
    private static final class Window {
        long startAt;
        final AtomicInteger count = new AtomicInteger();
        volatile long lastAccessAt;
    }

    /** 限流配置（窗口长度与上限） */
    private final RateLimitConfig config;

    /** 设备哈希 → 窗口状态 */
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    /**
     * @param config 限流配置
     */
    public InMemoryDeviceRequestLimiter(RateLimitConfig config) {
        this.config = config;
    }

    @Override
    public boolean allow(String deviceHash) {
        long now = System.currentTimeMillis();
        long windowMillis = config.getWindowSeconds() * 1000;
        Window window = windows.computeIfAbsent(deviceHash, key -> {
            Window created = new Window();
            created.startAt = now;
            created.lastAccessAt = now;
            return created;
        });
        window.lastAccessAt = now;
        // 当前窗口已过期：重置窗口起点并清零计数
        if (now - window.startAt >= windowMillis) {
            window.startAt = now;
            window.count.set(0);
        }
        int count = window.count.incrementAndGet();
        cleanupIfNeeded(now);
        return count <= config.getMaxRequests();
    }

    /**
     * 设备数量过多时，清理已超过两个窗口周期未活动的设备窗口。
     */
    private void cleanupIfNeeded(long now) {
        if (windows.size() <= MAX_DEVICES) {
            return;
        }
        long staleMillis = config.getWindowSeconds() * 2 * 1000;
        Iterator<Map.Entry<String, Window>> iterator = windows.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Window> entry = iterator.next();
            if (now - entry.getValue().lastAccessAt > staleMillis) {
                iterator.remove();
            }
        }
    }
}
