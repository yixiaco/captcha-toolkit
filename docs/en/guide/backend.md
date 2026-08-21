# Backend Integration

## Add the Dependency

```xml
<dependency>
  <groupId>com.captcha.toolkit</groupId>
  <artifactId>captcha-spring-boot-starter</artifactId>
  <version>0.1.0</version>
</dependency>
```

You can also use `captcha-core` directly without Spring.

## HTTP API

The default prefix is `/api/captcha`, configurable via `captcha.api-prefix`.

| Method | Path | Description |
| --- | --- | --- |
| GET | `{prefix}?type=slider\|click\|rotate\|curve` | Create a challenge |
| POST | `{prefix}/verify` | Verify the answer |
| GET/POST | `{prefix}/ticket/verify` | Verify a one-time ticket |
| GET | `{prefix}/types` | List supported types and shapes |

### Create a Challenge

```http
GET /api/captcha?type=slider&shape=classic&debug=1
```

Example response (debug mode includes the answer):

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

All type-specific properties are grouped in a generic `data` payload. When adding a
new captcha type, define its own `data` structure instead of adding fields to the
challenge model:

| Type | `data` fields | Description |
| --- | --- | --- |
| `slider` | `shape` / `pieceOffsetX` / `debugX` | Shape, piece offset, debug answer x |
| `click` | `prompt` / `debugTargets` | Prompt text, debug target coordinates |
| `rotate` | `debugAngle` | Debug answer angle (degrees) |
| `curve` | `debugCurve` | Debug expected curve sample points (pixels) |

Debug fields are only returned when `debug=1` and `debug-enabled` is on.

### Verify Answers

All coordinates are normalized 0~1:

Slider:

```json
{
  "id": "7f0e...",
  "type": "slider",
  "xNorm": 0.52,
  "clientType": "web",
  "td": "H4sI..."
}
```

Click:

```json
{
  "id": "7f0e...",
  "type": "click",
  "points": [{"x": 0.31, "y": 0.42}],
  "clientType": "web",
  "td": "H4sI..."
}
```

Rotate:

```json
{
  "id": "7f0e...",
  "type": "rotate",
  "angle": 275.3,
  "clientType": "web",
  "td": "H4sI..."
}
```

Curve:

```json
{
  "id": "7f0e...",
  "type": "curve",
  "curve": [{"x": 0.12, "y": 0.33}, {"x": 0.35, "y": 0.61}],
  "clientType": "web",
  "td": "H4sI..."
}
```

`td` is the behavior payload (plain text or gzip + base64url; the backend auto-detects both).

Click challenges also return `prompt` / `debugTargets`, rotate returns `debugAngle`,
and curve returns `debugCurve` (expected curve sample points in pixels, debug mode only).

### Ticket Verification

```http
GET /api/captcha/ticket/verify?ticket=xxx
```

or:

```http
POST /api/captcha/ticket/verify
Content-Type: application/json

{"ticket": "xxx"}
```

## Programmatic Usage

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

## Extension Points

- New captcha type: implement `CaptchaFactory` + `AbstractCaptchaGenerator`
- Backgrounds: implement `BackgroundProvider`
- Storage: implement `CaptchaSessionStore` (use Redis for multi-instance)
- Word sources: implement `WordFactory`
- Behavior validation: extend `AbstractBehaviorValidator`
