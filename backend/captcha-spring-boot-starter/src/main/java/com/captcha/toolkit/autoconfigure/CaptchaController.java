package com.captcha.toolkit.autoconfigure;

import com.captcha.toolkit.CaptchaEngine;
import com.captcha.toolkit.CaptchaType;
import com.captcha.toolkit.model.CaptchaAnswer;
import com.captcha.toolkit.model.CaptchaChallenge;
import com.captcha.toolkit.model.VerifyResult;
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
 * <br>GET  {prefix}/types
 */
@RestController
@RequestMapping("${captcha.api-prefix:/api/captcha}")
public class CaptchaController {

    private final CaptchaEngine engine;
    private final CaptchaProperties properties;

    public CaptchaController(CaptchaEngine engine, CaptchaProperties properties) {
        this.engine = engine;
        this.properties = properties;
    }

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

    @PostMapping("/verify")
    public VerifyResult verify(@RequestBody CaptchaAnswer answer) {
        if (answer == null || answer.getId() == null) {
            return VerifyResult.badRequest("缺少验证码 id");
        }
        return engine.verify(answer.getId(), answer);
    }

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
