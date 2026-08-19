package com.example.captcha.core;

import java.util.List;

/**
 * 验证码接口响应
 */
public class CaptchaVo {

    private String id;
    private String type;
    private String image1;
    private String image2;
    private Integer width;
    private Integer height;
    private String shape;
    private List<String> prompt;
    /** 小图（拼图块）内部左侧留白，前端定位时使用 */
    private Integer pieceOffsetX;
    /** 调试字段：仅 debug=1 时返回滑块答案 x */
    private Integer debugX;
    /** 调试字段：仅 debug=1 时返回点选目标坐标 */
    private List<PointVo> debugTargets;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getImage1() {
        return image1;
    }

    public void setImage1(String image1) {
        this.image1 = image1;
    }

    public String getImage2() {
        return image2;
    }

    public void setImage2(String image2) {
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
}
