package io.github.yixiaco.boot2test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spring Boot 2.7 自动配置集成测试：确认 JDK 17 + Boot 2.7 宿主引入 starter 后，
 * 无需任何额外配置即可通过 HTTP 下发验证码，并遵守 debug 开关。
 *
 * <p>这里手工启动嵌入式容器（不经过 Spring TestContext），既贴近真实运行方式，
 * 也避免为一次冒烟测试引入 Mockito 等额外测试基建。</p>
 */
class CaptchaBoot2AutoConfigurationTest {

    /** 随机端口的嵌入式容器（server.port=0） */
    private static ConfigurableApplicationContext application;

    /** 接口根地址 */
    private static String apiBase;

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private static final ObjectMapper JSON = new ObjectMapper();

    /** 启动宿主应用：只加载自动配置，不扫描 starter 自身的包 */
    @BeforeAll
    static void startApplication() {
        application = new SpringApplicationBuilder(Boot2TestApplication.class)
                .properties("server.port=0")
                .run();
        apiBase = baseUrl(application);
    }

    @AfterAll
    static void stopApplication() {
        if (application != null) {
            application.close();
        }
    }

    @Test
    void createsChallengeWithServerSelectedType() throws Exception {
        // 类型由后端决定：不带任何参数也能下发验证码
        HttpResponse<String> response = get(apiBase, "");
        assertEquals(200, response.statusCode(), response.body());

        Map<String, Object> challenge = json(response.body());
        Object type = challenge.get("type");
        assertTrue(type instanceof String && !((String) type).isEmpty(),
                "应返回后端选择的类型: " + response.body());
        assertNotNull(challenge.get("id"));
        // 部分类型（如角度验证）只下发独立小图，因此至少应有一张图片
        assertTrue(challenge.get("image1") != null || challenge.get("image2") != null,
                "应下发验证图片: " + response.body());
    }

    @Test
    void debugFlagAloneNeverExposesAnswers() throws Exception {
        // 类型池固定为滑块，确保这条断言针对确实带答案字段的类型
        try (ConfigurableApplicationContext sliderOnly = new SpringApplicationBuilder(Boot2TestApplication.class)
                .properties("server.port=0", "captcha.types=slider")
                .run()) {
            HttpResponse<String> response = get(baseUrl(sliderOnly), "?type=slider&debug=true");
            assertEquals(200, response.statusCode(), response.body());

            // 后端 debug-enabled 默认关闭：前端单独传 debug=1 不应拿到答案字段
            Map<String, Object> challenge = json(response.body());
            assertEquals("slider", challenge.get("type"), response.body());
            Object data = challenge.get("data");
            assertTrue(data instanceof Map, "data 载荷缺失: " + response.body());
            assertFalse(((Map<?, ?>) data).containsKey("debugX"),
                    "非 debug 响应不应包含答案: " + response.body());
        }
    }

    @Test
    void typeIsDecidedByServerUnlessDebugEnabled() throws Exception {
        try (ConfigurableApplicationContext debugApp = new SpringApplicationBuilder(Boot2TestApplication.class)
                .properties("server.port=0", "captcha.types=slider,click",
                        "captcha.debug-enabled=true")
                .run()) {
            String base = baseUrl(debugApp);

            // debug 模式下前端指定的类型生效（且受类型池限制）
            assertEquals("click", json(get(base, "?type=click&debug=1").body()).get("type"));

            // 未带 debug 时同一个 type 参数被忽略：多次请求应出现后端随机挑出的不同结果
            Set<Object> picked = new HashSet<>();
            for (int i = 0; i < 12; i++) {
                Object type = json(get(base, "?type=click").body()).get("type");
                assertTrue(Set.of("slider", "click").contains(type),
                        "下发类型应落在类型池内: " + type);
                picked.add(type);
            }
            assertTrue(picked.size() > 1,
                    "非 debug 下类型应由后端决定，而不是沿用客户端的 click: " + picked);

            // debug 指定类型池之外的类型：拒绝而不是降级
            Map<String, Object> rejected = json(get(base, "?type=rotate&debug=1").body());
            assertEquals(Boolean.FALSE, rejected.get("success"), rejected.toString());
            assertEquals("BAD_REQUEST", rejected.get("code"), rejected.toString());
        }
    }

    @Test
    void listsSupportedTypes() throws Exception {
        HttpResponse<String> response = get(apiBase, "/types");
        assertEquals(200, response.statusCode(), response.body());

        Object types = json(response.body()).get("types");
        assertTrue(types instanceof List, "types 载荷缺失: " + response.body());
        assertTrue(((List<?>) types).contains("slider"),
                "应包含滑块类型: " + response.body());
    }

