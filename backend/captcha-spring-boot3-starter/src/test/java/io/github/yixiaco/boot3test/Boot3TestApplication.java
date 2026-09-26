package io.github.yixiaco.boot3test;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 集成测试宿主应用：只依赖自动配置，不扫描 starter 自身的包，
 * 以此验证验证码接口确实由 {@code AutoConfiguration.imports} 自动配置注册。
 */
@SpringBootApplication
public class Boot3TestApplication {
}
