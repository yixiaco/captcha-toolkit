package com.captcha.toolkit.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 验证码下发载荷（接口响应模型）。
 *
 * <p>不同类型验证码的特定化属性（形状、提示、调试答案等）统一放在泛型
 * {@code data} 中：新增验证码类型时只需定义自己的数据类，不再给本类加字段。</p>
 *
 * @param <T> 类型特定化数据（如 {@link SliderChallengeData} / {@link ClickChallengeData}）
 */
@Data
public class CaptchaChallenge<T> {

    /** 验证码会话 id，前端提交答案时原样带回 */
    private String id;

    /** 验证码类型：slider / click / rotate / curve */
    private String type;

    /** 大图（滑块背景图 / 点选背景图），Base64 Data URI */
    private String image1;

    /** 小图（滑块拼图块），仅滑块类型返回 */
    private String image2;

    /** 图片宽度（服务端像素坐标系） */
    private Integer width;

    /** 图片高度（服务端像素坐标系） */
    private Integer height;

    /** 类型特定化数据：每种验证码类型携带各自的形状/提示/调试答案等字段 */
    private T data;
    /** 扩展元数据：自定义验证码可携带任意附加信息 */
    private Map<String, Object> metadata;

}
