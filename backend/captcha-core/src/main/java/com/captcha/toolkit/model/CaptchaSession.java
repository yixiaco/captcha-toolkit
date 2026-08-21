package com.captcha.toolkit.model;

import com.captcha.toolkit.CaptchaType;
import lombok.Data;

import java.util.List;

/**
 * 一次验证码会话：保存服务端答案，交给 {@link com.captcha.toolkit.store.CaptchaSessionStore} 缓存。
 */
@Data
public class CaptchaSession {

    /** 会话唯一标识 */
    private final String id;

    /** 验证码类型 */
    private final CaptchaType type;

    /** 滑块拼图形状（点选为 null） */
    private final String shape;

    /** 滑块正确答案 x（点选为 0） */
    private final int x;

    /** 滑块正确答案 y（点选为 0） */
    private final int y;

    /** 旋转验证码正确答案角度（度），其他类型为 0 */
    private final double rotation;

    /** 服务端图片宽度，用于前端位移比例换算 */
    private final int width;

    /** 服务端图片高度 */
    private final int height;

    /** 点选目标坐标（按点击顺序） */
    private final List<PointVo> targets;

    /** 点选提示文字 */
    private final List<String> prompt;

    /** 会话创建时间戳（毫秒） */
    private final long createdAt;

    /** 会话过期时间戳（毫秒） */
    private final long expiresAt;

    /** 私有构造：所有会话统一从这里创建 */
    private CaptchaSession(String id, CaptchaType type, String shape, int x, int y,
                           int width, int height, List<PointVo> targets, List<String> prompt,
                           double rotation, long ttlMillis) {
        this.id = id;
        this.type = type;
        this.shape = shape;
        this.x = x;
        this.y = y;
        this.rotation = rotation;
        this.width = width;
        this.height = height;
        this.targets = targets;
        this.prompt = prompt;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.expiresAt = now + ttlMillis;
    }

    /** 创建滑块会话 */
    public static CaptchaSession slider(String id, String shape, int x, int y,
                                        int width, int height, long ttlMillis) {
        return new CaptchaSession(id, CaptchaType.SLIDER, shape, x, y, width, height,
                null, null, 0, ttlMillis);
    }

    /** 创建点选会话 */
    public static CaptchaSession click(String id, int width, int height,
                                       List<PointVo> targets, List<String> prompt, long ttlMillis) {
        return new CaptchaSession(id, CaptchaType.CLICK, null, 0, 0, width, height,
                targets, prompt, 0, ttlMillis);
    }

    /** 创建旋转会话 */
    public static CaptchaSession rotate(String id, int width, int height,
                                        double rotation, long ttlMillis) {
        return new CaptchaSession(id, CaptchaType.ROTATE, null, 0, 0, width, height,
                null, null, rotation, ttlMillis);
    }

    /** 会话是否已过期 */
    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }
}
