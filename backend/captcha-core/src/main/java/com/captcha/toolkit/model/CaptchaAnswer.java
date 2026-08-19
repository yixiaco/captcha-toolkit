package com.captcha.toolkit.model;

import java.util.List;

/**
 * 验证码答案（前端提交的载荷）。
 */
public class CaptchaAnswer {

    private String id;
    private String type;
    private Double x;
    private Integer clientWidth;
    private List<PointVo> points;

    public static CaptchaAnswer slider(Double x, Integer clientWidth) {
        CaptchaAnswer answer = new CaptchaAnswer();
        answer.x = x;
        answer.clientWidth = clientWidth;
        return answer;
    }

    public static CaptchaAnswer click(List<PointVo> points) {
        CaptchaAnswer answer = new CaptchaAnswer();
        answer.points = points;
        return answer;
    }

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

    public Double getX() {
        return x;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public Integer getClientWidth() {
        return clientWidth;
    }

    public void setClientWidth(Integer clientWidth) {
        this.clientWidth = clientWidth;
    }

    public List<PointVo> getPoints() {
        return points;
    }

    public void setPoints(List<PointVo> points) {
        this.points = points;
    }
}
