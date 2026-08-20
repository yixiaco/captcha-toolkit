package com.captcha.toolkit.model;

import lombok.Data;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 生成器内部产物：图片 + 会话 + 调试信息。
 * 引擎负责把图片编码为 Data URI 并组装成对外响应。
 */
@Data
public class GeneratedCaptcha {

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

    /** 滑块拼图形状 */
    private String shape;

    /** 点选提示文字 */
    private List<String> prompt;

    /** 滑块小图内部左侧留白 */
    private Integer pieceOffsetX;

    /** 调试：滑块答案 x */
    private Integer debugX;

    /** 调试：点选目标坐标 */
    private List<PointVo> debugTargets;

    /** 调试：滑块假目标坐标 */
    private List<PointVo> debugFakeTargets;

    /** 调试：旋转验证码正确答案角度（度） */
    private Double debugAngle;

    /** 扩展元数据：自定义验证码可携带任意附加信息 */
    private final Map<String, Object> metadata = new HashMap<>();

    public void putMetadata(String key, Object value) {
        metadata.put(key, value);
    }
}
