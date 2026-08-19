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

    private CaptchaSession session;
    private BufferedImage image1;
    private BufferedImage image2;
    private Integer width;
    private Integer height;
    private String shape;
    private List<String> prompt;
    private Integer pieceOffsetX;
    private Integer debugX;
    private List<PointVo> debugTargets;
    private List<PointVo> debugFakeTargets;
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
