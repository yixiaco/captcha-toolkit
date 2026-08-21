package com.captcha.toolkit.word;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于配置的词组工厂：直接返回配置的静态词组列表。
 */
public class ConfigWordFactory implements WordFactory {

    /** 配置的静态词组列表 */
    private final List<String> words;

    /**
     * @param words 词组列表；为 null 时使用空列表
     */
    public ConfigWordFactory(List<String> words) {
        this.words = words == null ? List.of() : new ArrayList<>(words);
    }

    @Override
    public List<String> getWords() {
        return words;
    }
}
