package com.captcha.toolkit.config;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 背景图配置：滑块与点选共用。
 */
@Data
public class BackgroundConfig {

    /** 背景图资源（classpath 或文件路径），为空时走程序生成 */
    private List<String> sources = new ArrayList<>(List.of("/images/captcha/default.jpg"));

    /** 素材缺失或为空时，是否用程序生成的风景图兜底 */
    private boolean generateFallback = true;
}
