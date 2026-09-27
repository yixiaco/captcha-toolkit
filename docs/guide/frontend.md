# 前端接入

## 安装

```bash
npm install captcha-toolkit-vue
```

组件库把 Vue 声明为 peer dependency（构建时已 external，不打进产物），
宿主项目需要自己提供 Vue 3（`^3.5`）：

```bash
npm install vue
```

样式文件单独引入一次：`import 'captcha-toolkit-vue/style.css'`。

## 全局插件

```js
import { createApp } from 'vue'
import CaptchaToolkit from 'captcha-toolkit-vue'
import 'captcha-toolkit-vue/style.css'

createApp(App)
  .use(CaptchaToolkit, {
    baseUrl: '/api/captcha',  // 后端接口前缀
    debug: false,             // 生产环境务必关闭
  })
  .mount('#app')
```

## 组件用法

`Captcha` 通过 `display` 切换内嵌 / 弹窗两种形态：

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

<script setup>
import { Captcha, CaptchaModal } from 'captcha-toolkit-vue'

function onVerified(result) {
  console.log('验证通过，票据：', result.ticket)
}
</script>
```

默认 `mode="auto"`：组件请求时不带 `type`，由后端类型池决定，拿到响应后按其中的 `type`
自动渲染对应交互（滑块 / 文字点选 / 图形点选 / 旋转 / 角度 / 刮刮乐 / 曲线 / 滑动曲线 / 摆动图块）。

`mode="slider"`、`mode="click"` 这类具体类型只是**类型提示**，只有前端 `debug` 与后端
`debug-enabled` 同时开启时才会生效；生产环境由后端决定。无论提示是什么，组件都按后端返回的
`type` 渲染，因此不会出现“前端选滑块、后端发点选”的错配。

只想开放部分类型时，在后端配置类型池即可（不在池内的类型不会下发，debug 指定也会被拒绝）：

```yaml
captcha:
  types:
    - slider
    - click
```

也可按需使用底层组件：`SliderCaptcha` / `ClickCaptcha` / `RotateCaptcha` / `AngleCaptcha` / `ScratchCaptcha` / `CurveCaptcha` / `FloatingCaptcha`。

`Captcha` 的 `display` 支持三种展示方式：

- `inline`：内嵌到页面
- `modal`：居中弹窗
- `floating`：右下角浮动按钮，点击后在按钮位置原地展开验证面板（参考极验“浮动式”弹出样式）

## 主要 Props

| 参数 | 说明 | 默认值 |
| --- | --- | --- |
| `baseUrl` | 后端接口前缀 | `/api/captcha` |
| `api` | 自定义 API 客户端 | 自动创建 |
| `request` | 自定义请求函数 | 内置 fetch |
| `width` / `height` | 图片尺寸 | `340` / `190` |
| `mode` | 类型提示：`auto` 由后端类型池决定；具体类型（slider / click / click-shape / rotate / angle / scratch / curve / slide-curve / swing-tile）仅 debug 模式生效 | `auto` |
| `shape` | 滑块/摆动图块初始形状（仅 debug 生效，传 random 则后端随机） | `''` |
| `shapes` / `shapeLabels` | 形状选择器列表与标签（demo 由 `GET {prefix}/types?debug=1` 下发，非 debug 返回空列表） | 接口返回 |
| `debug` | 是否请求调试答案（仅联调） | `false` |
| `autoReload` | 失败后自动换一张 | `true` |
| `handleWidth` | 滑块手柄宽度 | `44` |
| `clientType` | `web` / `h5` / `mini_program`，影响后端行为校验画像 | 自动检测 |
| `promptPrefix` | 点选提示文案 | `请依次点选` |
| `curveTip` | 曲线绘制提示文案 | 中文默认 |
| `curveColor` | 用户绘制笔迹颜色 | `#3b7cff` |
| `curveWidth` | 用户绘制笔迹宽度（px） | `3` |
| `slideCurveTip` | 滑动曲线提示文案 | 中文默认 |
| `slideCurveColor` | 滑动曲线摆动曲线颜色 | `#3b7cff` |
| `swingTileTip` | 滑块摆动图块提示文案 | 中文默认 |
| `angleTip` | 角度验证提示文案 | 中文默认 |
| `scratchTip` | 刮刮乐提示文案 | 中文默认 |
| `floatingText` | 浮动按钮文案 | `安全验证` |
| `floatingPosition` | 浮动位置：bottom-right / bottom-left | `bottom-right` |
| `loadingText` / `imageAlt` | 加载提示与图片 alt | 中文默认 |
| `title` / `brandText` / `sloganText` | 弹窗标题/品牌/标语 | 品牌与标语默认隐藏 |

组件级 props 优先于插件级配置。

## 多语言与自定义提示

组件库内置简体中文（`zh-CN`，默认）与英文（`en`）两套提示文案，可通过
`locale` 切换；请求会自动携带 `Accept-Language` 请求头，与后端多语言联动
（验证失败/过期等后端消息与前端 UI 使用同一语言）。

```ts
createApp(App)
  .use(CaptchaToolkit, {
    locale: 'en', // 全局切到英文，后端消息也会返回英文
  })
```

`messages` 可按消息键覆盖任意提示，单个文案 prop 的优先级仍高于 `messages`：

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

消息键与默认文案见 `i18n.ts` 中的 `CaptchaMessages`；也可直接使用底层函数：

```ts
import { resolveCaptchaMessages, defaultMessagesFor } from 'captcha-toolkit-vue'
```

## 事件

| 事件 | 说明 |
| --- | --- |
| `success` | 验证通过，参数为 `{ ticket, ... }` |
| `fail` | 验证失败，参数为后端返回结果 |
| `error` | 请求异常 |

## 客户端类型

前端默认自动检测：

- 触摸屏且非精细指针 → `h5`
- 其余 → `web`

小程序等非 DOM 环境需要显式传入：

```vue
<Captcha client-type="mini_program" mode="click" />
```

小程序端需要使用原生组件生成同样的 `td` 报文，后端无需改动。
