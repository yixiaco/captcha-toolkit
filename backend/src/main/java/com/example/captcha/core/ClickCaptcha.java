package com.example.captcha.core;

import com.example.captcha.util.SceneBackground;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 文字点选验证码生成。
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
public class ClickCaptcha {

    /** 生成图片宽度 */
    private static final int WIDTH = 340;
    /** 生成图片高度 */
    private static final int HEIGHT = 190;
    /** 候选汉字池 */
    private static final String[] CHAR_POOL = {
            "安", "全", "快", "捷", "智", "能", "验", "证",
            "风", "控", "点", "选", "文", "字", "极", "简"
    };
    /** 装饰/手写字体，缺失时 Java 会自动回退到系统字体 */
    private static final String[] FONTS = {
            "华文行楷", "STXingkai", "华文彩云", "STCaiyun", "华文琥珀", "STHupo",
            "楷体", "KaiTi", "隶书", "LiSu", "幼圆", "YouYuan", "宋体", "SimSun"
    };

    private final Random random = new Random();
    private BufferedImage image;
    /** 目标字坐标（服务端保存，用于校验） */
    private final List<Point> targets = new ArrayList<>();
    /** 提示顺序（返回给前端展示） */
    private final List<String> prompt = new ArrayList<>();
    /** 本次布局的所有字 */
    private final List<Chip> chips = new ArrayList<>();

    /**
     * 单个待绘制的字
     */
    private static class Chip {
        String ch;
        boolean target;
        double x;
        double y;
        int size;
        /** 旋转角度（度） */
        double rotation;
        /** 倾斜错切系数 */
        double shear;
        /** 字形整体透明度 */
        double alpha;
        String font;
        /** 与背景相近的主色 */
        Color textColor;
        /** 同色相浅色，用于字形纵向渐变 */
        Color lightColor;
        /** 深色字用 multiply 混合，浅色字用 screen 混合 */
        boolean dark;
    }

    /**
     * 生成一张完整点选验证码图片
     */
    public void run() {
        // 1. 先画随机风景背景
        image = SceneBackground.create(WIDTH, HEIGHT);
        chips.clear();
        targets.clear();
        prompt.clear();

        // 2. 选 3 个目标字 + 5 个干扰字
        List<String> pool = new ArrayList<>(List.of(CHAR_POOL));
        Collections.shuffle(pool, random);
        List<String> targetChars = pool.subList(0, 3);
        prompt.addAll(targetChars);

        List<String> distractorPool = new ArrayList<>(pool.subList(3, pool.size()));
        Collections.shuffle(distractorPool, random);
        List<String> distractorChars = distractorPool.subList(0, 5);

        // 3. 目标字优先摆放，保证一定出现在图中；干扰字放不下就跳过
        for (String ch : targetChars) {
            Chip chip = tryPlace(ch, true, chips, 300);
            if (chip == null) {
                chip = forcePlace(ch, true);
            }
            chips.add(chip);
            targets.add(new Point((int) chip.x, (int) chip.y));
        }
        for (String ch : distractorChars) {
            Chip chip = tryPlace(ch, false, chips, 160);
            if (chip != null) {
                chips.add(chip);
            }
        }

        // 4. 逐个绘制“脏字形”
        for (Chip chip : chips) {
            drawChip(chip);
        }

        // 5. 用背景色曲线遮挡目标字
        Graphics2D occlusion = image.createGraphics();
        drawOcclusion(occlusion);
        occlusion.dispose();

        // 6. 整图波浪形变
        image = warp(image);

        // 7. 干扰线与噪点
        Graphics2D noise = image.createGraphics();
        drawNoise(noise);
        noise.dispose();
    }

