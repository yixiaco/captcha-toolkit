package com.captcha.toolkit.generator;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 生成请求：id + 扩展参数（如 shape）+ 是否返回调试答案。
 */
public class GenerateRequest {

    /** 会话唯一标识 */
    private final String id;

    /** 扩展参数（如滑块 shape） */
    private final Map<String, String> params;

    /** 是否请求调试答案 */
    private final boolean debug;

    /** 设备指纹（原始值，仅用于服务端脱敏哈希，不落库） */
    private final String deviceFingerprint;

    /**
     * @param id     会话唯一标识
     * @param params 扩展参数
     * @param debug  是否请求调试答案
     */
    public GenerateRequest(String id, Map<String, String> params, boolean debug) {
        this(id, params, debug, null);
    }

    /**
     * @param id               会话唯一标识
     * @param params           扩展参数
     * @param debug            是否请求调试答案
     * @param deviceFingerprint 设备指纹（可为 null）
     */
    public GenerateRequest(String id, Map<String, String> params, boolean debug,
                           String deviceFingerprint) {
        this.id = id;
        this.params = params == null ? new LinkedHashMap<>() : new LinkedHashMap<>(params);
        this.debug = debug;
        this.deviceFingerprint = deviceFingerprint;
    }

    /** 返回会话唯一标识 */
    public String getId() {
        return id;
    }

    /** 返回扩展参数（只读副本） */
    public Map<String, String> getParams() {
        return params;
    }

    /** 是否请求调试答案 */
    public boolean isDebug() {
        return debug;
    }

    /** 返回设备指纹（可为 null，表示未采集到指纹） */
    public String getDeviceFingerprint() {
        return deviceFingerprint;
    }
}
