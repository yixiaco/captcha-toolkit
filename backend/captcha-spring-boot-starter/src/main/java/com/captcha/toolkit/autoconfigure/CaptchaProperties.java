package com.captcha.toolkit.autoconfigure;

import com.captcha.toolkit.config.BackgroundConfig;
import com.captcha.toolkit.config.BehaviorConfig;
import com.captcha.toolkit.config.CaptchaConfig;
import com.captcha.toolkit.config.ClickConfig;
import com.captcha.toolkit.config.RateLimitConfig;
import com.captcha.toolkit.config.RotateConfig;
import com.captcha.toolkit.config.SliderConfig;
import com.captcha.toolkit.i18n.MessageProvider;
import com.captcha.toolkit.i18n.ResourceBundleMessageProvider;
import lombok.Data;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

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

    /** 验证通过后发放的票据有效期（秒） */
    private long ticketExpireSeconds = 120;

    /** 滑块/通用背景图配置 */
    private BackgroundConfig background = new BackgroundConfig();

    /** 滑块验证码配置 */
    private SliderConfig slider = new SliderConfig();

    /** 点选验证码配置 */
    private ClickConfig click = new ClickConfig();

    /** 图片旋转验证码配置 */
    private RotateConfig rotate = new RotateConfig();

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
        BeanUtils.copyProperties(slider, config.getSlider());
        BeanUtils.copyProperties(click, config.getClick());
        BeanUtils.copyProperties(rotate, config.getRotate());
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
