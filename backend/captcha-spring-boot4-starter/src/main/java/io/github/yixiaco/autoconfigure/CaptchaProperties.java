package io.github.yixiaco.autoconfigure;

import io.github.yixiaco.config.BackgroundConfig;
import io.github.yixiaco.config.AngleConfig;
import io.github.yixiaco.config.BehaviorConfig;
import io.github.yixiaco.config.CaptchaConfig;
import io.github.yixiaco.config.ClickConfig;
import io.github.yixiaco.config.ClickShapeConfig;
import io.github.yixiaco.config.CurveConfig;
import io.github.yixiaco.config.RateLimitConfig;
import io.github.yixiaco.config.RotateConfig;
import io.github.yixiaco.config.ScratchConfig;
import io.github.yixiaco.config.SlideCurveConfig;
import io.github.yixiaco.config.SliderConfig;
import io.github.yixiaco.config.SwingTileConfig;
import io.github.yixiaco.i18n.MessageProvider;
import io.github.yixiaco.i18n.ResourceBundleMessageProvider;
import lombok.Data;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * captcha.* 配置项。
 *
 * <p>滑块/点选配置复用核心的 {@link SliderConfig} / {@link ClickConfig}，
 * 与 {@link CaptchaConfig} 使用同一套配置类，不再各自维护一份字段。</p>
 *
 * <p>示例：
 * <pre>
 * captcha:
 *   enabled: true
 *   api-prefix: /api/captcha
 *   debug-enabled: false
 *   # 允许下发的类型；空表示全部。非 debug 时后端在此范围内随机挑选，
 *   # debug 时也限定前端可用 type 指定的范围
 *   types:
 *     - slider
 *     - click
 *   background:
 *     sources:
 *       - /images/captcha/default.jpg
 *     generate-fallback: true
 *   slider:
 *     width: 340
 *     height: 190
 *     tolerance: 8
 *   click:
 *     target-text:
 *       - 星巴克
 *       - 麦当劳
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "captcha")
public class CaptchaProperties {

    /** 是否注册 HTTP 接口（纯程序化调用时设为 false） */
    private boolean enabled = true;

    /** 接口前缀 */
    private String apiPrefix = "/api/captcha";

    /** 是否允许 debug=1 返回答案 */
    private boolean debugEnabled = false;

    /** 允许下发的验证码类型编码；为空表示全部已注册类型 */
    private List<String> types = new ArrayList<>();

    /** 验证通过后发放的票据有效期（秒） */
    private long ticketExpireSeconds = 120;

    /** 滑块/通用背景图配置 */
    private BackgroundConfig background = new BackgroundConfig();

    /** 滑块验证码配置 */
    private SliderConfig slider = new SliderConfig();

    /** 点选验证码配置 */
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

    /** 行为轨迹校验配置 */
    private BehaviorConfig behavior = new BehaviorConfig();

    /** 默认提示语言（如 zh_CN / en），用于解析用户提示消息资源 */
    private String locale = "zh_CN";

    /** 设备维度高频请求限流配置 */
    private RateLimitConfig rateLimit = new RateLimitConfig();

    /**
     * 转换成核心引擎配置。滑块/点选直接复用同一套配置对象，
     * 通过属性拷贝避免两处配置实例互相共享可变引用。
     */
    public CaptchaConfig toConfig() {
        CaptchaConfig config = new CaptchaConfig();
        config.setDebugEnabled(debugEnabled);
        config.setTicketExpireSeconds(ticketExpireSeconds);
        config.setTypes(List.copyOf(types));
        BeanUtils.copyProperties(slider, config.getSlider());
        BeanUtils.copyProperties(click, config.getClick());
        BeanUtils.copyProperties(clickShape, config.getClickShape());
        BeanUtils.copyProperties(rotate, config.getRotate());
        BeanUtils.copyProperties(angle, config.getAngle());
        BeanUtils.copyProperties(scratch, config.getScratch());
        BeanUtils.copyProperties(curve, config.getCurve());
        BeanUtils.copyProperties(slideCurve, config.getSlideCurve());
        BeanUtils.copyProperties(swingTile, config.getSwingTile());
        BeanUtils.copyProperties(behavior, config.getBehavior());
        BeanUtils.copyProperties(rateLimit, config.getRateLimit());
        config.setMessageProvider(new ResourceBundleMessageProvider(parseLocale(locale)));
        return config;
    }

    /** 把配置的 locale 字符串解析为 Locale；非法/为空时回退中文 */
    private static Locale parseLocale(String value) {
        if (value == null || value.isBlank()) {
            return Locale.SIMPLIFIED_CHINESE;
        }
        return Locale.forLanguageTag(value.trim().replace('_', '-'));
    }
}
