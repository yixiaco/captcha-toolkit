# 部署与文档站

## 本地预览文档

```bash
cd docs
npm install
npm run dev
```

文档站默认运行在 `http://localhost:5174`，与前端演示站（`5173`）端口区分开。

文档站支持：

- 图片点击放大（medium-zoom）
- 简体中文 / English 多语言切换（右上角语言菜单）

构建静态站点：

```bash
npm run build
```

产物输出到 `docs/.vitepress/dist`。

## GitHub Pages 自动部署

仓库包含 `.github/workflows/deploy-docs.yml`，推送 `master` 分支时自动：

1. 安装文档依赖（npm）
2. 执行 `vitepress build`
3. 上传 `docs/.vitepress/dist` 到 GitHub Pages
4. 发布

使用前需在仓库 Settings → Pages 中把 Source 设为 **GitHub Actions**。

::: warning 站点路径
`docs/.vitepress/config.mjs` 中的 `base` 为 `/captcha-toolkit/`，
需与 GitHub 仓库名保持一致；部署到自定义域名时改为 `/`。
:::

## 项目部署建议

后端打包：

```powershell
cd backend
$env:JAVA_HOME='D:\jdks\openjdk-21.0.2'
D:\software\apache-maven-3.9.11\bin\mvn.cmd clean install
```

前端构建：

```bash
cd frontend
npm install
npm run build:demo
```

演示站产物在 `frontend/dist`；生产环境请将 `captcha.debug-enabled` 设为 `false`，
并替换默认内存会话存储。
