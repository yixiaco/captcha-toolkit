package com.captcha.toolkit.behavior;

import com.captcha.toolkit.config.BehaviorConfig;
import com.captcha.toolkit.config.ClientBehaviorConfig;
import com.captcha.toolkit.model.CaptchaAnswer;
import com.captcha.toolkit.model.CaptchaSession;

import java.util.List;
import java.util.Optional;

/**
 * 行为校验的统一模板：
 *
 * <ol>
 *   <li>解析 td 报文并做通用字段/耗时/坐标检查</li>
 *   <li>调用子类 {@link #validateEvents} 校验事件序列</li>
 *   <li>调用子类 {@link #validateAnswer} 校验与本次答案的关联</li>
 * </ol>
 *
 * <p>新增验证码类型时，继承本类并实现两个抽象方法即可复用全部通用规则。</p>
 */
public abstract class AbstractBehaviorValidator implements BehaviorValidator {

    private final BehaviorConfig config;

    /**
     * @param config 行为校验配置（含分端画像）
     */
    protected AbstractBehaviorValidator(BehaviorConfig config) {
        this.config = config;
    }

    /**
     * 统一校验流程：解析报文 → 通用规则 → 子类事件序列 → 子类答案关联。
     */
    @Override
    public final Optional<String> validate(String td, CaptchaAnswer answer, CaptchaSession session) {
        if (!config.isEnabled()) {
            return Optional.empty();
        }
        if (td == null || td.isBlank()) {
            return Optional.of("缺少行为轨迹 td");
        }
        BehaviorTrace trace;
        try {
            trace = BehaviorTraceCodec.decode(td);
        } catch (IllegalArgumentException e) {
            return Optional.of("行为轨迹格式错误");
        }
        ClientBehaviorConfig profile = config.profileFor(
                answer == null ? null : answer.getClientType());
        Optional<String> common = validateCommon(trace, profile);
        if (common.isPresent()) {
            return common;
        }
        Optional<String> events = validateEvents(trace);
        if (events.isPresent()) {
            return events;
        }
        return validateAnswer(trace, answer, session, profile);
    }

    /** 返回当前校验器使用的行为配置 */
    protected BehaviorConfig config() {
        return config;
    }

    /** 子类校验事件序列（拖拽 vs 点选） */
    protected abstract Optional<String> validateEvents(BehaviorTrace trace);

    /** 子类校验轨迹与提交答案的关联 */
    protected abstract Optional<String> validateAnswer(
            BehaviorTrace trace, CaptchaAnswer answer, CaptchaSession session,
            ClientBehaviorConfig profile);

    /**
     * 通用规则：协议版本、视口尺寸、起止时间、耗时、点数、时间顺序、
     * 坐标范围与相邻点跳跃距离。
     */
    private Optional<String> validateCommon(BehaviorTrace trace, ClientBehaviorConfig profile) {
        if (trace.protocol() != config.getProtocol()) {
            return Optional.of("行为轨迹协议版本不支持");
        }
        if (trace.viewportWidth() <= 0 || trace.viewportHeight() <= 0) {
            return Optional.of("行为轨迹视口尺寸不合法");
        }
        if (trace.startTime() >= trace.endTime()) {
            return Optional.of("行为轨迹时间戳不合法");
        }
        long duration = trace.durationMillis();
        if (duration < profile.getMinDurationMs()) {
            return Optional.of("行为耗时过短");
        }
        if (duration > profile.getMaxDurationMs()) {
            return Optional.of("行为耗时过长");
        }
        List<BehaviorPoint> points = trace.points();
        if (points.isEmpty() || points.size() < profile.getMinPoints()) {
            return Optional.of("行为轨迹点数不足");
        }
        if (points.getFirst().timeMs() < 0 || points.getFirst().timeMs() > 100) {
            return Optional.of("行为轨迹起始时间不合法");
        }
        for (int i = 1; i < points.size(); i++) {
            BehaviorPoint prev = points.get(i - 1);
            BehaviorPoint current = points.get(i);
            if (current.timeMs() < prev.timeMs()) {
                return Optional.of("行为轨迹时间乱序");
            }
            if (current.x() < 0 || current.x() > 1
                    || current.y() < 0 || current.y() > 1) {
                return Optional.of("行为轨迹坐标越界");
            }
            long dt = current.timeMs() - prev.timeMs();
            if (dt > 0) {
                double distance = Math.hypot(
                        current.x() - prev.x(), current.y() - prev.y());
                if (distance > profile.getMaxJumpRatio()) {
                    return Optional.of("行为轨迹跳跃异常");
                }
            }
        }
        return Optional.empty();
    }
}
