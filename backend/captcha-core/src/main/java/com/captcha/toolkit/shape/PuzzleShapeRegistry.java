package com.captcha.toolkit.shape;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 拼图形状注册表：内置形状 + 宿主自定义形状。
 */
public class PuzzleShapeRegistry {

    /** 形状名 → 形状 映射（保持注册顺序） */
    private final Map<String, PuzzleShape> shapes = new LinkedHashMap<>();

    /** 使用全部内置形状 */
    public PuzzleShapeRegistry() {
        this(List.of());
    }

    /**
     * @param customShapes 宿主自定义形状，与内置形状合并注册
     */
    public PuzzleShapeRegistry(Collection<PuzzleShape> customShapes) {
        for (PuzzleShape shape : PuzzleShapes.all()) {
            register(shape);
        }
        if (customShapes != null) {
            for (PuzzleShape shape : customShapes) {
                register(shape);
            }
        }
    }

    /** 注册形状；名称为空时忽略 */
    public void register(PuzzleShape shape) {
        if (shape != null && shape.getName() != null) {
            shapes.put(shape.getName(), shape);
        }
    }

    /** 是否已注册指定名称的形状 */
    public boolean contains(String name) {
        return name != null && shapes.containsKey(name);
    }

    /** 按名称解析形状；未找到时回退到 classic */
    public PuzzleShape resolve(String name) {
        PuzzleShape shape = shapes.get(name);
        return shape == null ? shapes.get("classic") : shape;
    }

    /** 返回所有已注册形状的名称（保持注册顺序） */
    public List<String> names() {
        return new ArrayList<>(shapes.keySet());
    }
}
