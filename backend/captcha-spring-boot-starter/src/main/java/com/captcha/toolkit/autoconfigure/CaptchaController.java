package com.captcha.toolkit.autoconfigure;

import com.captcha.toolkit.CaptchaEngine;
import com.captcha.toolkit.CaptchaType;
import com.captcha.toolkit.model.CaptchaAnswer;
import com.captcha.toolkit.model.CaptchaChallenge;
import com.captcha.toolkit.model.TicketVerifyRequest;
import com.captcha.toolkit.model.VerifyResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
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

    /**
     * @param engine     验证码引擎
     * @param properties 验证码配置
     */
    public CaptchaController(CaptchaEngine engine, CaptchaProperties properties) {
        this.engine = engine;
        this.properties = properties;
    }

    /** 下发一张验证码：type 指定类型，shape 指定滑块形状，debug 请求调试答案 */
    @GetMapping
    public CaptchaChallenge create(@RequestParam(defaultValue = "slider") String type,
                                   @RequestParam(required = false) String shape,
                                   @RequestParam(defaultValue = "false") boolean debug) {
        Map<String, String> params = new LinkedHashMap<>();
        if (shape != null && !shape.isBlank()) {
            params.put("shape", shape);
        }
        return engine.create(CaptchaType.fromCode(type), params,
                debug && properties.isDebugEnabled());
    }

    /** 校验前端提交的答案 */
    @PostMapping("/verify")
    public VerifyResult verify(@Valid @RequestBody CaptchaAnswer answer) {
        if (answer == null || answer.getId() == null) {
            return VerifyResult.badRequest("缺少验证码 id");
        }
        return engine.verify(answer.getId(), answer);
    }

    /** 业务接口校验一次性票据（GET 方式，适合快速联调） */
    @GetMapping("/ticket/verify")
    public VerifyResult verifyTicket(
            @RequestParam @NotBlank(message = "缺少票据 ticket") String ticket) {
        return engine.consumeTicket(ticket);
    }

    /** 业务接口校验一次性票据（POST 方式，票据放请求体） */
    @PostMapping("/ticket/verify")
    public VerifyResult verifyTicket(@Valid @RequestBody TicketVerifyRequest request) {
        if (request == null || request.getTicket() == null) {
            return VerifyResult.badRequest("缺少票据 ticket");
        }
        return engine.consumeTicket(request.getTicket());
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
}
