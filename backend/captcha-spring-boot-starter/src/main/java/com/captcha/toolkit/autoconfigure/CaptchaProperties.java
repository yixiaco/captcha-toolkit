package com.captcha.toolkit.autoconfigure;

import com.captcha.toolkit.CaptchaConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * captcha.* 配置项，与核心 {@link CaptchaConfig} 一一对应。
 *
 * <p>示例：
 * <pre>
 * captcha:
 *   enabled: true
 *   api-prefix: /api/captcha
 *   debug-enabled: false
 *   background:
 *     sources:
 *       - /images/captcha/default.jpg
 *     generate-fallback: true
 *   slider:
 *     width: 340
 *     height: 190
 *     tolerance: 8
 *     enabled-shapes: [classic, leaf]
 *   click:
 *     target-count: 3
 *     distractor-count: 5
 *     tolerance: 18
 * </pre>
 */
@ConfigurationProperties(prefix = "captcha")
public class CaptchaProperties {

    /** 是否注册 HTTP 接口（纯程序化调用时设为 false） */
    private boolean enabled = true;
    private String apiPrefix = "/api/captcha";
    private boolean debugEnabled = false;
    private Background background = new Background();
    private Slider slider = new Slider();
    private Click click = new Click();

    public static class Background {
        private List<String> sources = new ArrayList<>(List.of("/images/captcha/default.jpg"));
        private boolean generateFallback = true;

        public List<String> getSources() {
            return sources;
        }

        public void setSources(List<String> sources) {
            this.sources = sources;
        }

        public boolean isGenerateFallback() {
            return generateFallback;
        }

        public void setGenerateFallback(boolean generateFallback) {
            this.generateFallback = generateFallback;
        }
    }

    public static class Slider {
        private int width = 340;
        private int height = 190;
        private double tolerance = 8;
        private long minElapsedMs = 500;
        private long expireSeconds = 300;
        private double pieceSizeRatio = 30.0 / 280.0;
        private int margin = 10;
        private int renderScale = 2;
        private int holeAlpha = 204;
        private float shadowRadius = 6f;
        private float shadowOpacity = 0.8f;
        private float shadowOffsetX = 2f;
        private float shadowOffsetY = -1f;
        private String defaultShape = "classic";
        private List<String> enabledShapes = new ArrayList<>(Arrays.asList(
                "classic", "leaf", "triangle", "circle", "diamond", "star", "heart"));
        private int piecePadding = 8;

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
    }

    public static class Click {
        /** 点选背景：默认 sources 为空，使用程序生成风景图；也可单独配置图片素材 */
        private Background background = emptyClickBackground();
        private int width = 340;
        private int height = 190;
        private int targetCount = 3;
        private int distractorCount = 5;
        private String targetText = "";
        private double tolerance = 18;
        private long minElapsedMs = 800;
        private long expireSeconds = 300;
        private int fontSizeMin = 14;
        private int fontSizeMax = 18;
        private int minSpacing = 36;
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
        private double lightnessDeltaMin = 0.12;
        private double lightnessDeltaMax = 0.18;
        private double hueShiftMax = 5;
        private int glyphRenderScale = 3;
        private int punchHolesMin = 5;
        private int punchHolesMax = 9;
        private double warpAmplitude = 2.0;
        private int curveCount = 24;
        private int dashCount = 12;
        private int dotCount = 160;
        private double occlusionProbability = 0.6;

        private static Background emptyClickBackground() {
            Background background = new Background();
            background.setSources(new ArrayList<>());
            return background;
        }

        public Background getBackground() {
            return background;
        }

        public void setBackground(Background background) {
            this.background = background;
        }

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

        public String getTargetText() {
            return targetText;
        }

        public void setTargetText(String targetText) {
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiPrefix() {
        return apiPrefix;
    }

    public void setApiPrefix(String apiPrefix) {
        this.apiPrefix = apiPrefix;
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public void setDebugEnabled(boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
    }

    public Background getBackground() {
        return background;
    }

    public void setBackground(Background background) {
        this.background = background;
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

    public CaptchaConfig toConfig() {
        CaptchaConfig config = new CaptchaConfig();
        config.setDebugEnabled(debugEnabled);

        CaptchaConfig.Slider s = config.getSlider();
        s.setWidth(slider.getWidth());
        s.setHeight(slider.getHeight());
        s.setTolerance(slider.getTolerance());
        s.setMinElapsedMs(slider.getMinElapsedMs());
        s.setExpireSeconds(slider.getExpireSeconds());
        s.setPieceSizeRatio(slider.getPieceSizeRatio());
        s.setMargin(slider.getMargin());
        s.setRenderScale(slider.getRenderScale());
        s.setHoleAlpha(slider.getHoleAlpha());
        s.setShadowRadius(slider.getShadowRadius());
        s.setShadowOpacity(slider.getShadowOpacity());
        s.setShadowOffsetX(slider.getShadowOffsetX());
        s.setShadowOffsetY(slider.getShadowOffsetY());
        s.setDefaultShape(slider.getDefaultShape());
        s.setEnabledShapes(new ArrayList<>(slider.getEnabledShapes()));
        s.setPiecePadding(slider.getPiecePadding());

        CaptchaConfig.Click c = config.getClick();
        c.setWidth(click.getWidth());
        c.setHeight(click.getHeight());
        c.setTargetCount(click.getTargetCount());
        c.setDistractorCount(click.getDistractorCount());
        c.setTargetText(click.getTargetText());
        c.setTolerance(click.getTolerance());
        c.setMinElapsedMs(click.getMinElapsedMs());
        c.setExpireSeconds(click.getExpireSeconds());
        c.setFontSizeMin(click.getFontSizeMin());
        c.setFontSizeMax(click.getFontSizeMax());
        c.setMinSpacing(click.getMinSpacing());
        c.setCharPool(new ArrayList<>(click.getCharPool()));
        c.setFonts(new ArrayList<>(click.getFonts()));
        c.setRotationMax(click.getRotationMax());
        c.setShearMax(click.getShearMax());
        c.setAlphaMin(click.getAlphaMin());
        c.setAlphaMax(click.getAlphaMax());
        c.setLightnessDeltaMin(click.getLightnessDeltaMin());
        c.setLightnessDeltaMax(click.getLightnessDeltaMax());
        c.setHueShiftMax(click.getHueShiftMax());
        c.setGlyphRenderScale(click.getGlyphRenderScale());
        c.setPunchHolesMin(click.getPunchHolesMin());
        c.setPunchHolesMax(click.getPunchHolesMax());
        c.setWarpAmplitude(click.getWarpAmplitude());
        c.setCurveCount(click.getCurveCount());
        c.setDashCount(click.getDashCount());
        c.setDotCount(click.getDotCount());
        c.setOcclusionProbability(click.getOcclusionProbability());
        return config;
    }
}
