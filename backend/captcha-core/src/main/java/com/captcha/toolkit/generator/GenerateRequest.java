package com.captcha.toolkit.generator;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 生成请求：id + 扩展参数（如 shape）+ 是否返回调试答案。
 */
public class GenerateRequest {

    private final String id;
    private final Map<String, String> params;
    private final boolean debug;

    public GenerateRequest(String id, Map<String, String> params, boolean debug) {
        this.id = id;
        this.params = params == null ? new LinkedHashMap<>() : new LinkedHashMap<>(params);
        this.debug = debug;
    }

    public String getId() {
        return id;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public boolean isDebug() {
        return debug;
    }
}
