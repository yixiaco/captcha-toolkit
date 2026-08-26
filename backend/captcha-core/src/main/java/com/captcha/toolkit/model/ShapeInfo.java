package com.captcha.toolkit.model;

/**
 * 形状信息：名称 + 展示标签，供 /types 接口下发前端形状选择器使用。
 *
 * @param name  形状唯一名称（与 PuzzleShape.getName 一致）
 * @param label 形状展示名称（中文标签）
 */
public record ShapeInfo(String name, String label) {
}