    /** 缺少 id 与缺少票据的请求应返回 BAD_REQUEST，而不是抛出 400 异常 */
    @Test
    void missingParametersReturnBadRequestResult() throws Exception {
        Map<String, Object> missingId = json(post(apiBase, "/verify", "{\"type\":\"slider\"}").body());
        assertEquals(Boolean.FALSE, missingId.get("success"), missingId.toString());
        assertEquals("BAD_REQUEST", missingId.get("code"), missingId.toString());

        Map<String, Object> missingTicket = json(get(apiBase, "/ticket/verify").body());
        assertEquals(Boolean.FALSE, missingTicket.get("success"), missingTicket.toString());
        assertEquals("BAD_REQUEST", missingTicket.get("code"), missingTicket.toString());
    }

    /**
     * 用前端约定的 camelCase 键（{@code xNorm}）提交滑块答案。
     *
     * <p>回归点：Jackson 默认会把 {@code xNorm} 推导成 {@code xnorm}，位移字段一旦丢失，
     * 滑块 / 刮刮乐 / 滑动曲线 / 滑块摆动四类验证码会永远判定失败。</p>
     */
    @Test
    void acceptsSliderAnswerSubmittedWithCamelCaseXNorm() throws Exception {
        try (ConfigurableApplicationContext debugApp = new SpringApplicationBuilder(Boot2TestApplication.class)
                .properties("server.port=0", "captcha.debug-enabled=true",
                        "captcha.slider.min-elapsed-ms=0")
                .run()) {
            String base = baseUrl(debugApp);
            HttpResponse<String> challengeResponse = get(base, "?type=slider&debug=1");
            assertEquals(200, challengeResponse.statusCode(), challengeResponse.body());

            Map<String, Object> challenge = json(challengeResponse.body());
            String id = (String) challenge.get("id");
            double width = ((Number) challenge.get("width")).doubleValue();
            Object data = challenge.get("data");
            assertTrue(data instanceof Map, "data 载荷缺失: " + challengeResponse.body());
            double expectedX = ((Number) ((Map<?, ?>) data).get("debugX")).doubleValue() / width;

            Map<String, Object> accepted = json(post(base, "/verify",
                    verifyBody(id, expectedX)).body());
            assertEquals(Boolean.TRUE, accepted.get("success"), accepted.toString());

            // 同一次下发只允许校验一次，这里重新取一张再提交明显错误的位置
            String wrongId = (String) json(get(base, "?type=slider&debug=1").body()).get("id");
            Map<String, Object> rejected = json(post(base, "/verify",
                    verifyBody(wrongId, expectedX + 0.2)).body());
            assertFalse(Boolean.TRUE.equals(rejected.get("success")), rejected.toString());
        }
    }

    /** 构造答案请求体：位移字段与前端一致，使用 camelCase 的 {@code xNorm} */
    private static String verifyBody(String id, double xNorm) {
        return "{\"id\":\"" + id + "\",\"type\":\"slider\",\"xNorm\":" + xNorm
                + ",\"clientType\":\"web\",\"td\":\"" + behaviorTrace(xNorm) + "\"}";
    }

    /** 构造前端格式的行为轨迹：m|w|h|s|e|time,x,y,type;...，终点 x 与提交答案一致 */
    private static String behaviorTrace(double endX) {
        long start = 1_700_000_000_000L;
        long duration = 900;
        int steps = 8;
        StringBuilder points = new StringBuilder();
        for (int i = 0; i <= steps; i++) {
            if (i > 0) {
                points.append(';');
            }
            long time = duration * i / steps;
            double x = endX * i / steps;
            int type = i == 0 ? 0 : (i == steps ? 2 : 1);
            points.append(time).append(',').append(x).append(",0.5,").append(type);
        }
        return "1|340|244|" + start + "|" + (start + duration) + "|" + points;
    }

    /** 接口根地址（随机端口） */
    private static String baseUrl(ConfigurableApplicationContext context) {
        return "http://localhost:"
                + context.getEnvironment().getProperty("local.server.port")
                + "/api/captcha";
    }

    /** 发起 GET 请求（pathAndQuery 形如 {@code ?type=slider} 或 {@code /types}） */
    private static HttpResponse<String> get(String base, String pathAndQuery) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + pathAndQuery)).GET().build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /** 发起 JSON POST 请求 */
    private static HttpResponse<String> post(String base, String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /** 把响应体解析为 JSON 对象 */
    private static Map<String, Object> json(String body) throws Exception {
        return JSON.readValue(body, new TypeReference<>() {
        });
    }
}
