# Frontend Integration

## Installation

```bash
npm install captcha-toolkit-vue
```

Vue is declared as a peer dependency (externalized at build time, never bundled),
so the host project must provide Vue 3 (`^3.5`) itself:

```bash
npm install vue
```

Import the stylesheet once: `import 'captcha-toolkit-vue/style.css'`.

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

## Vue 2 Hosts

Vue 2.7 uses the separate package `captcha-toolkit-vue2` (same implementation source as the Vue 3
package, same props and events):

```bash
npm install captcha-toolkit-vue2
```

```js
import Vue from 'vue'
import CaptchaToolkit from 'captcha-toolkit-vue2'
import 'captcha-toolkit-vue2/style.css'

Vue.use(CaptchaToolkit, {
  baseUrl: '/api/captcha',
  debug: false,
})
```

Differences worth knowing:

- The peer dependency is `vue@^2.7` (it relies on the built-in Composition API and
  `<script setup>` support); Vue 2.6 and earlier are not supported
- Vue 2 has no `Teleport`: the modal / floating components move their node to `body` after mount,
  which behaves the same from the outside
- Vue 2 requires a single root node, so the wrapper components add a `display: contents` host node
  that does not affect layout
- TypeScript declarations (`.d.ts`) are not shipped yet: Vue 2.7's type system is incompatible with
  the Vue 3 types used by the shared sources, so they need a dedicated generation pass
- The demo page is **the same source** as the Vue 3 package (`packages/captcha-toolkit-vue/src/demo/App.vue` plus
  `demo.css`); the Vue 2 side only swaps the bootstrap to `new Vue({ render }).$mount('#app')`
- Local check: `cd packages/captcha-toolkit-vue2 && npm install && npm run build:lib && npm run smoke`

## Components

`Captcha` switches between inline and modal via `display`:

```vue
<template>
  <Captcha
    display="inline"
    :width="300"
    :height="170"
    @success="onVerified"
  />

  <CaptchaModal
    :visible="visible"
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

The default is `mode="auto"`: the component asks the server without a `type`, lets the server
pick one, and renders whichever interaction the response carries (slider / click / shape click /
rotate / angle / scratch / curve / slide curve / swing tile).

`mode="slider"`, `mode="click"` and friends are only **hints**: they apply when the frontend runs
with `debug` *and* the backend has `debug-enabled=true`. In production the server decides. The
component always renders the `type` returned by the server, so a hint can never make the UI
mismatch the issued challenge.

To expose only some types, restrict the pool on the backend (types outside it are never issued,
and debug requests for them are rejected):

```yaml
captcha:
  types:
    - slider
    - click
```

Lower-level components: `SliderCaptcha` / `ClickCaptcha` / `RotateCaptcha` / `AngleCaptcha` / `ScratchCaptcha` / `CurveCaptcha` / `FloatingCaptcha`.

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
| `mode` | Type hint: `auto` lets the server pool decide; a concrete type (slider / click / click-shape / rotate / angle / scratch / curve / slide-curve / swing-tile) only applies in debug | `auto` |
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
| `angleTip` | Angle hint | Chinese default |
| `scratchTip` | Scratch hint | Chinese default |
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
