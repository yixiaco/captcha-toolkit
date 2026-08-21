package com.captcha.toolkit.generator;

import com.captcha.toolkit.CaptchaType;
import com.captcha.toolkit.behavior.BehaviorValidator;
import com.captcha.toolkit.behavior.ClickBehaviorValidator;
import com.captcha.toolkit.config.BehaviorConfig;
import com.captcha.toolkit.config.ClickConfig;
import com.captcha.toolkit.model.CaptchaAnswer;
import com.captcha.toolkit.exception.CaptchaException;
import com.captcha.toolkit.model.CaptchaSession;
import com.captcha.toolkit.model.GeneratedCaptcha;
import com.captcha.toolkit.model.NormalizedPoint;
import com.captcha.toolkit.model.PointVo;
import com.captcha.toolkit.model.VerifyResult;
import com.captcha.toolkit.render.BackgroundProvider;
import com.captcha.toolkit.util.ImageUtil;
import com.captcha.toolkit.util.ColorUtil;
import com.captcha.toolkit.word.WordFactory;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * 文字点选验证码生成器。
 *
 * <p>抗 OCR 思路（与极验类似）：
 * <ul>
 *   <li>使用手写/装饰字体，避免印刷体</li>
 *   <li>每个字先高清渲染，再做像素级正弦形变，破坏笔画直线</li>
 *   <li>在字形内部随机断笔，人眼可脑补补全，OCR 会把一个字切成多段</li>
 *   <li>字形内部叠加黑白噪点纹理，破坏 OCR 的均匀色块分割</li>
 *   <li>颜色从背景采样，深色字用 multiply、浅色字用 screen 与背景融合</li>
 *   <li>用接近背景色的曲线穿过目标字，把笔画和背景连成一片</li>
 *   <li>整图再做一次波浪形变，并叠加干扰线/噪点</li>
 * </ul>
 */
public class ClickCaptchaGenerator extends AbstractCaptchaGenerator {

    /** 点选配置 */
    private final ClickConfig options;

    /** 背景图提供者 */
    private final BackgroundProvider backgroundProvider;

    /** 目标词组工厂 */
    private final WordFactory wordFactory;
    /** 点选行为轨迹校验器 */
    private final BehaviorValidator behaviorValidator;

    /** 随机数源 */
    private final Random random = new Random();

    /** 合成完成的点选图片 */
    private BufferedImage image;
    /** 文字图层：字形/遮挡线先画在这一层，形变后再与背景合成，保证背景不被整图 warp 影响 */
    private BufferedImage textLayer;

    /** 目标文字坐标（按点击顺序） */
    private final List<PointVo> targets = new ArrayList<>();

    /** 点选提示文字 */
    private final List<String> prompt = new ArrayList<>();

    /** 全部待绘制的文字单元（目标字 + 干扰字） */
    private final List<Chip> chips = new ArrayList<>();

    /**
     * 单个待绘制的字。
     */
    private static class Chip {
        /** 文字内容 */
        String ch;

        /** 是否为目标字（干扰字为 false） */
        boolean target;

        /** 中心 x（服务端像素坐标系） */
        double x;

        /** 中心 y（服务端像素坐标系） */
        double y;

        /** 字号 */
        int size;

        /** 旋转角度（度） */
        double rotation;

        /** 斜切系数 */
        double shear;

        /** 透明度 */
        double alpha;

        /** 字体名称 */
        String font;

        /** 文字渐变上端颜色 */
        Color textColor;

        /** 文字渐变下端颜色 */
        Color lightColor;

        /** 是否深色字（决定用 multiply 还是 screen 混合） */
        boolean dark;
    }

    /** 使用默认词组来源与默认（关闭）行为校验构造生成器 */
    public ClickCaptchaGenerator(ClickConfig options, BackgroundProvider backgroundProvider) {
        this(options, backgroundProvider, null);
    }

