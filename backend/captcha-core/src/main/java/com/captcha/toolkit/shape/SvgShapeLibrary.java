package com.captcha.toolkit.shape;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * SVG 形状资源库：从 classpath {@code captcha-shapes/} 目录加载全部图标形状。
 *
 * <p>资源目录下维护 {@code index.txt}（每行一个 SVG 文件名），文件名约定
 * {@code 中文标签_英文名.svg}；形状名取最后一个下划线后的英文部分，
 * 展示名取中文前缀。资源形状注册在内置形状之后，同名时覆盖内置实现。</p>
 */
public final class SvgShapeLibrary {

    /** 资源目录 */
    private static final String RESOURCE_DIR = "captcha-shapes";

    /** 资源索引文件（每行一个 SVG 文件名） */
    private static final String INDEX_FILE = RESOURCE_DIR + "/index.txt";

    /** 加载结果缓存（形状模板解析一次后复用） */
    private static volatile List<PuzzleShape> cached;

    private SvgShapeLibrary() {
    }

    /**
     * 返回资源库中的全部形状（进程内缓存，模板只解析一次）。
     *
     * @return 形状列表；资源缺失时返回空列表
     */
    public static List<PuzzleShape> load() {
        List<PuzzleShape> local = cached;
        if (local != null) {
            return local;
        }
        synchronized (SvgShapeLibrary.class) {
            if (cached == null) {
                cached = loadFromResources();
            }
            return cached;
        }
    }

    /** 从 classpath 资源读取并解析全部形状 */
    private static List<PuzzleShape> loadFromResources() {
        List<PuzzleShape> shapes = new ArrayList<>();
        ClassLoader loader = SvgShapeLibrary.class.getClassLoader();
        try (InputStream in = loader.getResourceAsStream(INDEX_FILE);
             BufferedReader reader = in == null
                     ? null
                     : new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            if (reader == null) {
                return shapes;
            }
            List<String> files = reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .sorted()
                    .toList();
            for (String file : files) {
                try (InputStream svgIn = loader.getResourceAsStream(
                        RESOURCE_DIR + "/" + file)) {
                    if (svgIn == null) {
                        continue;
                    }
                    String svg = new String(svgIn.readAllBytes(), StandardCharsets.UTF_8);
                    shapes.add(PuzzleShapes.fromSvgDocument(
                            shapeName(file), shapeLabel(file), svg));
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("加载内置 SVG 形状资源失败", e);
        }
        return shapes;
    }

    /** 从文件名提取形状名：最后一个下划线后的英文部分 */
    private static String shapeName(String file) {
        String base = baseName(file);
        int idx = base.lastIndexOf('_');
        return idx >= 0 ? base.substring(idx + 1) : base;
    }

    /** 从文件名提取展示名：最后一个下划线前的中文部分 */
    private static String shapeLabel(String file) {
        String base = baseName(file);
        int idx = base.lastIndexOf('_');
        return idx > 0 ? base.substring(0, idx) : base;
    }

    /** 去掉 .svg 后缀 */
    private static String baseName(String file) {
        return file.endsWith(".svg") ? file.substring(0, file.length() - 4) : file;
    }
}
