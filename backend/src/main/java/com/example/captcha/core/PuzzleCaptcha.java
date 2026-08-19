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
        // 参考 puzzle_captcha：280 宽用 30 的拼图块，按宽度等比缩放
        vwh = Math.max(24, (int) Math.round(width * 30.0 / 280.0));
        x = random(vwh, width - vwh - MARGIN);
        y = random(MARGIN, height - vwh - MARGIN);

        Path2D path = PuzzleShape.create(shape, x, y, vwh);
        BufferedImage thumbnail = cover(source, width, height);

        // 小图：拼图块（原图裁剪）
        BufferedImage pieceFull = transparent(width, height);
        Graphics2D vg = pieceFull.createGraphics();
        vg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        vg.setClip(path);
        vg.drawImage(thumbnail, 0, 0, null);
        vg.dispose();

        // 大图：原图 + 缺口白色半透明蒙版 + 内阴影
        BufferedImage artworkFull = transparent(width, height);
        Graphics2D g = artworkFull.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(thumbnail, 0, 0, null);
        g.setClip(path);
        g.setComposite(AlphaComposite.SrcAtop);
        g.setColor(HOLE_COLOR);
        g.fill(path);
        float radius = 6f;
        ShadowFilter shadowFilter = new ShadowFilter(radius, 2f, -1f, 0.8f);
        BufferedImage innerShadow = shadowFilter.filter(ALPHA_FILTER.filter(pieceFull, null), null);
        g.drawImage(innerShadow, 0, 0, null);
        g.dispose();
        artwork = artworkFull;

        // 小图裁剪成竖条（拼图块 + 投影），并记录内部左侧留白
        Rectangle2D bounds = path.getBounds2D();
        int pad = (int) Math.ceil(radius) + 2;
        int cropX = Math.max(0, x - pad);
        int cropW = Math.min(width - cropX, (int) Math.ceil(bounds.getWidth()) + pad * 2);
        BufferedImage strip = pieceFull.getSubimage(cropX, 0, cropW, height);
        vacancy = shadowFilter.filter(strip, null);
        pieceOffsetX = x - cropX;
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
