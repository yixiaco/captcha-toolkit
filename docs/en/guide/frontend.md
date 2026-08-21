# Frontend Integration

## Global Plugin

```ts
import { createApp } from 'vue'
import CaptchaToolkit from 'captcha-toolkit-vue'
import 'captcha-toolkit-vue/style.css'

createApp(App)
  .use(CaptchaToolkit, {
    baseUrl: '/api/captcha',
    debug: false,
  })
  .mount('#app')
```

## Components

`Captcha` switches between inline and modal via `display`:

```vue
<template>
  <Captcha
    display="inline"
    mode="slider"
    :width="300"
    :height="170"
    @success="onVerified"
  />

  <CaptchaModal
    :visible="visible"
    mode="click"
    @success="onVerified"
  />
</template>

<script setup lang="ts">
import { Captcha, CaptchaModal } from 'captcha-toolkit-vue'
import type { VerifyResult } from 'captcha-toolkit-vue'

function onVerified(result: VerifyResult) {
  console.log('Verified, ticket:', result.ticket)
}
</script>
```

Lower-level components: `SliderCaptcha` / `ClickCaptcha` / `RotateCaptcha` / `CurveCaptcha` / `FloatingCaptcha`.

`Captcha` supports three display modes via `display`:

- `inline`: embed in the page
- `modal`: centered popup
- `floating`: a floating button (bottom-right) that expands the captcha panel in place at the button (GeeTest floating style)

## Main Props

| Prop | Description | Default |
| --- | --- | --- |
| `baseUrl` | Backend API prefix | `/api/captcha` |
| `api` | Custom API client | auto |
| `request` | Custom request function | fetch |
| `width` / `height` | Image size | `340` / `190` |
| `mode` | slider / click / rotate / curve / slide-curve / swing-tile | `slider` |
| `shape` | Initial slider shape (debug only) | `''` |
| `debug` | Request debug answers | `false` |
| `autoReload` | Reload after failure | `true` |
| `handleWidth` | Slider handle width | `44` |
| `clientType` | web / h5 / mini_program | auto-detected |
| `promptPrefix` | Click prompt prefix | `请依次点选` |
| `curveTip` | Curve drawing hint | Chinese default |
| `curveColor` | Stroke color | `#3b7cff` |
| `curveWidth` | Stroke width (px) | `3` |
| `slideCurveTip` | Slide curve hint | Chinese default |
| `slideCurveColor` | Swing curve color | `#3b7cff` |
| `swingTileTip` | Swing tile hint | Chinese default |
| `floatingText` | Floating button text | `安全验证` |
| `floatingPosition` | bottom-right / bottom-left | `bottom-right` |

## i18n & Custom Prompts

The library ships with Simplified Chinese (`zh-CN`, default) and English (`en`)
message dictionaries, switched via `locale`. Requests automatically carry the
`Accept-Language` header so server-side messages (verification failed, expired,
etc.) match the frontend language.

```ts
createApp(App)
  .use(CaptchaToolkit, {
    locale: 'en',
  })
```

`messages` overrides any message key; individual text props still take priority
over `messages`:

```ts
createApp(App)
  .use(CaptchaToolkit, {
    locale: 'en',
    messages: {
      sliderTip: 'Slide me to unlock',
      title: 'Verify you are human',
    },
  })
```

Message keys and defaults live in `CaptchaMessages` inside `i18n.ts`. Low-level
helpers are exported as well:

```ts
import { resolveCaptchaMessages, defaultMessagesFor } from 'captcha-toolkit-vue'
```

## Events

| Event | Description |
| --- | --- |
| `success` | Verified; payload `{ ticket, ... }` |
| `fail` | Verification failed |
| `error` | Request error |

## Client Type

The frontend auto-detects:

- Touch screen without a fine pointer → `h5`
- Otherwise → `web`

Pass it explicitly for mini programs:

```vue
<Captcha client-type="mini_program" mode="click" />
```

Mini programs need a native component producing the same `td` payload; the backend stays unchanged.
