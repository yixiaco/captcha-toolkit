# 后端接入

## 引入依赖

在宿主项目的 `pom.xml` 中加入 starter：

```xml
<dependency>
  <groupId>com.captcha.toolkit</groupId>
  <artifactId>captcha-spring-boot-starter</artifactId>
  <version>0.1.0</version>
</dependency>
```

也可以直接使用纯 Java 引擎 `captcha-core`，不依赖 Spring。

## HTTP 接口

接口前缀默认为 `/api/captcha`，可通过 `captcha.api-prefix` 修改。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `{prefix}?type=slider\|click\|rotate\|curve` | 下发验证码 |
| POST | `{prefix}/verify` | 校验答案 |
| GET/POST | `{prefix}/ticket/verify` | 业务接口校验一次性票据 |
| GET | `{prefix}/types` | 查询支持的类型与形状 |

### 下发验证码

```http
GET /api/captcha?type=slider&shape=classic&debug=1
```

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
    "shape": "classic",
    "pieceOffsetX": 8,
    "debugX": 168
  }
}
```

所有类型特定化属性统一放在 `data` 对象里（泛型载荷），新增验证码类型时只需定义自己的
`data` 结构，不需要给下发模型加字段：

| 类型 | `data` 字段 | 说明 |
| --- | --- | --- |
| `slider` | `shape` / `pieceOffsetX` / `debugX` | 拼图形状、拼图块留白、调试答案 x |
| `click` | `prompt` / `debugTargets` | 提示文字、调试目标坐标 |
| `rotate` | `debugAngle` | 调试答案角度（度） |
| `curve` | `debugCurve` | 调试期望曲线采样点（像素坐标） |

调试字段仅在 `debug=1` 且引擎开启 `debug-enabled` 时返回。

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

- 新增验证码类型：实现 `com.captcha.toolkit.factory.CaptchaFactory` + `AbstractCaptchaGenerator`，并新增 `CaptchaType` 枚举值
- 换背景：实现 `BackgroundProvider`（classpath / 文件 / 程序生成均可）
- 换存储：实现 `CaptchaSessionStore`（生产环境建议 Redis 等共享存储）
- 换词组来源：实现 `WordFactory`
- 自定义行为校验：继承 `AbstractBehaviorValidator` 并注册到对应生成器
