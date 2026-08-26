package com.captcha.toolkit.shape;

import java.util.ArrayList;
import java.util.List;

/**
 * 形状目录：汇总全部内置与 SVG 资源形状的名称，供配置默认白名单使用。
 */
public final class ShapeCatalog {

    private ShapeCatalog() {
    }

    /**
     * 返回默认启用的全部形状名称（去重保序）。
     *
     * @return 形状名称列表
     */
    public static List<String> defaultEnabledShapes() {
        List<String> names = new ArrayList<>();
        for (PuzzleShape shape : PuzzleShapes.all()) {
            if (!names.contains(shape.getName())) {
                names.add(shape.getName());
            }
        }
        return names;
    }
}
