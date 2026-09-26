package io.github.yixiaco.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 答案载荷的 JSON 绑定测试。
 *
 * <p>覆盖“单字母前缀 + 驼峰”字段名：Jackson 默认会把 {@code xNorm} 推导成 {@code xnorm}，
 * 若不加显式声明，前端提交的 {@code xNorm} 会被丢弃，滑块类验证码全部判定失败。</p>
 */
class CaptchaAnswerJsonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void bindsXNormFromFrontendCamelCaseKey() throws Exception {
        CaptchaAnswer answer = mapper.readValue(
                "{\"id\":\"c1\",\"type\":\"slider\",\"xNorm\":0.42}", CaptchaAnswer.class);

        assertEquals(0.42, answer.getXNorm(), 1e-9);
    }

    @Test
    void acceptsLowercaseXnormAlias() throws Exception {
        CaptchaAnswer answer = mapper.readValue(
                "{\"id\":\"c1\",\"type\":\"slider\",\"xnorm\":0.24}", CaptchaAnswer.class);

        assertEquals(0.24, answer.getXNorm(), 1e-9);
    }

    @Test
    void roundTripsThroughJsonWithCamelCaseName() throws Exception {
        String json = mapper.writeValueAsString(CaptchaAnswer.slider(0.31));
        assertTrue(json.contains("\"xNorm\":0.31"), json);

        CaptchaAnswer parsed = mapper.readValue(json, CaptchaAnswer.class);
        assertEquals(0.31, parsed.getXNorm(), 1e-9);
    }
}
