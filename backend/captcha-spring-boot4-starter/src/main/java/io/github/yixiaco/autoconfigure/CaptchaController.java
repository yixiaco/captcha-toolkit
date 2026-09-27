package io.github.yixiaco.autoconfigure;

import io.github.yixiaco.CaptchaEngine;
import io.github.yixiaco.exception.RateLimitExceededException;
import io.github.yixiaco.i18n.CaptchaMessages;
import io.github.yixiaco.i18n.MessageProvider;
import io.github.yixiaco.model.CaptchaAnswer;
import io.github.yixiaco.model.TicketVerifyRequest;
import io.github.yixiaco.model.VerifyResult;
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
 *
 * <p>类型由后端决定：非 debug 请求下 {@code type} 参数会被忽略，
 * 引擎从 {@code captcha.types} 类型池中随机挑选；只有 debug 请求且
 * {@code captcha.debug-enabled=true} 时，前端才能用 {@code type} 指定类型。</p>
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

    /**
     * 下发一张验证码：类型由后端决定（仅 debug 模式下 type 参数生效），
     * shape 指定滑块形状（同样仅 debug 生效），debug 请求调试答案。
     */
    @GetMapping
    public Object create(@RequestParam(required = false) String type,
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
            return engine.createForClient(type, params, debug, deviceFingerprint);
        } catch (RateLimitExceededException e) {
            return VerifyResult.fail(CaptchaMessages.RATE_LIMIT_EXCEEDED,
                    "RATE_LIMITED", messageProvider)
                    .localize(resolveLocale(lang, acceptLanguage), messageProvider);
        } catch (IllegalArgumentException e) {
            // 客户端传了未知类型，或 debug 指定了类型池之外的类型
            return VerifyResult.badRequest(CaptchaMessages.VERIFY_UNSUPPORTED_TYPE, messageProvider)
                    .localize(resolveLocale(lang, acceptLanguage), messageProvider);
        }
    }

    /** 校验前端提交的答案 */
    @PostMapping("/verify")
    public VerifyResult verify(@RequestBody(required = false) CaptchaAnswer answer,
                               @RequestHeader(name = "Accept-Language", required = false)
                               String acceptLanguage) {
        if (answer == null || answer.getId() == null || answer.getId().isBlank()) {
            return VerifyResult.badRequest(CaptchaMessages.VERIFY_MISSING_ID, messageProvider)
                    .localize(resolveLocale(null, acceptLanguage), messageProvider);
        }
        return engine.verify(answer.getId(), answer)
                .localize(resolveLocale(answer.getLang(), acceptLanguage), messageProvider);
    }

    /** 业务接口校验一次性票据（GET 方式，适合快速联调） */
    @GetMapping("/ticket/verify")
    public VerifyResult verifyTicket(
            @RequestParam(required = false) String ticket,
            @RequestParam(required = false) String lang,
            @RequestHeader(name = "Accept-Language", required = false) String acceptLanguage) {
        if (ticket == null || ticket.isBlank()) {
            return VerifyResult.badRequest(CaptchaMessages.VERIFY_MISSING_TICKET, messageProvider)
                    .localize(resolveLocale(lang, acceptLanguage), messageProvider);
        }
        return engine.consumeTicket(ticket)
                .localize(resolveLocale(lang, acceptLanguage), messageProvider);
    }

    /** 业务接口校验一次性票据（POST 方式，票据放请求体） */
    @PostMapping("/ticket/verify")
    public VerifyResult verifyTicket(@RequestBody(required = false) TicketVerifyRequest request,
                                     @RequestHeader(name = "Accept-Language", required = false)
                                     String acceptLanguage) {
        if (request == null || request.getTicket() == null || request.getTicket().isBlank()) {
            return VerifyResult.badRequest(CaptchaMessages.VERIFY_MISSING_TICKET, messageProvider)
                    .localize(resolveLocale(null, acceptLanguage), messageProvider);
        }
        return engine.consumeTicket(request.getTicket())
                .localize(resolveLocale(request.getLang(), acceptLanguage), messageProvider);
    }

    /** 查询后端允许下发的类型与滑块形状（debug 模式才返回形状列表，否则为空列表） */
    @GetMapping("/types")
    public Map<String, Object> types(@RequestParam(defaultValue = "false") boolean debug) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("types", engine.supportedTypes());
        body.put("shapes", engine.supportedShapes(debug && properties.isDebugEnabled()));
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
