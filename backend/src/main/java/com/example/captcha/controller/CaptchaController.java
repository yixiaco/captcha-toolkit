package com.example.captcha.controller;

import com.example.captcha.core.CaptchaResult;
import com.example.captcha.core.CaptchaSession;
import com.example.captcha.core.CaptchaStore;
import com.example.captcha.core.CaptchaVo;
import com.example.captcha.core.ClickCaptcha;
import com.example.captcha.core.PointVo;
import com.example.captcha.core.PuzzleCaptcha;
import com.example.captcha.core.PuzzleShape;
import com.example.captcha.util.ImageConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 验证码接口：
 * GET  /api/captcha?type=slider|click&shape=classic|...
 * POST /api/captcha/verify
 */
@RestController
@RequestMapping("/api/captcha")
public class CaptchaController {

    @Autowired
    private CaptchaStore store;

    @Value("${captcha.slider-tolerance:8}")
    private double sliderTolerance;

    @Value("${captcha.min-elapsed-ms:500}")
    private long minElapsedMs;

    @Value("${captcha.click-tolerance:18}")
    private double clickTolerance;

    @Value("${captcha.click-min-elapsed-ms:800}")
    private long clickMinElapsedMs;

    @Value("${captcha.expire-seconds:300}")
    private long expireSeconds;

    @GetMapping
    public CaptchaVo captcha(@RequestParam(defaultValue = "slider") String type,
                             @RequestParam(defaultValue = "classic") String shape,
                             @RequestParam(defaultValue = "false") boolean debug) {
        String id = UUID.randomUUID().toString();
        CaptchaVo vo = new CaptchaVo();
        vo.setId(id);
        vo.setType(type);

        if ("click".equals(type)) {
            ClickCaptcha captcha = new ClickCaptcha();
            captcha.run();
            vo.setImage1(ImageConvertUtil.toDataUri(captcha.getImage(), "png"));
            vo.setWidth(captcha.getWidth());
            vo.setHeight(captcha.getHeight());
            vo.setPrompt(captcha.getPrompt());
            if (debug) {
                vo.setDebugTargets(captcha.getTargets().stream()
                        .map(p -> new PointVo(p.x, p.y))
                        .toList());
            }
            store.put(CaptchaSession.click(id, captcha.getWidth(), captcha.getHeight(),
                    captcha.getTargets(), captcha.getPrompt(), expireSeconds * 1000));
            return vo;
        }

        if (!Arrays.asList(PuzzleShape.NAMES).contains(shape)) {
            shape = "classic";
        }
        BufferedImage source = null;
        try (InputStream in = getClass().getResourceAsStream("/images/captcha/default.jpg")) {
            if (in != null) {
                source = ImageIO.read(in);
            }
        } catch (IOException ignored) {
            // 读取失败时 source 保持 null，由 PuzzleCaptcha 使用默认背景图生成方案
        }
        PuzzleCaptcha captcha = new PuzzleCaptcha(source);
        captcha.setShape(shape);
        captcha.run();
        vo.setShape(shape);
        vo.setImage1(ImageConvertUtil.toDataUri(captcha.getArtwork(), "png"));
        vo.setImage2(ImageConvertUtil.toDataUri(captcha.getVacancy(), "png"));
        vo.setWidth(captcha.getWidth());
        vo.setHeight(captcha.getHeight());
        vo.setPieceOffsetX(captcha.getPieceOffsetX());
        if (debug) {
            vo.setDebugX(captcha.getX());
        }
        store.put(CaptchaSession.slider(id, shape, captcha.getX(), captcha.getWidth(),
                captcha.getHeight(), expireSeconds * 1000));
        return vo;
    }

    @PostMapping("/verify")
    public CaptchaResult verify(@RequestBody Map<String, Object> body) {
        String id = str(body.get("id"));
        String type = str(body.get("type"));
        CaptchaSession session = store.get(id);
        if (session == null) {
            return CaptchaResult.fail("验证码已过期，请刷新重试");
        }
        long elapsed = System.currentTimeMillis() - session.getCreatedAt();

        if ("click".equals(type)) {
            if (elapsed < clickMinElapsedMs) {
                store.remove(id);
                return CaptchaResult.fail("验证速度异常");
            }
            Object raw = body.get("points");
            if (!(raw instanceof List<?> points)
                    || points.size() != session.getTargets().size()) {
                return CaptchaResult.fail("参数错误");
            }
            for (int i = 0; i < points.size(); i++) {
                Object rawPoint = points.get(i);
                if (!(rawPoint instanceof Map<?, ?> point)) {
                    return CaptchaResult.fail("参数错误");
                }
                double px = num(point.get("x"));
                double py = num(point.get("y"));
                Point expected = session.getTargets().get(i);
                if (Math.hypot(px - expected.x, py - expected.y) > clickTolerance) {
                    store.remove(id);
                    return CaptchaResult.fail("点击错误，请重试");
                }
            }
            store.remove(id);
            return CaptchaResult.ok("验证通过", true);
        }

        // 滑块
        if (elapsed < minElapsedMs) {
            store.remove(id);
            return CaptchaResult.fail("验证速度异常");
        }
        double clientX = num(body.get("x"));
        int clientWidth = body.get("width") instanceof Number number ? number.intValue() : session.getWidth();
        double ratio = (double) session.getWidth() / clientWidth;
        if (Math.abs(session.getX() - clientX * ratio) <= sliderTolerance * ratio) {
            store.remove(id);
            return CaptchaResult.ok("验证通过", true);
        }
        store.remove(id);
        return CaptchaResult.fail("验证失败，请重试");
    }

    private static String str(Object value) {
        return value == null ? "" : value.toString();
    }

    private static double num(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        return 0;
    }
}
