# 前端接入

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

<script setup>
import { Captcha, CaptchaModal } from 'captcha-toolkit-vue'

function onVerified(result) {
  console.log('验证通过，票据：', result.ticket)
}
</script>
```

也可按需使用底层组件：`SliderCaptcha` / `ClickCaptcha` / `RotateCaptcha`。

## 主要 Props

| 参数 | 说明 | 默认值 |
| --- | --- | --- |
| `baseUrl` | 后端接口前缀 | `/api/captcha` |
| `api` | 自定义 API 客户端 | 自动创建 |
| `request` | 自定义请求函数 | 内置 fetch |
| `width` / `height` | 图片尺寸 | `340` / `190` |
| `mode` | 验证方式：slider / click / rotate | `slider` |
| `shape` | 滑块初始形状（仅 debug 生效） | `''` |
| `debug` | 是否请求调试答案（仅联调） | `false` |
| `autoReload` | 失败后自动换一张 | `true` |
| `handleWidth` | 滑块手柄宽度 | `44` |
| `clientType` | `web` / `h5` / `mini_program`，影响后端行为校验画像 | 自动检测 |
| `promptPrefix` | 点选提示文案 | `请依次点选` |
| `loadingText` / `imageAlt` | 加载提示与图片 alt | 中文默认 |
| `title` / `brandText` / `sloganText` | 弹窗标题/品牌/标语 | 品牌与标语默认隐藏 |

组件级 props 优先于插件级配置。

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
