package com.captcha.toolkit.model;

import com.captcha.toolkit.CaptchaType;

import java.util.List;

/**
 * 一次验证码会话：保存服务端答案，交给 {@link com.captcha.toolkit.store.CaptchaSessionStore} 缓存。
 */
public class CaptchaSession {

    private final String id;
    private final CaptchaType type;
    private final String shape;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final List<PointVo> targets;
    private final List<String> prompt;
    private final long createdAt;
    private final long expiresAt;

    private CaptchaSession(String id, CaptchaType type, String shape, int x, int y,
                           int width, int height, List<PointVo> targets, List<String> prompt,
                           long ttlMillis) {
        this.id = id;
        this.type = type;
        this.shape = shape;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.targets = targets;
        this.prompt = prompt;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.expiresAt = now + ttlMillis;
    }

    public static CaptchaSession slider(String id, String shape, int x, int y,
                                        int width, int height, long ttlMillis) {
        return new CaptchaSession(id, CaptchaType.SLIDER, shape, x, y, width, height,
                null, null, ttlMillis);
    }

    public static CaptchaSession click(String id, int width, int height,
                                       List<PointVo> targets, List<String> prompt, long ttlMillis) {
        return new CaptchaSession(id, CaptchaType.CLICK, null, 0, 0, width, height,
                targets, prompt, ttlMillis);
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

    public String getId() {
        return id;
    }

    public CaptchaType getType() {
        return type;
    }

    public String getShape() {
        return shape;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public List<PointVo> getTargets() {
        return targets;
    }

    public List<String> getPrompt() {
        return prompt;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }
}
