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

Lower-level components: `SliderCaptcha` / `ClickCaptcha` / `RotateCaptcha` / `CurveCaptcha`.

## Main Props

| Prop | Description | Default |
| --- | --- | --- |
| `baseUrl` | Backend API prefix | `/api/captcha` |
| `api` | Custom API client | auto |
| `request` | Custom request function | fetch |
| `width` / `height` | Image size | `340` / `190` |
| `mode` | slider / click / rotate / curve | `slider` |
| `shape` | Initial slider shape (debug only) | `''` |
| `debug` | Request debug answers | `false` |
| `autoReload` | Reload after failure | `true` |
| `handleWidth` | Slider handle width | `44` |
| `clientType` | web / h5 / mini_program | auto-detected |
| `promptPrefix` | Click prompt prefix | `请依次点选` |
| `curveTip` | Curve drawing hint | Chinese default |
| `curveColor` | Stroke color | `#3b7cff` |
| `curveWidth` | Stroke width (px) | `3` |

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