    /** 使用默认（关闭）行为校验构造生成器 */
    public ClickCaptchaGenerator(ClickConfig options,
                                 BackgroundProvider backgroundProvider,
                                 WordFactory wordFactory) {
        this(options, backgroundProvider, wordFactory,
                new ClickBehaviorValidator(new BehaviorConfig()));
    }

    /**
     * @param options           点选配置
     * @param backgroundProvider 背景图提供者
     * @param wordFactory        目标词组工厂
     * @param behaviorValidator  行为轨迹校验器
     */
    public ClickCaptchaGenerator(ClickConfig options,
                                 BackgroundProvider backgroundProvider,
                                 WordFactory wordFactory,
                                 BehaviorValidator behaviorValidator) {
        this.options = options;
        this.backgroundProvider = backgroundProvider;
        this.wordFactory = wordFactory;
        this.behaviorValidator = behaviorValidator;
    }

    @Override
    public CaptchaType type() {
        return CaptchaType.CLICK;
    }

    @Override
    protected GeneratedCaptcha doGenerate(GenerateRequest request) {
        run();

        CaptchaSession session = CaptchaSession.click(request.getId(),
                options.getWidth(), options.getHeight(), targets, prompt,
                options.getExpireSeconds() * 1000);

        GeneratedCaptcha result = new GeneratedCaptcha();
        result.setSession(session);
        result.setImage1(image);
        result.setWidth(options.getWidth());
        result.setHeight(options.getHeight());
        result.setPrompt(prompt);
        if (request.isDebug()) {
            result.setDebugTargets(new ArrayList<>(targets));
        }
        return result;
    }

    @Override
    protected VerifyResult doVerify(CaptchaSession session, CaptchaAnswer answer) {
        List<NormalizedPoint> points = answer == null ? null : answer.getPoints();
        if (points == null || points.size() != session.getTargets().size()) {
            return VerifyResult.badRequest("参数错误");
        }
        Optional<String> behaviorError = behaviorValidator.validate(
                answer.getTd(), answer, session);
        if (behaviorError.isPresent()) {
            return VerifyResult.fail(behaviorError.get(), "BEHAVIOR");
        }
        // 点选答案是归一化坐标；先换算回服务端像素再做距离校验，保持容差语义不变
        for (int i = 0; i < points.size(); i++) {
            NormalizedPoint actual = points.get(i);
            PointVo expected = session.getTargets().get(i);
            double actualX = actual.x() * session.getWidth();
            double actualY = actual.y() * session.getHeight();
            if (Math.hypot(actualX - expected.getX(), actualY - expected.getY())
                    > options.getTolerance()) {
                return VerifyResult.fail("点击错误，请重试", "WRONG");
            }
        }
        return VerifyResult.ok("验证通过");
    }

    @Override
    protected long minElapsedMs() {
        return options.getMinElapsedMs();
    }

