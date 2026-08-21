package com.captcha.toolkit.store;

import com.captcha.toolkit.model.CaptchaSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 默认内存会话存储：线程安全 + 后台守护线程定期清理过期会话。
 */
public class InMemoryCaptchaSessionStore implements CaptchaSessionStore {

    /** 会话 ID → 会话 映射 */
    private final Map<String, CaptchaSession> sessions = new ConcurrentHashMap<>();

    /** 定期清理过期会话的后台线程 */
    private final ScheduledExecutorService cleaner;

    /** 使用默认清理间隔（60 秒） */
    public InMemoryCaptchaSessionStore() {
        this(60_000);
    }

    /**
     * @param cleanupIntervalMillis 过期会话清理间隔（毫秒）
     */
    public InMemoryCaptchaSessionStore(long cleanupIntervalMillis) {
        cleaner = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "captcha-session-cleaner");
            thread.setDaemon(true);
            return thread;
        });
        cleaner.scheduleWithFixedDelay(this::clearExpired,
                cleanupIntervalMillis, cleanupIntervalMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void put(CaptchaSession session) {
        if (session != null) {
            sessions.put(session.getId(), session);
        }
    }

    @Override
    public CaptchaSession get(String id) {
        if (id == null) {
            return null;
        }
        CaptchaSession session = sessions.get(id);
        if (session == null) {
            return null;
        }
        if (session.isExpired()) {
            sessions.remove(id);
            return null;
        }
        return session;
    }

    @Override
    public void remove(String id) {
        if (id != null) {
            sessions.remove(id);
        }
    }

    @Override
    public void clearExpired() {
        long now = System.currentTimeMillis();
        sessions.entrySet().removeIf(entry -> entry.getValue().getExpiresAt() < now);
    }
}
