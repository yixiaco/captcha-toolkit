# Configuration Reference

All settings live under `captcha.*` and are bound by Spring Boot.

## General

| Setting | Description | Default |
| --- | --- | --- |
| `enabled` | Register HTTP endpoints | `true` |
| `api-prefix` | API prefix | `/api/captcha` |
| `debug-enabled` | Allow debug answers | `false` |
| `ticket-expire-seconds` | Ticket TTL (s) | `120` |
| `locale` | Default message language (e.g. `zh_CN` / `en`) | `zh_CN` |

## Localized Messages

All user-facing messages (verified, captcha expired, behavior risk too high,
etc.) are no longer hardcoded. They use message codes loaded from
`captcha-messages*.properties` in the core module (Chinese and English included):

- HTTP requests can switch the language with the `Accept-Language` header (e.g. `en-US`);
- verify endpoints can also carry a `lang` field (e.g. `en`) in the answer or ticket body, which takes priority;
- `captcha.locale` controls the server default language; missing codes fall back to the code itself;
- hosts can provide a custom `MessageProvider` bean backed by Spring MessageSource or their own dictionary.

See `CaptchaMessages` and `captcha-messages*.properties` for the full list of codes.

## Backgrounds

| Setting | Description | Default |
| --- | --- | --- |
| `background.sources` | Background sources (classpath or file) | `/images/captcha/default.jpg` |
| `background.generate-fallback` | Generate scenes when missing | `true` |
| `click.background.sources` | Click background sources | `[]` |
| `click.background.generate-fallback` | Click fallback | `true` |

## Behavior

| Setting | Description | Default |
| --- | --- | --- |
| `behavior.enabled` | Enable behavior validation | `false` |
| `behavior.risk-enabled` | Enable second-layer risk scoring | `false` |
| `behavior.risk-threshold` | Risk score threshold (0~1) | `0.65` |
| `behavior.protocol` | Payload protocol version | `1` |
| `behavior.min-points` | Min trajectory points | `3` |
| `behavior.min-duration-ms` | Min total duration | `100` |
| `behavior.max-duration-ms` | Max total duration | `60000` |
| `behavior.max-jump-ratio` | Max jump between points | `0.5` |
| `behavior.point-tolerance` | Normalized answer tolerance | `0.05` |
| `behavior.min-click-duration-ms` | Min click duration | `30` |
| `behavior.max-click-duration-ms` | Max click duration | `5000` |
| `behavior.h5.*` | H5 touch profile | touch defaults |
| `behavior.mini-program.*` | Mini program profile | touch defaults |

The `risk-threshold` inside the H5 / mini-program profiles defaults to `0.8`,
looser than web to tolerate touch noise.

## Slider

| Setting | Description | Default |
| --- | --- | --- |
| `slider.width` / `height` | Image size | `340` / `190` |
| `slider.tolerance` | Pixel tolerance | `8` |
| `slider.min-elapsed-ms` | Min elapsed time | `500` |
| `slider.expire-seconds` | Session TTL | `300` |
| `slider.default-shape` | Default shape | `classic` |
| `slider.enabled-shapes` | Shape whitelist | 9 shapes |
| `slider.fake-target-count` | Fake targets | `0` |
| `slider.fake-target-min-gap` | Min gap between targets | `24` |
| `slider.fake-target-axis-threshold` | Axis threshold | `12` |

## Click

| Setting | Description | Default |
| --- | --- | --- |
| `click.width` / `height` | Image size | `340` / `190` |
| `click.target-count` | Target characters | `3` |
| `click.distractor-count` | Distractors | `5` |
| `click.target-text` | Target text candidates | `[]` |
| `click.tolerance` | Click tolerance | `18` |
| `click.min-elapsed-ms` | Min elapsed time | `800` |
| `click.font-size-min/max` | Font size range | `18` / `24` |
| `click.min-spacing` | Min character spacing | `40` |
| `click.char-pool` | Character pool | CJK common |

## Rotate

| Setting | Description | Default |
| --- | --- | --- |
| `rotate.width` / `height` | Image size | `340` / `190` |
| `rotate.tolerance` | Angle tolerance | `3` |
| `rotate.min-angle` / `max-angle` | Misalignment range | `20` / `340` |
| `rotate.min-elapsed-ms` | Min elapsed time | `800` |
| `rotate.render-scale` | Supersampling | `2` |

## Security

- Keep `debug-enabled` off in production
- Sessions are one-time and expire server-side
- Replace the in-memory store with Redis for multi-instance deployments
- CORS belongs to the host application
