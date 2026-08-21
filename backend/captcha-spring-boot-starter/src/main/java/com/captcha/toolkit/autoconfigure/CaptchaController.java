package com.captcha.toolkit.autoconfigure;

import com.captcha.toolkit.CaptchaEngine;
import com.captcha.toolkit.type.CaptchaType;
import com.captcha.toolkit.exception.RateLimitExceededException;
import com.captcha.toolkit.i18n.CaptchaMessages;
import com.captcha.toolkit.i18n.MessageProvider;
import com.captcha.toolkit.model.CaptchaAnswer;
import com.captcha.toolkit.model.TicketVerifyRequest;
import com.captcha.toolkit.model.VerifyResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 验证码 HTTP 接口（前缀由 captcha.api-prefix 控制，默认 /api/captcha）。
 *
 * <p>GET  {prefix}?type=slider|click&shape=...&debug=1
 * <br>POST {prefix}/verify
 * <br>GET/POST {prefix}/ticket/verify?ticket=...（业务接口校验一次性票据）
 * <br>GET  {prefix}/types
 */
@RestController
@RequestMapping("${captcha.api-prefix:/api/captcha}")
@Validated
public class CaptchaController {

    /** 验证码引擎 */
    private final CaptchaEngine engine;

    /** 验证码配置（读取 debug 开关等） */
    private final CaptchaProperties properties;

    /** 用户提示消息提供者（按请求语言本地化） */
    private final MessageProvider messageProvider;

    /**
     * @param engine          验证码引擎
     * @param properties      验证码配置
     * @param messageProvider 用户提示消息提供者
     */
    public CaptchaController(CaptchaEngine engine,
                             CaptchaProperties properties,
                             MessageProvider messageProvider) {
        this.engine = engine;
        this.properties = properties;
        this.messageProvider = messageProvider;
    }

    /** 下发一张验证码：type 指定类型，shape 指定滑块形状，debug 请求调试答案 */
    @GetMapping
    public Object create(@RequestParam(defaultValue = "slider") String type,
                         @RequestParam(required = false) String shape,
                         @RequestParam(defaultValue = "false") boolean debug,
                         @RequestParam(required = false) String deviceFingerprint,
                         @RequestParam(required = false) String lang,
                         @RequestHeader(name = "Accept-Language", required = false)
                         String acceptLanguage) {
        Map<String, String> params = new LinkedHashMap<>();
        if (shape != null && !shape.isBlank()) {
            params.put("shape", shape);
        }
        try {
            return engine.create(CaptchaType.fromCode(type), params,
                    debug && properties.isDebugEnabled(), deviceFingerprint);
        } catch (RateLimitExceededException e) {
            return VerifyResult.fail(CaptchaMessages.RATE_LIMIT_EXCEEDED,
                    "RATE_LIMITED", messageProvider)
                    .localize(resolveLocale(lang, acceptLanguage), messageProvider);
        }
    }

    /** 校验前端提交的答案 */
    @PostMapping("/verify")
    public VerifyResult verify(@Valid @RequestBody CaptchaAnswer answer,
                               @RequestHeader(name = "Accept-Language", required = false)
                               String acceptLanguage) {
        if (answer == null || answer.getId() == null) {
            return VerifyResult.badRequest(CaptchaMessages.VERIFY_MISSING_ID, messageProvider)
                    .localize(resolveLocale(null, acceptLanguage), messageProvider);
        }
        return engine.verify(answer.getId(), answer)
                .localize(resolveLocale(answer.getLang(), acceptLanguage), messageProvider);
    }

    /** 业务接口校验一次性票据（GET 方式，适合快速联调） */
    @GetMapping("/ticket/verify")
    public VerifyResult verifyTicket(
            @RequestParam @NotBlank(message = "缺少票据 ticket") String ticket,
            @RequestParam(required = false) String lang,
            @RequestHeader(name = "Accept-Language", required = false) String acceptLanguage) {
        return engine.consumeTicket(ticket)
                .localize(resolveLocale(lang, acceptLanguage), messageProvider);
    }

    /** 业务接口校验一次性票据（POST 方式，票据放请求体） */
    @PostMapping("/ticket/verify")
    public VerifyResult verifyTicket(@Valid @RequestBody TicketVerifyRequest request,
                                     @RequestHeader(name = "Accept-Language", required = false)
                                     String acceptLanguage) {
        if (request == null || request.getTicket() == null) {
            return VerifyResult.badRequest(CaptchaMessages.VERIFY_MISSING_TICKET, messageProvider)
                    .localize(resolveLocale(null, acceptLanguage), messageProvider);
        }
        return engine.consumeTicket(request.getTicket())
                .localize(resolveLocale(request.getLang(), acceptLanguage), messageProvider);
    }

    /** 查询后端支持的类型与滑块形状 */
    @GetMapping("/types")
    public Map<String, Object> types() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("types", engine.supportedTypes());
        Map<String, List<String>> shapes = new LinkedHashMap<>();
        shapes.put("slider", engine.supportedShapes());
        body.put("shapes", shapes);
        return body;
    }

    /** 解析请求语言：lang 参数优先，其次 Accept-Language，最后使用服务端默认语言 */
    private Locale resolveLocale(String lang, String acceptLanguage) {
        if (lang != null && !lang.isBlank()) {
            return Locale.forLanguageTag(lang.trim().replace('_', '-'));
        }
        if (acceptLanguage != null && !acceptLanguage.isBlank()) {
            String first = acceptLanguage.split(",")[0].trim();
            if (!first.isBlank()) {
                return Locale.forLanguageTag(first.replace('_', '-'));
            }
        }
        return messageProvider.defaultLocale();
    }
}
