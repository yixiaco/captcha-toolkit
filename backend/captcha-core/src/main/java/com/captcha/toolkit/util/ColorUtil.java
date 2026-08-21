package com.captcha.toolkit.util;

import java.awt.Color;

/**
 * 颜色工具：HSL 转换、明度、clamp。
 */
public final class ColorUtil {

    /** 工具类不可实例化 */
    private ColorUtil() {
    }

    /**
     * 计算颜色明度（HSB 的 Brightness，0~1）。
     *
     * @param c 颜色
     * @return 明度值
     */
    public static float brightness(Color c) {
        return Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null)[2];
    }

    /**
     * RGB 转 HSL。
     *
     * @param r 红（0~255）
     * @param g 绿（0~255）
     * @param b 蓝（0~255）
     * @return [h(0~360), s(0~1), l(0~1)]
     */
    public static float[] rgbToHsl(int r, int g, int b) {
        float rf = r / 255f;
        float gf = g / 255f;
        float bf = b / 255f;
        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float l = (max + min) / 2;
        float s;
        float h;
        if (max == min) {
            s = 0;
            h = 0;
        } else {
            float d = max - min;
            s = l > 0.5f ? d / (2 - max - min) : d / (max + min);
            if (max == rf) {
                h = (gf - bf) / d + (gf < bf ? 6 : 0);
            } else if (max == gf) {
                h = (bf - rf) / d + 2;
            } else {
                h = (rf - gf) / d + 4;
            }
            h *= 60;
        }
        return new float[]{h, s, l};
    }

    /** 整数 clamp：把 value 限制在 [min, max] */
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /** 浮点 clamp：把 value 限制在 [min, max] */
    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
