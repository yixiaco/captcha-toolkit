package com.example.captcha.core;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 验证码内存存储，按 id 保存一次会话
 */
@Component
public class CaptchaStore {

    private final Map<String, CaptchaSession> sessions = new ConcurrentHashMap<>();

    @Value("${captcha.expire-seconds:300}")
    private long expireSeconds;

    public void put(CaptchaSession session) {
        sessions.put(session.getId(), session);
    }

    public CaptchaSession get(String id) {
        if (id == null) {
            return null;
        }
        CaptchaSession session = sessions.get(id);
        if (session == null) {
            return null;
        }
        if (session.getExpiresAt() < System.currentTimeMillis()) {
            sessions.remove(id);
            return null;
        }
        return session;
    }

    public void remove(String id) {
        if (id != null) {
            sessions.remove(id);
        }
    }

    @Scheduled(fixedRate = 60000)
    public void cleanup() {
        long now = System.currentTimeMillis();
        sessions.entrySet().removeIf(entry -> entry.getValue().getExpiresAt() < now);
    }
}
