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

## 发布到 Maven Central

`captcha-core`、`captcha-spring-boot4-starter` 与 `captcha-spring-boot3-starter` 已发布到 Maven Central，
坐标为 `io.github.yixiaco:captcha-core`、`io.github.yixiaco:captcha-spring-boot4-starter`、
`io.github.yixiaco:captcha-spring-boot3-starter`。
groupId 必须是已通过验证的命名空间（本例由 GitHub 账号自动验证），
Java 包名与命名空间保持一致，同为 `io.github.yixiaco`。

发布由父 POM 中的 `release` profile 驱动：它会生成 sources / javadoc 附件并用 GPG 签名，
再通过 Central Portal 上传并自动发布。

```powershell
cd backend
$env:JAVA_HOME='D:\jdks\openjdk-21.0.2'
# GPG 用 Git 自带的那份，需要把它的目录加进 PATH（否则 keyboxd 无法启动）
$env:PATH="C:\Program Files\Git\usr\bin;$env:PATH"
D:\software\apache-maven-3.9.11\bin\mvn.cmd -Prelease -pl captcha-core,captcha-spring-boot4-starter,captcha-spring-boot3-starter -am deploy
```

几个要点：

- 凭据来自 `settings.xml` 中 `<id>sonatype</id>` 的 server（Central Portal 用户令牌）
- 父 POM `captcha-toolkit-parent` 必须一起发布，否则使用方无法解析子 POM 的 `<parent>`
- `captcha-demo` 已标记跳过发布，只发布 core 与两个 starter
- `captcha-spring-boot3-starter` 的父 POM 是 Spring Boot 3.5.16（无法继承 `captcha-toolkit-parent`），
  它自带一份 release profile；升级版本号时要同步修改该模块的 `<version>`
- 每次发布前先递增版本号：Central 上的同一版本不可覆盖，属于不可变发布

## 发布到 npm

Vue 组件库发布为 `captcha-toolkit-vue`。`prepublishOnly` 钩子会先执行 `build:lib`，
因此不会误发过期的 `dist`。

```bash
cd frontend
npm publish
```

几个要点：

- Vue 声明为 peer dependency（构建时 `external: ['vue']`），不会打进产物，
  避免宿主项目出现两份 Vue
- 只发布 `dist`（`files` 已限定），README 与 LICENSE 由 npm 自动附带
- 账号开启 2FA 时，`_authToken` 必须是勾选了 “bypass 2FA” 的 Granular Access Token；
  临时发布也可以现场验证：`npm publish --otp=<6 位验证码>`
- 版本号在 `frontend/package.json` 中维护，已发布的版本同样不可覆盖
