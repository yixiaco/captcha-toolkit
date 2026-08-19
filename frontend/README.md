# 前端组件库（Vue 3 + Vite）

`src/lib/` 是可复用的验证码组件库：滑块拼图 `SliderCaptcha`、文字点选 `ClickCaptcha`、
弹窗容器 `CaptchaModal`，以及请求适配器 `createCaptchaApi` 与形状注册表 `PUZZLE_SHAPES`。

`src/demo/` 是演示站点，展示了组件库的接入方式与 URL 调试参数：

- `?captcha=slider&shape=classic`
- `?captcha=click`
- `?captcha=random`

## 命令

```bash
npm install
npm run dev          # 开发演示站（/api 代理到 http://localhost:8080）
npm run build:lib    # 构建可发布组件库到 dist/
npm run build:demo   # 构建演示站到 dist-demo/
```

## 嵌入宿主项目

```js
import CaptchaToolkit from 'captcha-toolkit-vue'
import 'captcha-toolkit-vue/style.css'

app.use(CaptchaToolkit, { baseUrl: '/api/captcha' })
```

或按需引入：

```vue
<script setup>
import { CaptchaModal } from 'captcha-toolkit-vue'
</script>
```

所有行为参数（接口地址、尺寸、形状、文案、调试开关等）都支持
“插件级配置 → 组件 props” 两级覆盖，详见仓库根目录 README。
