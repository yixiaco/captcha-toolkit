package com.captcha.toolkit.model;

import lombok.Data;

/**
 * 坐标值对象：滑块答案 / 点选答案 / 调试坐标共用。
 */
@Data
public class PointVo {

    /** 横坐标（图片内部像素坐标） */
    private int x;

    /** 纵坐标（图片内部像素坐标） */
    private int y;

    /** 无参构造（反序列化用） */
    public PointVo() {
    }

    /**
     * @param x 横坐标
     * @param y 纵坐标
     */
    public PointVo(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
