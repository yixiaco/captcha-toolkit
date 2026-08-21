package com.captcha.toolkit.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 汉字字库工具。
 */
public final class CharPools {

    /** CJK 统一汉字区起点：一（U+4E00） */
    private static final int CJK_START = 0x4E00;

    /** CJK 统一汉字区终点：龟（U+9FA5），覆盖绝大多数常用汉字 */
    private static final int CJK_END = 0x9FA5;

    /** 预生成的常用汉字只读列表 */
    private static final List<String> COMMON_CHINESE = buildCommonChinese();

    /** 工具类不可实例化 */
    private CharPools() {
    }

    /**
     * 返回中文常见字符范围（U+4E00–U+9FA5）的汉字列表，
     * 用于点选干扰字与随机目标字的默认字库。
     */
    public static List<String> commonChinese() {
        return COMMON_CHINESE;
    }

    /** 构建 U+4E00–U+9FA5 的汉字列表并包装为只读列表 */
    private static List<String> buildCommonChinese() {
        List<String> chars = new ArrayList<>(CJK_END - CJK_START + 1);
        for (int codePoint = CJK_START; codePoint <= CJK_END; codePoint++) {
            chars.add(new String(Character.toChars(codePoint)));
        }
        return Collections.unmodifiableList(chars);
    }
}
