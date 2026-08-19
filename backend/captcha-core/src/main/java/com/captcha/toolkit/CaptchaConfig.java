package com.captcha.toolkit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 验证码引擎配置。
 *
 * <p>所有可调参数集中在这里，Spring Starter 会从 {@code captcha.*} 配置项绑定到本对象；
 * 纯 Java 项目也可以直接 new 出来手工赋值。</p>
 */
public class CaptchaConfig {

    /** 是否允许 debug 参数返回答案（仅建议本地联调开启） */
    private boolean debugEnabled = false;

    private Slider slider = new Slider();

    private Click click = new Click();

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public void setDebugEnabled(boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
    }

    public Slider getSlider() {
        return slider;
    }

    public void setSlider(Slider slider) {
        this.slider = slider;
    }

    public Click getClick() {
        return click;
    }

    public void setClick(Click click) {
        this.click = click;
    }

    /**
     * 滑块验证码参数。
     */
    public static class Slider {

        private int width = 340;
        private int height = 190;
        /** 像素容差（服务端图片坐标系） */
        private double tolerance = 8;
        /** 最短验证耗时，拦截脚本秒答 */
        private long minElapsedMs = 500;
        /** 会话有效期（秒） */
        private long expireSeconds = 300;
        /** 拼图块边长占宽度比例（参考 puzzle_captcha：280 宽对 30） */
        private double pieceSizeRatio = 30.0 / 280.0;
        /** 拼图块允许出现的边界留白 */
        private int margin = 10;
        /** 抗锯齿超采样倍数 */
        private int renderScale = 2;
        /** 缺口白色蒙版 alpha（0-255） */
        private int holeAlpha = 204;
        private float shadowRadius = 6f;
        private float shadowOpacity = 0.8f;
        private float shadowOffsetX = 2f;
        private float shadowOffsetY = -1f;
        /** 默认形状；shape 参数非法或未启用时回退到它 */
        private String defaultShape = "classic";
        /** 对外可用的形状白名单 */
        private List<String> enabledShapes = new ArrayList<>(Arrays.asList(
                "classic", "leaf", "triangle", "circle", "diamond", "star", "heart"));
        /** 小图左右留白，用于容纳柔光投影 */
        private int piecePadding = 8;
        /** 假目标（干扰凹槽）数量：图中会出现多个缺口，但只有真目标能拼合 */
        private int fakeTargetCount = 0;
        /** 假目标与真目标/彼此之间的最小纵向间距（像素），避免落在同一 y 轴 */
        private int fakeTargetMinGap = 24;

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public double getTolerance() {
            return tolerance;
        }

        public void setTolerance(double tolerance) {
            this.tolerance = tolerance;
        }

        public long getMinElapsedMs() {
            return minElapsedMs;
        }

        public void setMinElapsedMs(long minElapsedMs) {
            this.minElapsedMs = minElapsedMs;
        }

        public long getExpireSeconds() {
            return expireSeconds;
        }

        public void setExpireSeconds(long expireSeconds) {
            this.expireSeconds = expireSeconds;
        }

        public double getPieceSizeRatio() {
            return pieceSizeRatio;
        }

        public void setPieceSizeRatio(double pieceSizeRatio) {
            this.pieceSizeRatio = pieceSizeRatio;
        }

        public int getMargin() {
            return margin;
        }

        public void setMargin(int margin) {
            this.margin = margin;
        }

        public int getRenderScale() {
            return renderScale;
        }

        public void setRenderScale(int renderScale) {
            this.renderScale = renderScale;
        }

        public int getHoleAlpha() {
            return holeAlpha;
        }

        public void setHoleAlpha(int holeAlpha) {
            this.holeAlpha = holeAlpha;
        }

        public float getShadowRadius() {
            return shadowRadius;
        }

        public void setShadowRadius(float shadowRadius) {
            this.shadowRadius = shadowRadius;
        }

        public float getShadowOpacity() {
            return shadowOpacity;
        }

        public void setShadowOpacity(float shadowOpacity) {
            this.shadowOpacity = shadowOpacity;
        }

        public float getShadowOffsetX() {
            return shadowOffsetX;
        }

        public void setShadowOffsetX(float shadowOffsetX) {
            this.shadowOffsetX = shadowOffsetX;
        }

        public float getShadowOffsetY() {
            return shadowOffsetY;
        }

        public void setShadowOffsetY(float shadowOffsetY) {
            this.shadowOffsetY = shadowOffsetY;
        }

        public String getDefaultShape() {
            return defaultShape;
        }

        public void setDefaultShape(String defaultShape) {
            this.defaultShape = defaultShape;
        }

        public List<String> getEnabledShapes() {
            return enabledShapes;
        }

        public void setEnabledShapes(List<String> enabledShapes) {
            this.enabledShapes = enabledShapes;
        }

        public int getPiecePadding() {
            return piecePadding;
        }

        public void setPiecePadding(int piecePadding) {
            this.piecePadding = piecePadding;
        }

        public int getFakeTargetCount() {
            return fakeTargetCount;
        }

        public void setFakeTargetCount(int fakeTargetCount) {
            this.fakeTargetCount = fakeTargetCount;
        }

