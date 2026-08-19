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

    private final Map<String, PuzzleShape> shapes = new LinkedHashMap<>();

    public PuzzleShapeRegistry() {
        this(List.of());
    }

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

    public void register(PuzzleShape shape) {
        if (shape != null && shape.getName() != null) {
            shapes.put(shape.getName(), shape);
        }
    }

    public boolean contains(String name) {
        return name != null && shapes.containsKey(name);
    }

    public PuzzleShape resolve(String name) {
        PuzzleShape shape = shapes.get(name);
        return shape == null ? shapes.get("classic") : shape;
    }

    public List<String> names() {
        return new ArrayList<>(shapes.keySet());
    }
}
