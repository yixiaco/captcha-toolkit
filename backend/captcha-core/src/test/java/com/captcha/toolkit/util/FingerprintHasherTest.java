package com.captcha.toolkit.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * 设备指纹哈希测试：相同输入稳定、不同输入/盐互不相同。
 */
class FingerprintHasherTest {

    @Test
    void hashIsStableForSameInput() {
        assertEquals(
                FingerprintHasher.hash("device-a", "salt"),
                FingerprintHasher.hash("device-a", "salt"));
    }

    @Test
    void hashDiffersBySaltAndFingerprint() {
        assertNotEquals(
                FingerprintHasher.hash("device-a", "salt"),
                FingerprintHasher.hash("device-a", "other"));
        assertNotEquals(
                FingerprintHasher.hash("device-a", "salt"),
                FingerprintHasher.hash("device-b", "salt"));
    }

    @Test
    void hashIsSha256Hex() {
        assertEquals(64, FingerprintHasher.hash("device-a", "salt").length());
    }
}
