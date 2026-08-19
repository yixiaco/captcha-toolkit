package com.example.captcha.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 滑块无底图时使用默认背景图生成方案的测试
 */
class PuzzleCaptchaTests {

    @Test
    void generatesDefaultBackgroundWhenSourceMissing() {
        PuzzleCaptcha captcha = new PuzzleCaptcha(null);
        captcha.run();

        assertNotNull(captcha.getArtwork(), "大图不应为空");
        assertNotNull(captcha.getVacancy(), "小图不应为空");
        assertEquals(340, captcha.getArtwork().getWidth());
        assertEquals(190, captcha.getArtwork().getHeight());
    }
}