    /**
     * 绘制单个字：阴影 → 变形容器 → multiply/screen 混合
     */
    private void drawChip(Chip chip) {
        // 采样字形上下两片区域的背景平均色：
        // 上端颜色作为字顶部渐变、下端颜色作为字底部渐变，
        // 让字形颜色跟随照片本身的明暗/色相变化，而不是单一取色。
        int radius = chip.size / 2 + 4;
        Color bgTop = sampleArea(chip.x, chip.y - chip.size * 0.5, radius);
        Color bgBottom = sampleArea(chip.x, chip.y + chip.size * 0.5, radius);
        chip.textColor = pickColor(bgTop);
        chip.lightColor = pickColor(bgBottom);
        chip.dark = (brightness(bgTop) + brightness(bgBottom)) / 2f < 0.55f;

        // 高清渲染一个“脏字形”
        BufferedImage glyph = renderGlyph(chip);
        int dx = (int) Math.round(chip.x - glyph.getWidth() / 2.0);
        int dy = (int) Math.round(chip.y - glyph.getHeight() / 2.0);

        // 先画柔光投影，让字稍微浮起来、人眼更好认
        drawShadow(glyph, dx + 2, dy + 1);

        // 再用 multiply / screen 混合进背景，让字形吸收背景纹理
        blendGlyph(glyph, dx, dy, chip.dark, chip.alpha);
    }

    /**
     * 高清渲染单个字，返回带透明通道的字形图。
     * 步骤：画蒙版 → 正弦形变 → 随机断笔 → 同色相渐变着色 → 内部噪点纹理。
     */
    private BufferedImage renderGlyph(Chip chip) {
        // 放大 3 倍渲染，让形变和断笔更平滑
        int scale = 3;
        int size = (chip.size + 26) * scale;

        // 先用白色画字形蒙版
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

        // 像素级正弦位移：笔画不再横平竖直，OCR 的骨架/边缘提取会失效
        mask = warpGlyph(mask, scale);

        // 随机断笔：挖掉几个小洞，人眼能脑补，OCR 会把字符切成碎片
        punchHoles(mask);

        // 着色：用与背景相近的色相做纵向渐变
        BufferedImage glyph = colorize(mask, chip.textColor, chip.lightColor);

        // 字形内部叠加黑白噪点，破坏均匀色块
        addGlyphTexture(glyph);
        return glyph;
    }

    /**
     * 对字形蒙版做像素级波浪形变
     */
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

