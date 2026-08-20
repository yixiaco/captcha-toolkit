package com.captcha.toolkit.shape;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 内置形状的几何回归测试。
 */
class PuzzleShapesTest {

    @Test
    void moonIsLeftFacingCrescent() {
        Path2D path = PuzzleShapes.moon().create(0, 0, 100);
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fill(path);
        g.dispose();

        int left = 0;
        int right = 0;
        for (int y = 0; y < 100; y++) {
            for (int x = 0; x < 100; x++) {
                if (((image.getRGB(x, y) >>> 24) & 0xFF) > 0) {
                    if (x < 50) {
                        left++;
                    } else {
                        right++;
                    }
                }
            }
        }
        // 月牙主体应在左半侧（凸面朝左、凹面朝右）
        assertTrue(left > right * 2,
                "月亮应为左侧月牙，实际 left=" + left + ", right=" + right);
    }
}
