package com.captcha.toolkit.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 设备指纹脱敏工具：原始指纹拼接盐后做 SHA-256，输出十六进制哈希。
 *
 * <p>后端只保存哈希，不落库原始指纹，避免设备标识被直接关联到个人。</p>
 */
public final class FingerprintHasher {

    private FingerprintHasher() {
    }

    /**
     * 计算脱敏后的设备指纹哈希。
     *
     * @param fingerprint 前端提交的原始设备指纹
     * @param salt        脱敏盐（可为空字符串）
     * @return SHA-256 十六进制字符串（小写）
     */
    public static String hash(String fingerprint, String salt) {
        String raw = fingerprint == null ? "" : fingerprint.trim();
        String input = raw + (salt == null ? "" : salt);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", e);
        }
    }
}
