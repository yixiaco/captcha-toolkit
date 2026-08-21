package com.captcha.toolkit.util;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 验证码字体加载器。
 *
 * <p>优先级：配置/调用方指定的字体 → 资源目录内置的开源字体
 * （{@code fonts/}，如 ZCOOL 快乐体，OFL 协议）→ 系统常见中文字体
 * （微软雅黑/黑体/宋体/PingFang/Noto Sans CJK 等）→ 逻辑字体兜底。
 * 解决部署环境（如无中文字体的 Linux 服务器）渲染提示文字出现方块的问题。</p>
 */
public final class CaptchaFonts {

    /** 资源目录内置字体（按优先级排列，取第一个可加载的） */
    private static final List<String> BUNDLED_FONTS = List.of(
            "/fonts/ZCOOLKuaiLe-Regular.ttf");

    /** 系统常见中文字体（按优先级排列） */
    private static final List<String> SYSTEM_CJK_FONTS = List.of(
            "Microsoft YaHei",
            "微软雅黑",
            "SimHei",
            "黑体",
            "SimSun",
            "宋体",
            "PingFang SC",
            "Noto Sans CJK SC",
            "Source Han Sans SC",
            "WenQuanYi Micro Hei",
            "文泉驿微米黑");

    /** 已加载的内置字体（懒加载缓存） */
    private static Font bundledFont;

    /** 已解析的基础字体缓存：key = family|text */
    private static final Map<String, Font> BASE_FONT_CACHE = new HashMap<>();

    private CaptchaFonts() {
    }

    /**
     * 解析一个能完整显示 {@code text} 的字体。
     *
     * @param family 首选字体族（可为 null/空，也可为 SansSerif 等逻辑字体）
     * @param text   需要显示的文字（如单个汉字）
     * @param style  {@link Font#PLAIN} / {@link Font#BOLD} / {@link Font#ITALIC}
     * @param size   字号（像素）
     * @return 可用的字体（一定能显示 text，或最后一个兜底字体）
     */
    public static Font resolve(String family, String text, int style, float size) {
        return baseFont(family, text).deriveFont(style, size);
    }

    /** 获取能显示 text 的基础字体（字号 1），按 family|text 缓存 */
    private static synchronized Font baseFont(String family, String text) {
        String key = (family == null ? "" : family) + "|" + (text == null ? "" : text);
        Font cached = BASE_FONT_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        Font resolved = candidates(family).stream()
                .filter(font -> canDisplay(font, text))
                .findFirst()
                .orElseGet(() -> new Font(Font.SANS_SERIF, Font.PLAIN, 1));
        BASE_FONT_CACHE.put(key, resolved);
        return resolved;
    }

    /** 候选字体列表：首选 → 内置 → 系统 CJK → 逻辑字体 */
    private static List<Font> candidates(String family) {
        List<Font> fonts = new ArrayList<>();
        if (family != null && !family.isBlank()) {
            fonts.add(new Font(family, Font.PLAIN, 1));
        }
        Font bundled = bundledFont();
        if (bundled != null) {
            fonts.add(bundled);
        }
        for (String name : SYSTEM_CJK_FONTS) {
            fonts.add(new Font(name, Font.PLAIN, 1));
        }
        fonts.add(new Font(Font.SANS_SERIF, Font.PLAIN, 1));
        return fonts;
    }

    /** 文字是否全部可显示（-1 表示全部可显示） */
    private static boolean canDisplay(Font font, String text) {
        return text == null || text.isEmpty() || font.canDisplayUpTo(text) == -1;
    }

    /** 懒加载资源目录内置字体；全部缺失时返回 null */
    private static synchronized Font bundledFont() {
        if (bundledFont != null) {
            return bundledFont;
        }
        for (String path : BUNDLED_FONTS) {
            try (InputStream in = CaptchaFonts.class.getResourceAsStream(path)) {
                if (in != null) {
                    bundledFont = Font.createFont(Font.TRUETYPE_FONT, in);
                    return bundledFont;
                }
            } catch (IOException | FontFormatException ignored) {
                // 继续尝试下一个内置字体
            }
        }
        return null;
    }
}
