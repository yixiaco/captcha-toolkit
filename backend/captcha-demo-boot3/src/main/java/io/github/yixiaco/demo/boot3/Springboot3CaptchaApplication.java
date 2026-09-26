package io.github.yixiaco.demo.boot3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 3 / JDK 17 演示应用：只依赖 captcha-spring-boot3-starter，
 * 自动配置会注册验证码引擎、存储与 HTTP 接口。
 */
@SpringBootApplication
public class Springboot3CaptchaApplication {

    /** 演示应用入口 */
    public static void main(String[] args) {
        SpringApplication.run(Springboot3CaptchaApplication.class, args);
    }
}
