package com.captcha.toolkit.limit;

/**
 * 设备维度请求限流策略。
 *
 * <p>入参必须是脱敏后的设备指纹哈希（由调用方 {@code FingerprintHasher} 生成），
 * 实现方不得保存原始指纹。默认使用 {@link InMemoryDeviceRequestLimiter}，
 * 多实例部署时建议替换为 Redis 等共享实现。</p>
 */
public interface DeviceRequestLimiter {

    /**
     * 检查设备是否允许继续请求，并记录本次请求。
     *
     * @param deviceHash 脱敏后的设备指纹哈希
     * @return true 允许，false 触发限流拒绝
     */
    boolean allow(String deviceHash);
}
