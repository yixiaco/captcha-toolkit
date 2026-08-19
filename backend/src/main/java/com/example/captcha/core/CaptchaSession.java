package com.example.captcha.core;

import java.awt.Point;
import java.util.List;

/**
 * 一次验证码会话（内存存储）
 */
public class CaptchaSession {

    private final String id;
    private final String type;
    private final String shape;
    private final int x;
    private final int width;
    private final int height;
    private final List<Point> targets;
    private final List<String> prompt;
    private final long createdAt;
    private final long expiresAt;
    private int clickIndex;

    private CaptchaSession(String id, String type, String shape, int x, int width, int height,
                           List<Point> targets, List<String> prompt, long ttlMillis) {
        this.id = id;
        this.type = type;
        this.shape = shape;
        this.x = x;
        this.width = width;
        this.height = height;
        this.targets = targets;
        this.prompt = prompt;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.expiresAt = now + ttlMillis;
    }

    public static CaptchaSession slider(String id, String shape, int x, int width, int height, long ttlMillis) {
        return new CaptchaSession(id, "slider", shape, x, width, height, null, null, ttlMillis);
    }

    public static CaptchaSession click(String id, int width, int height, List<Point> targets,
                                       List<String> prompt, long ttlMillis) {
        return new CaptchaSession(id, "click", null, 0, width, height, targets, prompt, ttlMillis);
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getShape() {
        return shape;
    }

    public int getX() {
        return x;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public List<Point> getTargets() {
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

    public int getClickIndex() {
        return clickIndex;
    }

    public void setClickIndex(int clickIndex) {
        this.clickIndex = clickIndex;
    }
}