    /**
     * 在字形内部随机挖洞（断笔）
     */
    private void punchHoles(BufferedImage mask) {
        int w = mask.getWidth();
        int h = mask.getHeight();
        int holes = 5 + random.nextInt(5);
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

    /**
     * 用纵向渐变给蒙版着色（destination-in 只保留笔画区域）
     */
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

    /**
     * 字形内部叠加黑白噪点纹理（source-atop 只影响已有笔画像素）
     */
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

    /**
     * 把字形投影画到主图上：先拷贝黑色 alpha，再做盒式模糊
     */
    private void drawShadow(BufferedImage glyph, int dx, int dy) {
        int w = glyph.getWidth();
        int h = glyph.getHeight();
        BufferedImage shadow = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int a = (glyph.getRGB(x, y) >>> 24) & 0xFF;
                if (a > 0) {
                    shadow.setRGB(x, y, (a << 24)); // 黑色 + 字形 alpha
                }
            }
        }
        shadow = blur(shadow, 4);
        Graphics2D g = image.createGraphics();
        g.drawImage(shadow, dx, dy, null);
        g.dispose();
    }

    /**
     * 盒式模糊（近似高斯），用于柔化阴影
     */
    private BufferedImage blur(BufferedImage src, int radius) {
        int size = radius * 2 + 1;
        float[] data = new float[size * size];
        Arrays.fill(data, 1f / (size * size));
        ConvolveOp op = new ConvolveOp(new Kernel(size, size, data), ConvolveOp.EDGE_NO_OP, null);
        return op.filter(src, null);
    }

    /**
     * 手工实现 multiply / screen 混合，让字形颜色和背景照片融合：
     * 深色字 multiply 会变深、浅色字 screen 会变亮，同时保留背景纹理。
     */
    private void blendGlyph(BufferedImage glyph, int dx, int dy, boolean multiply, double alpha) {
        int gw = glyph.getWidth();
        int gh = glyph.getHeight();
        for (int y = 0; y < gh; y++) {
            for (int x = 0; x < gw; x++) {
                int argb = glyph.getRGB(x, y);
                int ga = (argb >>> 24) & 0xFF;
                if (ga == 0) {
                    continue;
                }
                int tx = dx + x;
                int ty = dy + y;
                if (tx < 0 || ty < 0 || tx >= WIDTH || ty >= HEIGHT) {
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

                float a = ga / 255f * (float) alpha;
                int nr = (int) (br * a + dr * (1 - a));
                int ng = (int) (bg * a + dg * (1 - a));
                int nb = (int) (bb * a + db * (1 - a));
                image.setRGB(tx, ty, (nr << 16) | (ng << 8) | nb);
            }
        }
    }

    /**
     * 用接近背景色的曲线穿过目标字，OCR 会把笔画与背景连成一片
     */
    private void drawOcclusion(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (Chip chip : chips) {
            if (!chip.target) {
                continue;
            }
            int count = random.nextDouble() < 0.6 ? 1 : 2;
            for (int i = 0; i < count; i++) {
                int px = clamp((int) Math.round(chip.x), 0, WIDTH - 1);
                int py = clamp((int) Math.round(chip.y), 0, HEIGHT - 1);
                Color bg = new Color(image.getRGB(px, py));
                float[] hsl = rgbToHsl(bg.getRed(), bg.getGreen(), bg.getBlue());
                float nl = clamp(hsl[2] + (float) rand(-0.09, 0.09), 0, 1);
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

    /**
     * 摆放一个字符，保证与已摆放字符保持足够间距
     */
    private Chip tryPlace(String ch, boolean target, List<Chip> placed, int maxAttempts) {
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            // 字号 14~18px，更贴近极验点选字的比例，同时降低 OCR 可读性
            int size = 14 + random.nextInt(5);
            double x = rand(18 + size / 2.0, WIDTH - 18 - size / 2.0);
            double y = rand(24 + size / 2.0, HEIGHT - 12 - size / 2.0);
            boolean clear = true;
            for (Chip p : placed) {
                if (Math.hypot(p.x - x, p.y - y) <= (p.size + size) / 2.0 + 36) {
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

    /**
     * 目标字兜底：即使没有空位也强制放入，保证提示中的字一定在图中
     */
    private Chip forcePlace(String ch, boolean target) {
        int size = 14 + random.nextInt(5);
        return chip(ch, target,
                rand(18 + size / 2.0, WIDTH - 18 - size / 2.0),
                rand(24 + size / 2.0, HEIGHT - 12 - size / 2.0),
                size);
    }

    /**
     * 创建字符对象并随机旋转、倾斜、透明度、字体
     */
    private Chip chip(String ch, boolean target, double x, double y, int size) {
        Chip chip = new Chip();
        chip.ch = ch;
        chip.target = target;
        chip.x = x;
        chip.y = y;
        chip.size = size;
        chip.rotation = rand(-18, 18);
        chip.shear = rand(-0.14, 0.14);
        // 半透明绘制，让字形颜色和背景进一步融合
        chip.alpha = rand(0.78, 0.9);
        chip.font = FONTS[random.nextInt(FONTS.length)];
        return chip;
    }

    /**
     * 取某个圆形区域内的背景平均色，避免单像素取色偏差
     */
    private Color sampleArea(double cx, double cy, int radius) {
        long r = 0;
        long g = 0;
        long b = 0;
        int count = 0;
        int minX = clamp((int) Math.floor(cx - radius), 0, WIDTH - 1);
        int maxX = clamp((int) Math.ceil(cx + radius), 0, WIDTH - 1);
        int minY = clamp((int) Math.floor(cy - radius), 0, HEIGHT - 1);
        int maxY = clamp((int) Math.ceil(cy + radius), 0, HEIGHT - 1);
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
            return new Color(image.getRGB(clamp((int) Math.round(cx), 0, WIDTH - 1),
                    clamp((int) Math.round(cy), 0, HEIGHT - 1)));
        }
        return new Color((int) (r / count), (int) (g / count), (int) (b / count));
    }

    /**
     * 根据背景平均色挑选文字颜色：
     * 明度差只保留 12%~18%，饱和度几乎跟随背景、色相偏移 ±5°，
     * 让人眼能辨认但颜色与背景融为一体，OCR 很难做颜色分割。
     */
    private Color pickColor(Color bg) {
        float[] hsl = rgbToHsl(bg.getRed(), bg.getGreen(), bg.getBlue());
        float shift = hsl[2] > 0.55f
                ? -(0.12f + random.nextFloat() * 0.06f)
                : 0.12f + random.nextFloat() * 0.06f;
        float nl = clamp(hsl[2] + shift, 0.14f, 0.88f);
        float ns = clamp(hsl[1] + (random.nextFloat() - 0.5f) * 0.08f, 0.15f, 0.4f);
        float nh = (hsl[0] + (random.nextFloat() * 10 - 5) + 360) % 360;
        return Color.getHSBColor(nh / 360f, ns, nl);
    }

    /**
     * 颜色明度
     */
    private float brightness(Color c) {
        return Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null)[2];
    }

    /**
     * RGB → HSL
     */
    private float[] rgbToHsl(int r, int g, int b) {
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

    /**
     * 整图波浪形变：背景和文字一起扭曲，进一步破坏 OCR 的版面/字符定位
     */
    private BufferedImage warp(BufferedImage src) {
        double amp = 2.0;
        double freq = 0.08;
        double phase = random.nextDouble() * Math.PI * 2;
        double phase2 = random.nextDouble() * Math.PI * 2;
        BufferedImage out = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int sx = clamp(x + (int) Math.round(Math.sin(y * freq + phase) * amp), 0, WIDTH - 1);
                int sy = clamp(y + (int) Math.round(Math.sin(x * freq * 0.7 + phase2) * amp), 0, HEIGHT - 1);
                out.setRGB(x, y, src.getRGB(sx, sy));
            }
        }
        return out;
    }

    /**
     * 干扰线与噪点：覆盖在整图上，增加 OCR 的文字区域检测难度
     */
    private void drawNoise(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // 随机贝塞尔曲线
        for (int i = 0; i < 24; i++) {
            g.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256),
                    20 + random.nextInt(30)));
            g.setStroke(new BasicStroke((float) rand(1, 2.2)));
            Path2D path = new Path2D.Double();
            path.moveTo(random.nextInt(WIDTH), random.nextInt(HEIGHT));
            path.curveTo(random.nextInt(WIDTH), random.nextInt(HEIGHT),
                    random.nextInt(WIDTH), random.nextInt(HEIGHT),
                    random.nextInt(WIDTH), random.nextInt(HEIGHT));
            g.draw(path);
        }
        // 随机短线片段
        for (int i = 0; i < 12; i++) {
            g.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256),
                    25 + random.nextInt(35)));
            g.setStroke(new BasicStroke((float) rand(1, 2)));
            int x = random.nextInt(WIDTH);
            int y = random.nextInt(HEIGHT);
            int len = 6 + random.nextInt(16);
            g.drawLine(x, y, x + len, y + random.nextInt(7) - 3);
        }
        // 噪点
        for (int i = 0; i < 160; i++) {
            g.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256),
                    12 + random.nextInt(35)));
            int r = 1 + random.nextInt(3);
            g.fillOval(random.nextInt(WIDTH), random.nextInt(HEIGHT), r, r);
        }
    }

    /**
     * 随机浮点数
     */
    private double rand(double min, double max) {
        return min + random.nextDouble() * (max - min);
    }

    /**
     * 随机整数（含边界）
     */
    private int random(int min, int max) {
        return min + random.nextInt(Math.max(1, max - min + 1));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public BufferedImage getImage() {
        return image;
    }

    public List<Point> getTargets() {
        return targets;
    }

    public List<String> getPrompt() {
        return prompt;
    }

    public int getWidth() {
        return WIDTH;
    }

    public int getHeight() {
        return HEIGHT;
    }
}
