package com.captcha.toolkit.word;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于配置的词组工厂：直接返回配置的静态词组列表。
 */
public class ConfigWordFactory implements WordFactory {

    private final List<String> words;

    public ConfigWordFactory(List<String> words) {
        this.words = words == null ? List.of() : new ArrayList<>(words);
    }

    @Override
    public List<String> getWords() {
        return words;
    }
}
