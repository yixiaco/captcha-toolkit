package io.github.yixiaco.boot4test;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spring Boot 4 自动配置集成测试：确认 Boot 4（Jackson 3）宿主引入 starter 后
 * 能正常下发验证码并接收位移答案。
 *
 * <p>这里手工启动嵌入式容器（不经过 Spring TestContext），避免为一次冒烟测试
 * 引入 Mockito 等额外测试基建。</p>
 */
class CaptchaBoot4AutoConfigurationTest {

    /** 随机端口的嵌入式容器（server.port=0） */
    private static ConfigurableApplicationContext application;

    /** 接口根地址 */
    private static String apiBase;

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private static final ObjectMapper JSON = new ObjectMapper();

    @BeforeAll
    static void startApplication() {
        application = new SpringApplicationBuilder(Boot4TestApplication.class)
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
    void createsSliderChallengeWithoutExtraConfiguration() throws Exception {
        HttpResponse<String> response = get(apiBase, "?type=slider");
        assertEquals(200, response.statusCode(), response.body());

        JsonNode challenge = JSON.readTree(response.body());
        assertEquals("slider", challenge.get("type").asString());
        assertFalse(challenge.get("id").asString().isEmpty());
        assertFalse(challenge.get("image1").asString().isEmpty());
    }

    @Test
    void listsSupportedTypes() throws Exception {
        HttpResponse<String> response = get(apiBase, "/types");
        assertEquals(200, response.statusCode(), response.body());

        JsonNode types = JSON.readTree(response.body()).get("types");
        assertTrue(types.size() > 0, "应返回支持的验证码类型: " + response.body());
        assertTrue(response.body().contains("slider"), response.body());
    }

    /**
     * 用前端约定的 camelCase 键（{@code xNorm}）提交滑块答案。
     *
     * <p>回归点：Jackson（2 与 3 相同）默认会把 {@code xNorm} 推导成 {@code xnorm}，
     * 位移字段一旦丢失，滑块 / 刮刮乐 / 滑动曲线 / 滑块摆动四类验证码会永远判定失败。</p>
     */
    @Test
    void acceptsSliderAnswerSubmittedWithCamelCaseXNorm() throws Exception {
        try (ConfigurableApplicationContext debugApp = new SpringApplicationBuilder(Boot4TestApplication.class)
                .properties("server.port=0", "captcha.debug-enabled=true",
                        "captcha.slider.min-elapsed-ms=0")
                .run()) {
            String base = baseUrl(debugApp);
            HttpResponse<String> challengeResponse = get(base, "?type=slider&debug=1");
            assertEquals(200, challengeResponse.statusCode(), challengeResponse.body());

            JsonNode challenge = JSON.readTree(challengeResponse.body());
            String id = challenge.get("id").asString();
            double width = challenge.get("width").asDouble();
            double expectedX = challenge.get("data").get("debugX").asDouble() / width;

            JsonNode accepted = JSON.readTree(post(base, "/verify", verifyBody(id, expectedX)).body());
            assertTrue(accepted.get("success").asBoolean(), accepted.toString());

            // 同一次下发只允许校验一次，这里重新取一张再提交明显错误的位置
            String wrongId = JSON.readTree(get(base, "?type=slider&debug=1").body()).get("id").asString();
            JsonNode rejected = JSON.readTree(post(base, "/verify", verifyBody(wrongId, expectedX + 0.2)).body());
            assertFalse(rejected.get("success").asBoolean(), rejected.toString());
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

    /** 发起 GET 请求 */
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
}
