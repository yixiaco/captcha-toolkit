package com.captcha.toolkit.model;

/**
 * 坐标值对象：滑块答案 / 点选答案 / 调试坐标共用。
 */
public class PointVo {

    /** 横坐标（图片内部像素坐标） */
    private int x;

    /** 纵坐标（图片内部像素坐标） */
    private int y;

    public PointVo() {
    }

    public PointVo(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }
}