    /**
     * 生成一张完整点选验证码图片。
     */
    private void run() {
        // 背景素材可能是任意尺寸的图片，必须统一等比裁剪到验证码画布大小，
        // 否则文字坐标（340x190 坐标系）会画错位置、整体看起来“找不到字”
        BufferedImage raw = backgroundProvider.provide(options.getWidth(), options.getHeight())
                .orElseThrow(() -> new CaptchaException("没有可用的背景图，请配置 captcha.background.sources 或开启 generate-fallback"));
        image = ImageUtil.cover(raw, options.getWidth(), options.getHeight());
        textLayer = new BufferedImage(options.getWidth(), options.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        chips.clear();
        targets.clear();
        prompt.clear();

        List<String> pool = new ArrayList<>(options.getCharPool());
        Collections.shuffle(pool, random);

        // 优先从 target-text 数组里随机选一个词组作为目标
        // （如“星巴克”→ 星/巴/克）；未配置时退回从字库随机选 targetCount 个字
        List<String> targetChars = new ArrayList<>();
        List<String> targetTexts = wordFactory != null
                ? wordFactory.getWords()
                : options.getTargetText();
        if (targetTexts != null && !targetTexts.isEmpty()) {
            List<String> candidates = new ArrayList<>(targetTexts);
            Collections.shuffle(candidates, random);
            String targetText = candidates.get(0);
            for (int i = 0; i < targetText.length(); i++) {
                targetChars.add(String.valueOf(targetText.charAt(i)));
            }
        } else {
            int targetCount = Math.min(options.getTargetCount(), pool.size());
            targetChars.addAll(pool.subList(0, targetCount));
        }
        prompt.addAll(targetChars);

        // 干扰字排除目标字，避免用户分不清
        List<String> distractorPool = new ArrayList<>(pool);
        distractorPool.removeIf(targetChars::contains);
        Collections.shuffle(distractorPool, random);
        int distractorCount = Math.min(options.getDistractorCount(), distractorPool.size());
        List<String> distractorChars = distractorPool.subList(0, distractorCount);

        // 目标字优先摆放，保证一定出现在图中；干扰字放不下就跳过
        for (String ch : targetChars) {
            Chip chip = tryPlace(ch, true, chips, 300);
            if (chip == null) {
                chip = forcePlace(ch, true);
            }
            chips.add(chip);
            targets.add(new PointVo((int) chip.x, (int) chip.y));
        }
        for (String ch : distractorChars) {
            Chip chip = tryPlace(ch, false, chips, 160);
            if (chip != null) {
                chips.add(chip);
            }
        }

        for (Chip chip : chips) {
            drawChip(chip);
        }

        // 字形与遮挡线全部画在文字图层，背景保持干净
        Graphics2D occlusion = textLayer.createGraphics();
        drawOcclusion(occlusion);
        occlusion.dispose();

        // 只对文字图层做整图波浪形变，再与背景合成
        textLayer = warp(textLayer);
        image = composite(image, textLayer);

        // 干扰线与噪点最后叠加在合成图上
        Graphics2D noise = image.createGraphics();
        drawNoise(noise);
        noise.dispose();
    }

    /** 绘制单个文字：采样背景色、渲染字形、加投影并混合进文字图层 */
    private void drawChip(Chip chip) {
        // 采样字形上下两片区域的背景平均色，让字形颜色跟随照片本身的明暗/色相变化
        int radius = chip.size / 2 + 4;
        Color bgTop = sampleArea(chip.x, chip.y - chip.size * 0.5, radius);
        Color bgBottom = sampleArea(chip.x, chip.y + chip.size * 0.5, radius);
        chip.textColor = pickColor(bgTop);
        chip.lightColor = pickColor(bgBottom);
        chip.dark = (ColorUtil.brightness(bgTop) + ColorUtil.brightness(bgBottom)) / 2f < 0.55f;

        BufferedImage glyph = renderGlyph(chip);
        int dx = (int) Math.round(chip.x - glyph.getWidth() / 2.0);
        int dy = (int) Math.round(chip.y - glyph.getHeight() / 2.0);

        // 先画柔光投影，让字稍微浮起来、人眼更好认
        drawShadow(glyph, dx + 2, dy + 1);
        // 再用 multiply / screen 混合进背景
        blendGlyph(glyph, dx, dy, chip.dark, chip.alpha);
    }

    /**
     * 高清渲染单个字：画蒙版 → 正弦形变 → 随机断笔 → 同色相渐变着色 → 内部噪点纹理。
     */
    private BufferedImage renderGlyph(Chip chip) {
        int scale = Math.max(1, options.getGlyphRenderScale());
        int size = (chip.size + 26) * scale;

        BufferedImage mask = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = mask.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(new Font(chip.font, Font.BOLD, chip.size * scale));
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(chip.ch);
        int baseline = (fm.getAscent() - fm.getDescent()) / 2;
        g.setColor(Color.WHITE);
        g.drawString(chip.ch, (size - tw) / 2f, size / 2f + baseline);
        g.dispose();

        mask = warpGlyph(mask, scale);
        punchHoles(mask);
        BufferedImage glyph = colorize(mask, chip.textColor, chip.lightColor);
        addGlyphTexture(glyph);
        // 高清字形画完后缩回目标字号（chip.size + 留白），
        // 否则 3 倍画布会直接 1:1 贴到图上，字比配置大 3 倍且互相挤压
        int targetSize = chip.size + 26;
        glyph = ImageUtil.scaleDown(glyph, targetSize, targetSize);
        return glyph;
    }

    /** 对单个字形做正弦形变，破坏笔画直线 */
    private BufferedImage warpGlyph(BufferedImage src, int scale) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        double ampX = scale * rand(1.2, 2.2);
        double ampY = scale * rand(1.2, 2.2);
        double freqX = rand(0.04, 0.09);
        double freqY = rand(0.04, 0.09);
        double phaseX = random.nextDouble() * Math.PI * 2;
        double phaseY = random.nextDouble() * Math.PI * 2;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int sx = clamp(x + (int) Math.round(Math.sin(y * freqY + phaseY) * ampX), 0, w - 1);
                int sy = clamp(y + (int) Math.round(Math.sin(x * freqX + phaseX) * ampY), 0, h - 1);
                out.setRGB(x, y, src.getRGB(sx, sy));
            }
        }
        return out;
    }

    /** 在字形笔画内部随机挖洞，模拟手写断笔 */
    private void punchHoles(BufferedImage mask) {
        int w = mask.getWidth();
        int h = mask.getHeight();
        int maxHoles = Math.max(options.getPunchHolesMin(), options.getPunchHolesMax());
        int minHoles = Math.min(options.getPunchHolesMin(), options.getPunchHolesMax());
        int holes = minHoles + random.nextInt(Math.max(1, maxHoles - minHoles + 1));
        int made = 0;
        int attempts = 0;
        Graphics2D g = mask.createGraphics();
        g.setComposite(AlphaComposite.DstOut);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        while (made < holes && attempts < 400) {
            attempts++;
            int x = random.nextInt(w);
            int y = random.nextInt(h);
            // 只在笔画内部挖洞，避免把洞挖到空白区域
            if (((mask.getRGB(x, y) >>> 24) & 0xFF) > 120) {
                int r = 1 + random.nextInt(3);
                g.fillOval(x - r, y - r, r * 2, r * 2);
                made++;
            }
        }
        g.dispose();
    }

    /** 按上下渐变颜色为字形蒙版着色 */
    private BufferedImage colorize(BufferedImage mask, Color c1, Color c2) {
        int w = mask.getWidth();
        int h = mask.getHeight();
        BufferedImage glyph = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = glyph.createGraphics();
        g.setPaint(new GradientPaint(0, 0, c1, 0, h, c2));
        g.fillRect(0, 0, w, h);
        g.setComposite(AlphaComposite.DstIn);
        g.drawImage(mask, 0, 0, null);
        g.dispose();
        return glyph;
    }

    /** 在字形内部叠加黑白噪点纹理，干扰 OCR 色块分割 */
    private void addGlyphTexture(BufferedImage glyph) {
        int w = glyph.getWidth();
        int h = glyph.getHeight();
        Graphics2D g = glyph.createGraphics();
        g.setComposite(AlphaComposite.SrcAtop);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (int i = 0; i < 60; i++) {
            int x = random.nextInt(w);
            int y = random.nextInt(h);
            if (((glyph.getRGB(x, y) >>> 24) & 0xFF) > 0) {
                g.setColor(random.nextBoolean() ? new Color(255, 255, 255, 30 + random.nextInt(25))
                        : new Color(0, 0, 0, 30 + random.nextInt(25)));
                int r = 1 + random.nextInt(2);
                g.fillOval(x - r, y - r, r * 2, r * 2);
            }
        }
        g.dispose();
    }

    /** 为字形绘制柔光投影 */
    private void drawShadow(BufferedImage glyph, int dx, int dy) {
        int w = glyph.getWidth();
        int h = glyph.getHeight();
        BufferedImage shadow = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int a = (glyph.getRGB(x, y) >>> 24) & 0xFF;
                if (a > 0) {
                    shadow.setRGB(x, y, (a << 24));
                }
            }
        }
        shadow = blur(shadow, 4);
        Graphics2D g = textLayer.createGraphics();
        g.drawImage(shadow, dx, dy, null);
        g.dispose();
    }

    /** 盒式模糊：生成柔光投影 */
    private BufferedImage blur(BufferedImage src, int radius) {
        int size = radius * 2 + 1;
        float[] data = new float[size * size];
        Arrays.fill(data, 1f / (size * size));
        ConvolveOp op = new ConvolveOp(new Kernel(size, size, data), ConvolveOp.EDGE_NO_OP, null);
        return op.filter(src, null);
    }

    /**
     * multiply / screen 混合，让字形颜色和背景照片融合，同时保留背景纹理。
     */
    private void blendGlyph(BufferedImage glyph, int dx, int dy, boolean multiply, double alpha) {
        int gw = glyph.getWidth();
        int gh = glyph.getHeight();
        int imgW = textLayer.getWidth();
        int imgH = textLayer.getHeight();
        for (int y = 0; y < gh; y++) {
            for (int x = 0; x < gw; x++) {
                int argb = glyph.getRGB(x, y);
                int ga = (argb >>> 24) & 0xFF;
                if (ga == 0) {
                    continue;
                }
                int tx = dx + x;
                int ty = dy + y;
                if (tx < 0 || ty < 0 || tx >= imgW || ty >= imgH) {
                    continue;
                }
                int gr = (argb >> 16) & 0xFF;
                int gg = (argb >> 8) & 0xFF;
                int gb = argb & 0xFF;
                int drgb = image.getRGB(tx, ty);
                int dr = (drgb >> 16) & 0xFF;
                int dg = (drgb >> 8) & 0xFF;
                int db = drgb & 0xFF;

                int br;
                int bg;
                int bb;
                if (multiply) {
                    br = dr * gr / 255;
                    bg = dg * gg / 255;
                    bb = db * gb / 255;
                } else {
                    br = 255 - (255 - dr) * (255 - gr) / 255;
                    bg = 255 - (255 - dg) * (255 - gg) / 255;
                    bb = 255 - (255 - db) * (255 - gb) / 255;
                }

                // 文字图层保存“混合后的字色 + alpha”，合成阶段再与背景做标准 alpha 混合
                int alphaByte = (int) Math.round(ga / 255f * alpha * 255);
                if (alphaByte <= 0) {
                    continue;
                }
                textLayer.setRGB(tx, ty,
                        (alphaByte << 24) | (br << 16) | (bg << 8) | bb);
            }
        }
    }

    /**
     * 把文字图层按 alpha 合成到干净背景上。
     */
    private BufferedImage composite(BufferedImage bg, BufferedImage text) {
        int w = bg.getWidth();
        int h = bg.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = text.getRGB(x, y);
                int a = (argb >>> 24) & 0xFF;
                if (a == 0) {
                    out.setRGB(x, y, bg.getRGB(x, y));
                    continue;
                }
                int tr = (argb >> 16) & 0xFF;
                int tg = (argb >> 8) & 0xFF;
                int tb = argb & 0xFF;
                if (a == 255) {
                    out.setRGB(x, y, (tr << 16) | (tg << 8) | tb);
                    continue;
                }
                int drgb = bg.getRGB(x, y);
                int dr = (drgb >> 16) & 0xFF;
                int dg = (drgb >> 8) & 0xFF;
                int db = drgb & 0xFF;
                int nr = (tr * a + dr * (255 - a)) / 255;
                int ng = (tg * a + dg * (255 - a)) / 255;
                int nb = (tb * a + db * (255 - a)) / 255;
                out.setRGB(x, y, (nr << 16) | (ng << 8) | nb);
            }
        }
        return out;
    }

    /** 在目标字上绘制接近背景色的遮挡曲线 */
    private void drawOcclusion(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (Chip chip : chips) {
            if (!chip.target) {
                continue;
            }
            int count = random.nextDouble() < options.getOcclusionProbability() ? 1 : 2;
            for (int i = 0; i < count; i++) {
                int px = clamp((int) Math.round(chip.x), 0, image.getWidth() - 1);
                int py = clamp((int) Math.round(chip.y), 0, image.getHeight() - 1);
                Color bg = new Color(image.getRGB(px, py));
                float[] hsl = ColorUtil.rgbToHsl(bg.getRed(), bg.getGreen(), bg.getBlue());
                float nl = ColorUtil.clamp(hsl[2] + (float) rand(-0.09, 0.09), 0, 1);
                Color lineColor = Color.getHSBColor(hsl[0] / 360f, hsl[1], nl);
                g.setColor(new Color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(),
                        70 + random.nextInt(45)));
                g.setStroke(new BasicStroke((float) rand(1.5, 2.8)));

                Path2D path = new Path2D.Double();
                double x0 = chip.x - chip.size * 0.72;
                double y0 = chip.y + rand(-chip.size * 0.4, chip.size * 0.4);
                double x1 = chip.x + chip.size * 0.72;
                double y1 = chip.y + rand(-chip.size * 0.4, chip.size * 0.4);
                path.moveTo(x0, y0);
                path.quadTo(chip.x + rand(-8, 8),
                        chip.y + rand(-chip.size * 0.55, chip.size * 0.55), x1, y1);
                g.draw(path);
            }
        }
    }

    /** 尝试在空白位置放置文字；放不下返回 null */
    private Chip tryPlace(String ch, boolean target, List<Chip> placed, int maxAttempts) {
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            int size = randomSize();
            double x = rand(18 + size / 2.0, options.getWidth() - 18 - size / 2.0);
            double y = rand(24 + size / 2.0, options.getHeight() - 12 - size / 2.0);
            boolean clear = true;
            for (Chip p : placed) {
                if (Math.hypot(p.x - x, p.y - y) <= (p.size + size) / 2.0 + options.getMinSpacing()) {
                    clear = false;
                    break;
                }
            }
            if (clear) {
                return chip(ch, target, x, y, size);
            }
        }
        return null;
    }

    /** 强制放置文字（目标字兜底，允许与已有文字重叠） */
    private Chip forcePlace(String ch, boolean target) {
        int size = randomSize();
        return chip(ch, target,
                rand(18 + size / 2.0, options.getWidth() - 18 - size / 2.0),
                rand(24 + size / 2.0, options.getHeight() - 12 - size / 2.0),
                size);
    }

    /** 创建文字单元并随机化旋转、斜切、透明度与字体 */
    private Chip chip(String ch, boolean target, double x, double y, int size) {
        List<String> fonts = options.getFonts().isEmpty() ? List.of("SansSerif") : options.getFonts();
        Chip chip = new Chip();
        chip.ch = ch;
        chip.target = target;
        chip.x = x;
        chip.y = y;
        chip.size = size;
        chip.rotation = rand(-options.getRotationMax(), options.getRotationMax());
        chip.shear = rand(-options.getShearMax(), options.getShearMax());
        chip.alpha = rand(options.getAlphaMin(), options.getAlphaMax());
        chip.font = fonts.get(random.nextInt(fonts.size()));
        return chip;
    }

    /** 返回 [fontSizeMin, fontSizeMax] 区间内的随机字号 */
    private int randomSize() {
        int min = options.getFontSizeMin();
        int max = Math.max(min, options.getFontSizeMax());
        return min + random.nextInt(max - min + 1);
    }

    /** 采样图片局部区域的平均颜色 */
    private Color sampleArea(double cx, double cy, int radius) {
        long r = 0;
        long g = 0;
        long b = 0;
        int count = 0;
        int minX = clamp((int) Math.floor(cx - radius), 0, image.getWidth() - 1);
        int maxX = clamp((int) Math.ceil(cx + radius), 0, image.getWidth() - 1);
        int minY = clamp((int) Math.floor(cy - radius), 0, image.getHeight() - 1);
        int maxY = clamp((int) Math.ceil(cy + radius), 0, image.getHeight() - 1);
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                int rgb = image.getRGB(x, y);
                r += (rgb >> 16) & 0xFF;
                g += (rgb >> 8) & 0xFF;
                b += rgb & 0xFF;
                count++;
            }
        }
        if (count == 0) {
            return new Color(image.getRGB(
                    clamp((int) Math.round(cx), 0, image.getWidth() - 1),
                    clamp((int) Math.round(cy), 0, image.getHeight() - 1)));
        }
        return new Color((int) (r / count), (int) (g / count), (int) (b / count));
    }

    /**
     * 根据背景平均色挑选文字颜色：明度差只保留配置范围、色相偏移 ±hueShiftMax，
     * 人眼可辨认但颜色与背景融为一体，OCR 很难做颜色分割。
     */
    private Color pickColor(Color bg) {
        float[] hsl = ColorUtil.rgbToHsl(bg.getRed(), bg.getGreen(), bg.getBlue());
        double deltaMin = Math.min(options.getLightnessDeltaMin(), options.getLightnessDeltaMax());
        double deltaMax = Math.max(options.getLightnessDeltaMin(), options.getLightnessDeltaMax());
        float shift = hsl[2] > 0.55f
                ? -(float) rand(deltaMin, deltaMax)
                : (float) rand(deltaMin, deltaMax);
        float nl = ColorUtil.clamp(hsl[2] + shift, 0.14f, 0.88f);
        float ns = ColorUtil.clamp(hsl[1] + (random.nextFloat() - 0.5f) * 0.08f, 0.15f, 0.4f);
        float nh = (float) ((hsl[0] + (random.nextFloat() * 2 - 1) * options.getHueShiftMax() + 360) % 360);
        return Color.getHSBColor(nh / 360f, ns, nl);
    }

    /**
     * 整图波浪形变：背景和文字一起扭曲，进一步破坏 OCR 的版面/字符定位。
     */
    private BufferedImage warp(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        double amp = options.getWarpAmplitude();
        double freq = 0.08;
        double phase = random.nextDouble() * Math.PI * 2;
        double phase2 = random.nextDouble() * Math.PI * 2;
        // 文字图层需要保留 alpha，因此用 ARGB 输出
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int sx = clamp(x + (int) Math.round(Math.sin(y * freq + phase) * amp), 0, w - 1);
                int sy = clamp(y + (int) Math.round(Math.sin(x * freq * 0.7 + phase2) * amp), 0, h - 1);
                out.setRGB(x, y, src.getRGB(sx, sy));
            }
        }
        return out;
    }

    /**
     * 干扰线与噪点：覆盖在整图上，增加 OCR 的文字区域检测难度。
     */
    private void drawNoise(Graphics2D g) {
        int w = image.getWidth();
        int h = image.getHeight();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (int i = 0; i < options.getCurveCount(); i++) {
            g.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256),
                    20 + random.nextInt(30)));
            g.setStroke(new BasicStroke((float) rand(1, 2.2)));
            Path2D path = new Path2D.Double();
            path.moveTo(random.nextInt(w), random.nextInt(h));
            path.curveTo(random.nextInt(w), random.nextInt(h),
                    random.nextInt(w), random.nextInt(h),
                    random.nextInt(w), random.nextInt(h));
            g.draw(path);
        }
        for (int i = 0; i < options.getDashCount(); i++) {
            g.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256),
                    25 + random.nextInt(35)));
            g.setStroke(new BasicStroke((float) rand(1, 2)));
            int x = random.nextInt(w);
            int y = random.nextInt(h);
            int len = 6 + random.nextInt(16);
            g.drawLine(x, y, x + len, y + random.nextInt(7) - 3);
        }
        for (int i = 0; i < options.getDotCount(); i++) {
            g.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256),
                    12 + random.nextInt(35)));
            int r = 1 + random.nextInt(3);
            g.fillOval(random.nextInt(w), random.nextInt(h), r, r);
        }
    }

    /** 返回 [min, max] 区间内的随机浮点数 */
    private double rand(double min, double max) {
        return min + random.nextDouble() * (max - min);
    }

    /** 把整数限制在 [min, max] */
    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
