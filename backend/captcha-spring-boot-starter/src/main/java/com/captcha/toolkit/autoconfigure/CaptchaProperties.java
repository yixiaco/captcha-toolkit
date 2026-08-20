package com.captcha.toolkit.autoconfigure;

import com.captcha.toolkit.BackgroundConfig;
import com.captcha.toolkit.CaptchaConfig;
import com.captcha.toolkit.ClickConfig;
import com.captcha.toolkit.SliderConfig;
import lombok.Data;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

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
        return config;
    }
}
