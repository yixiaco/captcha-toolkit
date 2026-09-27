package io.github.yixiaco.config;

import io.github.yixiaco.i18n.MessageProvider;
import io.github.yixiaco.i18n.ResourceBundleMessageProvider;
import io.github.yixiaco.limit.DeviceRequestLimiter;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * 验证码引擎配置。
 *
 * <p>滑块/点选参数独立成 {@link SliderConfig} / {@link ClickConfig}，
 * 与 Spring 的 {@code CaptchaProperties} 复用同一套配置类；
 * getter/setter 由 Lombok 生成。</p>
 */
@Data
public class CaptchaConfig {

    /** 是否允许 debug 参数返回答案（仅建议本地联调开启） */
    private boolean debugEnabled = false;

    /**
     * 允许下发的验证码类型编码（如 slider / click / click-shape）。
     *
     * <p>为空表示全部已注册类型都参与下发；非空时既限定“后端决定类型”的
     * 候选范围，也限定 debug 模式下前端能显式指定的类型。</p>
     */
    private List<String> types = new ArrayList<>();

    /** 类型随机源；为 null 时引擎每次选择使用 {@link java.util.concurrent.ThreadLocalRandom} */
    private RandomGenerator typeRandom;

    /** 验证通过后发放的票据有效期（秒），供登录等业务接口校验 */
    private long ticketExpireSeconds = 120;

    /** 滑块验证码配置 */
    private SliderConfig slider = new SliderConfig();

    /** 文字点选验证码配置 */
    private ClickConfig click = new ClickConfig();

    /** 图形点选验证码配置 */
    private ClickShapeConfig clickShape = new ClickShapeConfig();

    /** 图片旋转验证码配置 */
    private RotateConfig rotate = new RotateConfig();

    /** 角度验证（圆盘旋转）验证码配置 */
    private AngleConfig angle = new AngleConfig();

    /** 刮刮乐验证码配置 */
    private ScratchConfig scratch = new ScratchConfig();

    /** 曲线绘制验证码配置 */
    private CurveConfig curve = new CurveConfig();

    /** 滑动曲线验证码配置 */
    private SlideCurveConfig slideCurve = new SlideCurveConfig();

    /** 滑块摆动图块验证码配置 */
    private SwingTileConfig swingTile = new SwingTileConfig();

    /** 行为轨迹校验配置（滑块/点选/旋转共用） */
    private BehaviorConfig behavior = new BehaviorConfig();

    /** 用户提示消息提供者（多语言资源加载，默认中文） */
    private MessageProvider messageProvider = new ResourceBundleMessageProvider();

    /** 设备维度高频请求限流配置 */
    private RateLimitConfig rateLimit = new RateLimitConfig();

    /** 设备维度限流器；为 null 时由引擎按 rateLimit 配置创建内存实现 */
    private DeviceRequestLimiter deviceRequestLimiter;
}
