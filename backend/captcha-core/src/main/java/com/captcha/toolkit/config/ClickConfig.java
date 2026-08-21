package com.captcha.toolkit.config;

import com.captcha.toolkit.util.CharPools;
import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 文字点选验证码配置，CaptchaConfig 与 CaptchaProperties 共用。
 */
@Data
public class ClickConfig {

    /** 点选背景：默认 sources 为空，使用程序生成风景图；也可单独配置图片素材 */
    private BackgroundConfig background = emptyBackground();

    /** 图片宽度 */
    private int width = 340;

    /** 图片高度 */
    private int height = 190;
    /** 单字模式的目标字数 */
    private int targetCount = 3;
    /** 单字模式的干扰字数 */
    private int distractorCount = 5;
    /** 目标文字候选数组：每次随机选一个（如“星巴克”→ 星/巴/克），留空则随机选字 */
    private List<String> targetText = new ArrayList<>();

    /** 像素容差 */
    private double tolerance = 18;

    /** 最短验证耗时（毫秒） */
    private long minElapsedMs = 800;

    /** 会话有效期（秒） */
    private long expireSeconds = 300;

    /** 字号下限 */
    private int fontSizeMin = 18;

    /** 字号上限 */
    private int fontSizeMax = 24;
    /** 字符间最小间距 */
    private int minSpacing = 40;
    /** 汉字字库：默认使用中文常见字符范围（CJK 统一汉字区 U+4E00–U+9FA5） */
    private List<String> charPool = new ArrayList<>(CharPools.commonChinese());

    /** 候选字体列表 */
    private List<String> fonts = new ArrayList<>(Arrays.asList(
            "华文行楷", "STXingkai", "华文彩云", "STCaiyun", "华文琥珀", "STHupo",
            "楷体", "KaiTi", "隶书", "LiSu", "幼圆", "YouYuan", "宋体", "SimSun"));

    /** 字形最大旋转角度（度） */
    private double rotationMax = 18;

    /** 字形最大斜切系数 */
    private double shearMax = 0.14;

    /** 字形最小透明度 */
    private double alphaMin = 0.78;

    /** 字形最大透明度 */
    private double alphaMax = 0.9;
    /** 字形与背景的明度差范围，越小越难被 OCR 做颜色分割 */
    private double lightnessDeltaMin = 0.12;

    /** 字形与背景的明度差上限 */
    private double lightnessDeltaMax = 0.18;

    /** 字形色相偏移上限 */
    private double hueShiftMax = 5;
    /** 字形高清渲染倍率 */
    private int glyphRenderScale = 3;

    /** 字形内断笔洞数量下限 */
    private int punchHolesMin = 5;

    /** 字形内断笔洞数量上限 */
    private int punchHolesMax = 9;

    /** 整图波浪形变幅度 */
    private double warpAmplitude = 2.0;

    /** 干扰曲线数量 */
    private int curveCount = 24;

    /** 干扰短横线数量 */
    private int dashCount = 12;

    /** 噪点数量 */
    private int dotCount = 160;
    /** 目标字被背景色遮挡线穿过的概率 */
    private double occlusionProbability = 0.6;

    /** 构建点选默认背景配置（素材为空，走程序生成） */
    private static BackgroundConfig emptyBackground() {
        BackgroundConfig background = new BackgroundConfig();
        background.setSources(new ArrayList<>());
        return background;
    }
}
