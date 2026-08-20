package com.captcha.toolkit.model;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 生成器内部产物：图片 + 会话 + 调试信息。
 * 引擎负责把图片编码为 Data URI 并组装成对外响应。
 */
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

    /** 扩展元数据：自定义验证码可携带任意附加信息 */
    private final Map<String, Object> metadata = new HashMap<>();

    public CaptchaSession getSession() {
        return session;
    }

    public void setSession(CaptchaSession session) {
        this.session = session;
    }

    public BufferedImage getImage1() {
        return image1;
    }

    public void setImage1(BufferedImage image1) {
        this.image1 = image1;
    }

    public BufferedImage getImage2() {
        return image2;
    }

    public void setImage2(BufferedImage image2) {
        this.image2 = image2;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public String getShape() {
        return shape;
    }

    public void setShape(String shape) {
        this.shape = shape;
    }

    public List<String> getPrompt() {
        return prompt;
    }

    public void setPrompt(List<String> prompt) {
        this.prompt = prompt;
    }

    public Integer getPieceOffsetX() {
        return pieceOffsetX;
    }

    public void setPieceOffsetX(Integer pieceOffsetX) {
        this.pieceOffsetX = pieceOffsetX;
    }

    public Integer getDebugX() {
        return debugX;
    }

    public void setDebugX(Integer debugX) {
        this.debugX = debugX;
    }

    public List<PointVo> getDebugTargets() {
        return debugTargets;
    }

    public void setDebugTargets(List<PointVo> debugTargets) {
        this.debugTargets = debugTargets;
    }

    public List<PointVo> getDebugFakeTargets() {
        return debugFakeTargets;
    }

    public void setDebugFakeTargets(List<PointVo> debugFakeTargets) {
        this.debugFakeTargets = debugFakeTargets;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void putMetadata(String key, Object value) {
        metadata.put(key, value);
    }
}
