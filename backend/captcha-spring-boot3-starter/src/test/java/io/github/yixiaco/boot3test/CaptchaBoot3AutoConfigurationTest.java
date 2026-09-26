package io.github.yixiaco.boot3test;

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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spring Boot 3 自动配置集成测试：确认 JDK 17 + Boot 3 宿主引入 starter 后，
 * 无需任何额外配置即可通过 HTTP 下发验证码，并遵守 debug 开关。
 *
 * <p>这里手工启动嵌入式容器（不经过 Spring TestContext），既贴近真实运行方式，
 * 也避免为一次冒烟测试引入 Mockito 等额外测试基建。</p>
 */
class CaptchaBoot3AutoConfigurationTest {

    /** 随机端口的嵌入式容器（server.port=0） */
    private static ConfigurableApplicationContext application;

    /** 接口根地址 */
    private static String apiBase;

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private static final ObjectMapper JSON = new ObjectMapper();

    /** 启动宿主应用：只加载自动配置，不扫描 starter 自身的包 */
    @BeforeAll
    static void startApplication() {
        application = new SpringApplicationBuilder(Boot3TestApplication.class)
                .properties("server.port=0")
                .run();
        apiBase = "http://localhost:"
                + application.getEnvironment().getProperty("local.server.port")
                + "/api/captcha";
    }

    @AfterAll
    static void stopApplication() {
        if (application != null) {
            application.close();
        }
    }

    @Test
    void createsSliderChallengeWithoutExtraConfiguration() throws Exception {
        HttpResponse<String> response = get("?type=slider");
        assertEquals(200, response.statusCode(), response.body());

        Map<String, Object> challenge = json(response.body());
        assertEquals("slider", challenge.get("type"));
        assertNotNull(challenge.get("id"));
        assertNotNull(challenge.get("image1"));
        assertNotNull(challenge.get("image2"));
    }

    @Test
    void debugFlagAloneNeverExposesAnswers() throws Exception {
        HttpResponse<String> response = get("?type=slider&debug=true");
        assertEquals(200, response.statusCode(), response.body());

        // 后端 debug-enabled 默认关闭：前端单独传 debug=1 不应拿到答案字段
        Object data = json(response.body()).get("data");
        assertTrue(data instanceof Map, "data 载荷缺失: " + response.body());
        assertFalse(((Map<?, ?>) data).containsKey("debugX"),
                "非 debug 响应不应包含答案: " + response.body());
    }

    @Test
    void listsSupportedTypes() throws Exception {
        HttpResponse<String> response = get("/types");
        assertEquals(200, response.statusCode(), response.body());

        Object types = json(response.body()).get("types");
        assertTrue(types instanceof List, "types 载荷缺失: " + response.body());
        assertTrue(((List<?>) types).contains("slider"),
                "应包含滑块类型: " + response.body());
    }

    /** 发起 GET 请求（pathAndQuery 形如 {@code ?type=slider} 或 {@code /types}） */
    private static HttpResponse<String> get(String pathAndQuery) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(apiBase + pathAndQuery)).GET().build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /** 把响应体解析为 JSON 对象 */
    private static Map<String, Object> json(String body) throws Exception {
        return JSON.readValue(body, new TypeReference<>() {
        });
    }
}
