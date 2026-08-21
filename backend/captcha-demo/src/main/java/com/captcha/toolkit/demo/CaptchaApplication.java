package com.captcha.toolkit.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 演示应用：只依赖 captcha-spring-boot-starter，
 * 自动配置会注册验证码引擎、存储与 HTTP 接口。
 */
@SpringBootApplication
public class CaptchaApplication {

    /** 演示应用入口 */
    public static void main(String[] args) {
        SpringApplication.run(CaptchaApplication.class, args);
    }
}