        public int getFakeTargetMinGap() {
            return fakeTargetMinGap;
        }

        public void setFakeTargetMinGap(int fakeTargetMinGap) {
            this.fakeTargetMinGap = fakeTargetMinGap;
        }
    }

    /**
     * 文字点选验证码参数。
     */
    public static class Click {

        private int width = 340;
        private int height = 190;
        private int targetCount = 3;
        private int distractorCount = 5;
        /**
         * 目标文字候选数组：每次生成时随机选一个词组（如“星巴克”→ 星/巴/克），
         * 提示也依次显示这个词组的字；留空则从 charPool 随机选 targetCount 个字。
         */
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

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public int getTargetCount() {
            return targetCount;
        }

        public void setTargetCount(int targetCount) {
            this.targetCount = targetCount;
        }

        public List<String> getTargetText() {
            return targetText;
        }

        public void setTargetText(List<String> targetText) {
            this.targetText = targetText;
        }

        public int getDistractorCount() {
            return distractorCount;
        }

        public void setDistractorCount(int distractorCount) {
            this.distractorCount = distractorCount;
        }

        public double getTolerance() {
            return tolerance;
        }

        public void setTolerance(double tolerance) {
            this.tolerance = tolerance;
        }

        public long getMinElapsedMs() {
            return minElapsedMs;
        }

        public void setMinElapsedMs(long minElapsedMs) {
            this.minElapsedMs = minElapsedMs;
        }

        public long getExpireSeconds() {
            return expireSeconds;
        }

        public void setExpireSeconds(long expireSeconds) {
            this.expireSeconds = expireSeconds;
        }

        public int getFontSizeMin() {
            return fontSizeMin;
        }

        public void setFontSizeMin(int fontSizeMin) {
            this.fontSizeMin = fontSizeMin;
        }

        public int getFontSizeMax() {
            return fontSizeMax;
        }

        public void setFontSizeMax(int fontSizeMax) {
            this.fontSizeMax = fontSizeMax;
        }

        public int getMinSpacing() {
            return minSpacing;
        }

        public void setMinSpacing(int minSpacing) {
            this.minSpacing = minSpacing;
        }

        public List<String> getCharPool() {
            return charPool;
        }

        public void setCharPool(List<String> charPool) {
            this.charPool = charPool;
        }

        public List<String> getFonts() {
            return fonts;
        }

        public void setFonts(List<String> fonts) {
            this.fonts = fonts;
        }

        public double getRotationMax() {
            return rotationMax;
        }

        public void setRotationMax(double rotationMax) {
            this.rotationMax = rotationMax;
        }

        public double getShearMax() {
            return shearMax;
        }

        public void setShearMax(double shearMax) {
            this.shearMax = shearMax;
        }

        public double getAlphaMin() {
            return alphaMin;
        }

        public void setAlphaMin(double alphaMin) {
            this.alphaMin = alphaMin;
        }

        public double getAlphaMax() {
            return alphaMax;
        }

        public void setAlphaMax(double alphaMax) {
            this.alphaMax = alphaMax;
        }

        public double getLightnessDeltaMin() {
            return lightnessDeltaMin;
        }

        public void setLightnessDeltaMin(double lightnessDeltaMin) {
            this.lightnessDeltaMin = lightnessDeltaMin;
        }

        public double getLightnessDeltaMax() {
            return lightnessDeltaMax;
        }

        public void setLightnessDeltaMax(double lightnessDeltaMax) {
            this.lightnessDeltaMax = lightnessDeltaMax;
        }

        public double getHueShiftMax() {
            return hueShiftMax;
        }

        public void setHueShiftMax(double hueShiftMax) {
            this.hueShiftMax = hueShiftMax;
        }

        public int getGlyphRenderScale() {
            return glyphRenderScale;
        }

        public void setGlyphRenderScale(int glyphRenderScale) {
            this.glyphRenderScale = glyphRenderScale;
        }

        public int getPunchHolesMin() {
            return punchHolesMin;
        }

        public void setPunchHolesMin(int punchHolesMin) {
            this.punchHolesMin = punchHolesMin;
        }

        public int getPunchHolesMax() {
            return punchHolesMax;
        }

        public void setPunchHolesMax(int punchHolesMax) {
            this.punchHolesMax = punchHolesMax;
        }

        public double getWarpAmplitude() {
            return warpAmplitude;
        }

        public void setWarpAmplitude(double warpAmplitude) {
            this.warpAmplitude = warpAmplitude;
        }

        public int getCurveCount() {
            return curveCount;
        }

        public void setCurveCount(int curveCount) {
            this.curveCount = curveCount;
        }

        public int getDashCount() {
            return dashCount;
        }

        public void setDashCount(int dashCount) {
            this.dashCount = dashCount;
        }

        public int getDotCount() {
            return dotCount;
        }

        public void setDotCount(int dotCount) {
            this.dotCount = dotCount;
        }

        public double getOcclusionProbability() {
            return occlusionProbability;
        }

        public void setOcclusionProbability(double occlusionProbability) {
            this.occlusionProbability = occlusionProbability;
        }
    }
}
