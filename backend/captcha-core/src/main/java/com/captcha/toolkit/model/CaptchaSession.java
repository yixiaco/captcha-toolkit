package com.captcha.toolkit.model;

import com.captcha.toolkit.type.CaptchaType;
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

    /** 曲线绘制验证码期望曲线采样点（像素坐标），其他类型为 null */
    private final List<PointVo> curve;

    /** 刮刮乐图案布局（仅刮刮乐会话使用） */
    private final List<ScratchPatternSpec> scratchPatterns;

    /** 刮刮乐目标图案下标（仅刮刮乐会话使用） */
    private final List<Integer> scratchTargets;

    /** 会话创建时间戳（毫秒） */
    private final long createdAt;

    /** 会话过期时间戳（毫秒） */
    private final long expiresAt;

    /** 私有构造：所有会话统一从这里创建 */
    private CaptchaSession(String id, CaptchaType type, String shape, int x, int y,
                           int width, int height, List<PointVo> targets, List<String> prompt,
                           double rotation, List<PointVo> curve,
                           List<ScratchPatternSpec> scratchPatterns,
                           List<Integer> scratchTargets, long ttlMillis) {
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
        this.curve = curve;
        this.scratchPatterns = scratchPatterns;
        this.scratchTargets = scratchTargets;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.expiresAt = now + ttlMillis;
    }

    /** 创建滑块会话 */
    public static CaptchaSession slider(String id, String shape, int x, int y,
                                        int width, int height, long ttlMillis) {
        return new CaptchaSession(id, CaptchaType.SLIDER, shape, x, y, width, height,
                null, null, 0, null, null, null, ttlMillis);
    }

    /** 创建点选会话 */
    public static CaptchaSession click(String id, int width, int height,
                                       List<PointVo> targets, List<String> prompt, long ttlMillis) {
        return new CaptchaSession(id, CaptchaType.CLICK, null, 0, 0, width, height,
                targets, prompt, 0, null, null, null, ttlMillis);
    }

    /** 创建旋转会话 */
    public static CaptchaSession rotate(String id, int width, int height,
                                        double rotation, long ttlMillis) {
        return new CaptchaSession(id, CaptchaType.ROTATE, null, 0, 0, width, height,
                null, null, rotation, null, null, null, ttlMillis);
    }

    /** 创建角度验证（圆盘旋转）会话 */
    public static CaptchaSession angle(String id, int width, int height,
                                       double rotation, long ttlMillis) {
        return new CaptchaSession(id, CaptchaType.ANGLE, null, 0, 0, width, height,
                null, null, rotation, null, null, null, ttlMillis);
    }

    /** 创建曲线绘制会话 */
    public static CaptchaSession curve(String id, int width, int height,
                                       List<PointVo> curve, long ttlMillis) {
        return new CaptchaSession(id, CaptchaType.CURVE, null, 0, 0, width, height,
                null, null, 0, curve, null, null, ttlMillis);
    }

    /**
     * 创建滑动曲线会话。
     *
     * @param x 摆动答案放大 10000 倍后的整数（0~1 摆动量），避免改动会话模型
     */
    public static CaptchaSession slideCurve(String id, int width, int height,
                                            int x, long ttlMillis) {
        return new CaptchaSession(id, CaptchaType.SLIDE_CURVE, null, x, 0, width, height,
                null, null, 0, null, null, null, ttlMillis);
    }

    /**
     * 创建滑块摆动图块会话。
     *
     * @param x 真凹槽在路径上的位置（0~1）放大 10000 倍后的整数
     */
    public static CaptchaSession swingTile(String id, int width, int height,
                                           int x, long ttlMillis) {
        return new CaptchaSession(id, CaptchaType.SWING_TILE, null, x, 0, width, height,
                null, null, 0, null, null, null, ttlMillis);
    }

    /**
     * 创建刮刮乐会话。
     *
     * @param x             答案滑块位置（0~1 放大 10000 倍后的整数）
     * @param patterns       全部图案布局
     * @param targetIndices  目标图案下标
     */
    public static CaptchaSession scratch(String id, int width, int height, int x,
                                         List<ScratchPatternSpec> patterns,
                                         List<Integer> targetIndices, long ttlMillis) {
        return new CaptchaSession(id, CaptchaType.SCRATCH, null, x, 0, width, height,
                null, null, 0, null, patterns, targetIndices, ttlMillis);
    }

    /** 会话是否已过期 */
    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }
}
