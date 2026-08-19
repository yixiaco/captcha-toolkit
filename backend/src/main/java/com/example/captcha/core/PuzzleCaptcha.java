package com.example.captcha.core;

import com.jhlabs.image.InvertAlphaFilter;
import com.jhlabs.image.ShadowFilter;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * 滑块验证码生成，参考 puzzle_captcha：
 * 大图为原图 + 白色半透明缺口 + 内阴影；小图为拼图块 + 黑色柔光投影。
 */
public class PuzzleCaptcha {

    private static final int DEFAULT_WIDTH = 340;
    private static final int DEFAULT_HEIGHT = 190;
    private static final int MARGIN = 10;
    /**
     * 超采样倍数：先用 2 倍分辨率渲染拼图边缘，再缩小回目标尺寸，
     * 可以让弧形/斜边产生平滑的抗锯齿过渡，避免 1x 直出的锯齿感。
     */
    private static final int RENDER_SCALE = 2;
    /** 缺口填充：白色半透明蒙版（alpha 204） */
    private static final Color HOLE_COLOR = new Color(255, 255, 255, 204);
    private static final InvertAlphaFilter ALPHA_FILTER = new InvertAlphaFilter();

    private final Random random = new Random();

    private int width = DEFAULT_WIDTH;
    private int height = DEFAULT_HEIGHT;
    private int x;
    private int y;
    private int vwh;
    private String shape = "classic";
    private BufferedImage source;
    private BufferedImage artwork;
    private BufferedImage vacancy;
    private int pieceOffsetX;

    public PuzzleCaptcha(BufferedImage source) {
        this.source = source;
    }

    public void run() {
        // 缺少底图时使用默认生成的风景背景
        if (source == null) {
            source = com.example.captcha.util.SceneBackground.create(width, height);
        }
        // 参考 puzzle_captcha：280 宽用 30 的拼图块，按宽度等比缩放
        vwh = Math.max(24, (int) Math.round(width * 30.0 / 280.0));
        x = random(vwh, width - vwh - MARGIN);
        y = random(MARGIN, height - vwh - MARGIN);

        // 超采样：所有绘制都在 2 倍分辨率的画布上进行
        int renderWidth = width * RENDER_SCALE;
        int renderHeight = height * RENDER_SCALE;
        int renderVwh = vwh * RENDER_SCALE;
        Path2D path = PuzzleShape.create(shape, x * RENDER_SCALE, y * RENDER_SCALE, renderVwh);
        BufferedImage thumbnail = cover(source, renderWidth, renderHeight);

        // 小图（高分辨率）：拼图块 = 原图按路径裁剪
        BufferedImage pieceFull = transparent(renderWidth, renderHeight);
        Graphics2D vg = pieceFull.createGraphics();
        vg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        vg.setClip(path);
        vg.drawImage(thumbnail, 0, 0, null);
        vg.dispose();

        // 大图（高分辨率）：原图 + 缺口白色半透明蒙版 + 内阴影
        BufferedImage artworkFull = transparent(renderWidth, renderHeight);
        Graphics2D g = artworkFull.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(thumbnail, 0, 0, null);
        g.setClip(path);
        g.setComposite(AlphaComposite.SrcAtop);
        g.setColor(HOLE_COLOR);
        g.fill(path);
        // 阴影参数同样按超采样倍数放大
        float radius = 6f * RENDER_SCALE;
        ShadowFilter shadowFilter = new ShadowFilter(radius, 2f * RENDER_SCALE, -1f * RENDER_SCALE, 0.8f);
        BufferedImage innerShadow = shadowFilter.filter(ALPHA_FILTER.filter(pieceFull, null), null);
        g.drawImage(innerShadow, 0, 0, null);
        g.dispose();

        // 高分辨率大图缩小回目标尺寸，边缘自动平滑
        artwork = scaleDown(artworkFull, width, height);

        // 小图裁剪成竖条（拼图块 + 投影），并记录内部左侧留白
        Rectangle2D bounds = path.getBounds2D();
        int pad = 8; // 最终尺寸下的左右留白（容纳阴影）
        int cropX = Math.max(0, (x - pad) * RENDER_SCALE);
        int cropW = (int) Math.ceil(bounds.getWidth() / RENDER_SCALE) + pad * 2;
        cropW = Math.min(width - cropX / RENDER_SCALE, cropW);
        int cropWHigh = cropW * RENDER_SCALE;
        BufferedImage strip = pieceFull.getSubimage(cropX, 0, cropWHigh, renderHeight);
        // 先加投影，再缩小回目标尺寸
        BufferedImage stripShadowed = shadowFilter.filter(strip, null);
        vacancy = scaleDown(stripShadowed, cropW, height);
        pieceOffsetX = x - cropX / RENDER_SCALE;
    }

    /**
     * 高质量缩小：双线性插值 + 质量优先渲染，消除高分辨率绘制留下的锯齿
     */
    private BufferedImage scaleDown(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    private BufferedImage cover(BufferedImage src, int w, int h) {
        double scale = Math.max((double) w / src.getWidth(), (double) h / src.getHeight());
        int sw = (int) Math.ceil(src.getWidth() * scale);
        int sh = (int) Math.ceil(src.getHeight() * scale);
        BufferedImage scaled = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_RGB);
        Graphics2D sg = scaled.createGraphics();
        sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        sg.drawImage(src, 0, 0, sw, sh, null);
        sg.dispose();

        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(scaled, (w - sw) / 2, (h - sh) / 2, null);
        g.dispose();
        return out;
    }

    private BufferedImage transparent(int w, int h) {
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    }

    private int random(int min, int max) {
        return min + random.nextInt(Math.max(1, max - min + 1));
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public String getShape() {
        return shape;
    }

    public void setShape(String shape) {
        this.shape = shape;
    }

    public BufferedImage getArtwork() {
        return artwork;
    }

    public BufferedImage getVacancy() {
        return vacancy;
    }

    public int getPieceOffsetX() {
        return pieceOffsetX;
    }
}
