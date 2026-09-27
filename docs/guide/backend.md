# 后端接入

## 引入依赖

按宿主的 Spring Boot 版本选择 starter（两者接口、配置项、扩展点完全一致）：

| 宿主环境 | 依赖坐标 | 编译基线 |
| --- | --- | --- |
| Spring Boot 4.x | `io.github.yixiaco:captcha-spring-boot4-starter` | Java 21 |
| Spring Boot 3.x | `io.github.yixiaco:captcha-spring-boot3-starter` | Java 17 |
| Spring Boot 2.7.x | `io.github.yixiaco:captcha-spring-boot2-starter` | Java 17 |

```xml
<dependency>
  <groupId>io.github.yixiaco</groupId>
  <artifactId>captcha-spring-boot4-starter</artifactId>
  <version>0.4.0</version>
</dependency>
```

Boot 3 / Boot 2.7 宿主把 `artifactId` 换成 `captcha-spring-boot3-starter` /
`captcha-spring-boot2-starter` 即可。三个 starter 共用同一份实现源码，
接口与配置项完全一致；只有 Boot 2.7 的 Bean Validation 仍是 javax 体系，
因此共享源码不使用 Bean Validation 注解，参数校验由控制器显式完成。
也可以直接使用纯 Java 引擎 `captcha-core`（Java 17 字节码），不依赖 Spring。

## HTTP 接口

接口前缀默认为 `/api/captcha`，可通过 `captcha.api-prefix` 修改。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `{prefix}?debug=1` | 下发验证码（类型由后端决定，仅 debug 下 `type` 生效） |
| POST | `{prefix}/verify` | 校验答案 |
| GET/POST | `{prefix}/ticket/verify` | 业务接口校验一次性票据 |
| GET | `{prefix}/types?debug=1` | 查询支持的类型与形状（debug 才返回形状列表，否则为空列表） |

### 下发验证码

```http
GET /api/captcha
```

类型由**后端决定**：引擎从 `captcha.types` 类型池（为空表示全部已注册类型）中随机挑选，
响应里的 `type` 就是实际下发的类型。非 debug 请求携带的 `type` 会被忽略；
只有前端 `debug=1` 且后端 `captcha.debug-enabled=true` 时，才允许用
`/api/captcha?type=slider&shape=classic&debug=1` 指定类型与形状，
且 `type` 必须是类型池内的类型（否则返回 `BAD_REQUEST`）。

返回示例（debug 模式会附带答案字段）：

```json
{
  "id": "7f0e...",
  "type": "slider",
  "image1": "data:image/png;base64,...",
  "image2": "data:image/png;base64,...",
  "width": 340,
  "height": 190,
  "data": {
    "pieceOffsetX": 8,
    "debugX": 168
  }
}
```

所有类型特定化属性统一放在 `data` 对象里（泛型载荷），新增验证码类型时只需定义自己的
`data` 结构，不需要给下发模型加字段：

| 类型 | `data` 字段 | 说明 |
| --- | --- | --- |
| `slider` | `pieceOffsetX` / `debugX` | 拼图块留白、调试答案 x（拼图形状仅在服务端会话保存，不下发） |
| `click` | `promptImage` / `targetCount` / `debugTargets` | 提示词整图（透明背景）、目标字数、调试目标坐标 |
| `click-shape` | `promptImage` / `targetCount` / `debugTargets` | 提示词整图（内置图形）、目标数量、调试目标坐标 |
| `rotate` | `debugAngle` | 调试答案角度（度） |
| `angle` | `discSize`，调试 `debugAngle` | 圆形图直径（像素）、调试答案角度（度，0~360） |
| `scratch` | `promptImage` / `targetCount`，调试 `debugX` / `debugTargets` / `debugPatterns` | 提示词整图（透明背景）、目标图形数、调试答案位置与图案布局 |
| `curve` | `debugCurve` | 调试期望曲线采样点（像素坐标） |
| `slide-curve` | `endpoints` / `amplitude` / `shape`，调试 `debugSwing` / `debugFakeTargets` | 前端绘制摆动曲线所需参数、调试摆动答案与假凹槽坐标 |
| `swing-tile` | `path` / `startRotation` / `endRotation` / `swingAmplitude` / `pieceSize`，调试 `debugT` / `debugFakeTargets` | 贝塞尔路径与摆动参数、调试真凹槽位置与假凹槽坐标 |

调试模式只能由后端 `debug-enabled` 开启；前端的 `debug=1` 只是透传的请求参数，
调试字段仅在两者同时满足时返回，前端无法单独通过传参拿到答案。

### 校验答案

所有坐标为归一化 0~1，不依赖前端渲染尺寸：

滑块：

```json
{
  "id": "7f0e...",
  "type": "slider",
  "xNorm": 0.52,
  "clientType": "web",
  "td": "H4sI..."
}
```

点选：

```json
{
  "id": "7f0e...",
  "type": "click",
  "points": [{"x": 0.31, "y": 0.42}],
  "clientType": "web",
  "td": "H4sI..."
}
```

旋转：

```json
{
  "id": "7f0e...",
  "type": "rotate",
  "angle": 275.3,
  "clientType": "web",
  "td": "H4sI..."
}
```

角度验证：

```json
{
  "id": "7f0e...",
  "type": "angle",
  "angle": 275.3,
  "clientType": "web",
  "td": "H4sI..."
}
```

刮刮乐：

```json
{
  "id": "7f0e...",
  "type": "scratch",
  "xNorm": 0.62,
  "clientType": "web",
  "td": "H4sI..."
}
```

`xNorm` 为滑块最终位置（归一化 0~1，横扫揭开的进度）；答案位置是全部提示图形
刚好完整出现的最小位置，停早未出全、停晚继续右移都会判定失败。

曲线：

```json
{
  "id": "7f0e...",
  "type": "curve",
  "curve": [{"x": 0.12, "y": 0.33}, {"x": 0.35, "y": 0.61}],
  "clientType": "web",
  "td": "H4sI..."
}
```

`td` 为行为轨迹报文（明文或 gzip+base64url，后端自动识别），开启行为校验后必填。

### 票据校验

```http
GET /api/captcha/ticket/verify?ticket=xxx
```

或：

```http
POST /api/captcha/ticket/verify
Content-Type: application/json

{"ticket": "xxx"}
```

## 程序化调用

不经过 HTTP，直接调用引擎：

```java
CaptchaConfig config = new CaptchaConfig();
CaptchaEngine engine = CaptchaEngine.of(
        config,
        new InMemoryCaptchaSessionStore(),
        new DataUriImageCodec(),
        List.of(),
        FallbackBackgroundProvider.of(List.of("/images/captcha/default.jpg"), true));

CaptchaChallenge challenge = engine.create(CaptchaType.SLIDER, Map.of(), false);
VerifyResult result = engine.verify(challenge.getId(),
        CaptchaAnswer.slider(100.0 / challenge.getWidth()));
```

## 自定义扩展

- 新增验证码类型：实现 `io.github.yixiaco.factory.CaptchaFactory` + `AbstractCaptchaGenerator`，并新增 `CaptchaType` 枚举值
- 换背景：实现 `BackgroundProvider`（classpath / 文件 / 程序生成均可）
- 换存储：实现 `CaptchaSessionStore`（生产环境建议 Redis 等共享存储）
- 换词组来源：实现 `WordFactory`
- 自定义行为校验：继承 `AbstractBehaviorValidator` 并注册到对应生成器
