package com.captcha.toolkit.word;

import java.util.List;

/**
 * 词组工厂（工厂模式）：为文字点选提供候选词组。
 *
 * <p>默认实现从 {@code captcha.click.target-text} 配置读取静态词组；
 * 宿主可以注册自己的 Bean（例如从数据库、远程接口动态取词组），
 * 只要实现本接口即可，生成器与校验逻辑完全不用改动。</p>
 */
@FunctionalInterface
public interface WordFactory {

    /**
     * 返回候选词组列表，每次生成时生成器会从中随机选一个作为目标。
     * 返回空列表时，点选自动退回“从字库随机选字”模式。
     */
    List<String> getWords();
}
