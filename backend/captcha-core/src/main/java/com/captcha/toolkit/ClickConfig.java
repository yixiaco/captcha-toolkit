package com.captcha.toolkit;

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

    private int width = 340;
    private int height = 190;
    /** 单字模式的目标字数 */
    private int targetCount = 3;
    /** 单字模式的干扰字数 */
    private int distractorCount = 5;
    /** 目标文字候选数组：每次随机选一个（如“星巴克”→ 星/巴/克），留空则随机选字 */
    private List<String> targetText = new ArrayList<>();

    /** 像素容差 */
    private double tolerance = 18;
    private long minElapsedMs = 800;
    private long expireSeconds = 300;
    private int fontSizeMin = 18;
    private int fontSizeMax = 24;
    /** 字符间最小间距 */
    private int minSpacing = 40;
    private List<String> charPool = new ArrayList<>(Arrays.asList(
            "安", "全", "快", "捷", "智", "能", "验", "证",
            "风", "控", "点", "选", "文", "字", "极", "简"));
    private List<String> fonts = new ArrayList<>(Arrays.asList(
            "华文行楷", "STXingkai", "华文彩云", "STCaiyun", "华文琥珀", "STHupo",
            "楷体", "KaiTi", "隶书", "LiSu", "幼圆", "YouYuan", "宋体", "SimSun"));
    private double rotationMax = 18;
    private double shearMax = 0.14;
    private double alphaMin = 0.78;
    private double alphaMax = 0.9;
    /** 字形与背景的明度差范围，越小越难被 OCR 做颜色分割 */
    private double lightnessDeltaMin = 0.12;
    private double lightnessDeltaMax = 0.18;
    private double hueShiftMax = 5;
    /** 字形高清渲染倍率 */
    private int glyphRenderScale = 3;
    private int punchHolesMin = 5;
    private int punchHolesMax = 9;
    private double warpAmplitude = 2.0;
    private int curveCount = 24;
    private int dashCount = 12;
    private int dotCount = 160;
    /** 目标字被背景色遮挡线穿过的概率 */
    private double occlusionProbability = 0.6;

    private static BackgroundConfig emptyBackground() {
        BackgroundConfig background = new BackgroundConfig();
        background.setSources(new ArrayList<>());
        return background;
    }
}
