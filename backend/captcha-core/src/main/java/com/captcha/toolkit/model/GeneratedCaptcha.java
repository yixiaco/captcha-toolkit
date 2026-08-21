package com.captcha.toolkit.model;

import lombok.Data;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 生成器内部产物：图片 + 会话 + 类型特定化数据。
 *
 * <p>不同类型验证码的额外字段（形状、提示、调试答案等）统一放在泛型
 * {@code data} 中，新增验证码类型时只需定义自己的数据类，不再往这里加字段。</p>
 *
 * @param <T> 类型特定化数据（如 {@link SliderChallengeData} / {@link ClickChallengeData}）
 */
@Data
public class GeneratedCaptcha<T> {

    /** 本次生成对应的服务端会话 */
    private CaptchaSession session;

    /** 主图（滑块大图 / 点选图） */
    private BufferedImage image1;

    /** 滑块小图（拼图块），点选为 null */
    private BufferedImage image2;

    /** 图片宽度 */
    private Integer width;

    /** 图片高度 */
    private Integer height;

    /** 类型特定化数据：每种验证码类型携带各自的形状/提示/调试答案等字段 */
    private T data;

    /** 扩展元数据：自定义验证码可携带任意附加信息 */
    private final Map<String, Object> metadata = new HashMap<>();

    /** 写入一条扩展元数据 */
    public void putMetadata(String key, Object value) {
        metadata.put(key, value);
    }
}
