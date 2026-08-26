package com.captcha.toolkit.shape;

import java.util.List;
import java.util.Random;

/**
 * 拼图形状选择器：统一处理“前端显式指定 / 后端随机 / 非法回退”的解析规则。
 *
 * <p>滑块拼图与滑块摆动图块共用本选择器：未指定或 {@code random} 时从启用白名单
 * 随机选择；显式指定时要求同时命中白名单与注册表；都不满足时回退默认形状，
 * 默认形状不可用时兜底 {@code classic}。</p>
 */
public final class ShapeSelector {

    /** 形状注册表 */
    private final PuzzleShapeRegistry registry;

    /** 对外可用的形状白名单 */
    private final List<String> enabledShapes;

    /** 默认形状 */
    private final String defaultShape;

    /** 随机数源 */
    private final Random random;

    /**
     * @param registry      形状注册表
     * @param enabledShapes 对外可用的形状白名单
     * @param defaultShape  默认形状
     */
    public ShapeSelector(PuzzleShapeRegistry registry,
                         List<String> enabledShapes,
                         String defaultShape) {
        this(registry, enabledShapes, defaultShape, new Random());
    }

    /**
     * @param registry      形状注册表
     * @param enabledShapes 对外可用的形状白名单
     * @param defaultShape  默认形状
     * @param random        随机数源
     */
    public ShapeSelector(PuzzleShapeRegistry registry,
                         List<String> enabledShapes,
                         String defaultShape,
                         Random random) {
        this.registry = registry;
        this.enabledShapes = List.copyOf(enabledShapes);
        this.defaultShape = defaultShape;
        this.random = random;
    }

    /**
     * 解析本次使用的形状名。
     *
     * @param requested 前端指定的形状名；未指定/random 时后端随机选择
     * @return 实际使用的形状名
     */
    public String resolve(String requested) {
        if (requested == null || requested.isBlank()
                || "random".equalsIgnoreCase(requested)) {
            List<String> candidates = enabledShapes.stream()
                    .filter(registry::contains)
                    .toList();
            if (!candidates.isEmpty()) {
                return candidates.get(random.nextInt(candidates.size()));
            }
        }
        if (requested != null && enabledShapes.contains(requested)
                && registry.contains(requested)) {
            return requested;
        }
        if (registry.contains(defaultShape) && enabledShapes.contains(defaultShape)) {
            return defaultShape;
        }
        return "classic";
    }

    /** 返回启用且已注册的形状名称列表 */
    public List<String> getShapeNames() {
        return enabledShapes.stream()
                .filter(registry::contains)
                .toList();
    }
}
