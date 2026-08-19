# 前端（Vue 3 + Vite）

极验风格验证码原型前端，需要配合 `backend/` 一起运行。

```bash
npm install
npm run dev
```

开发服务器默认 `http://localhost:5173`，`/api` 请求会代理到 `http://localhost:8080`。

滑块拼图与文字点选均由后端生成图片并完成校验，前端只负责交互。
