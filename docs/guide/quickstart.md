# 快速开始

## 环境要求

- JDK 21（构建全部模块 / 运行 Boot 4 演示需要；只构建 `captcha-core` 与 Boot 3 模块时 JDK 17 即可）
- Maven 3.9+（本仓库使用 `D:\software\apache-maven-3.9.11`，本地仓库为 `D:\Maven\.m2`）
- Node.js 18+（Vite 6 要求；本机默认 Node 16 会报 `crypto$2.getRandomValues is not a function`，请使用 Node 18+ 或 Codex 捆绑的 Node 24）

## 启动后端

```powershell
cd backend
$env:JAVA_HOME='D:\jdks\openjdk-21.0.2'
D:\software\apache-maven-3.9.11\bin\mvn.cmd clean install
# Boot 4 演示：http://localhost:18080（前端 dev server 默认代理到它）
D:\software\apache-maven-3.9.11\bin\mvn.cmd -pl captcha-demo-boot4 -am spring-boot:run
```

Boot 3 / JDK 17 演示：同一套配置与接口，端口 18081（与 Boot 4 演示可同时运行）：

```powershell
cd backend
$env:JAVA_HOME='D:\jdks\graalvm-jdk-17.0.12'
D:\software\apache-maven-3.9.11\bin\mvn.cmd -pl captcha-demo-boot3 -am spring-boot:run
```

两个演示都在 `captcha.debug-enabled: true` 下运行，便于自检；生产环境务必关闭。

::: tip 修改了 captcha-core 或 starter 源码后
先重新执行 `mvn.cmd -pl captcha-core,captcha-spring-boot4-starter,captcha-spring-boot3-starter install -DskipTests`，
否则 demo 会使用本地仓库中的旧版本。
:::

## 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端开发服务器默认监听 `http://localhost:5173`，并把 `/api` 代理到 `:18080`。

前端为 TypeScript 工程，常用校验命令：

```bash
npm run type-check   # vue-tsc 类型检查
npm run lint         # ESLint 校验
npm run build:lib    # 构建组件库（含 .d.ts 类型声明）
```

## 打开演示

浏览器访问：

- `http://localhost:5173/?captcha=slider` 滑块拼图
- `http://localhost:5173/?captcha=click` 文字点选
- `http://localhost:5173/?captcha=click-shape` 图形点选
- `http://localhost:5173/?captcha=rotate` 图片旋转
- `http://localhost:5173/?captcha=angle` 角度验证
- `http://localhost:5173/?captcha=scratch` 刮刮乐
- `http://localhost:5173/?captcha=curve` 曲线绘制
- `http://localhost:5173/?captcha=slide-curve` 滑动曲线
- `http://localhost:5173/?captcha=swing-tile` 滑块摆动图块
- `http://localhost:5173/?captcha=auto`（同 `random`）后端决定类型

具体类型属于调试用法：只有演示（`debug-enabled: true`）下才会按 `?captcha=` 指定的类型下发，
生产环境由后端 `captcha.types` 类型池决定。滑块调试时可追加形状参数，例如 `?captcha=slider&shape=classic`。

## 接口自检

```bash
curl "http://localhost:18080/api/captcha/types?debug=1"
```

（Boot 3 演示换成 `http://localhost:18081/api/captcha/types?debug=1`。）

返回后端支持的类型与滑块形状：

```json
{"types":["angle","click","click-shape","curve","rotate","scratch","slide-curve","slider","swing-tile"],"shapes":{"slider":[{"name":"classic","label":"经典"},{"name":"leaf","label":"叶子"},...],"swing-tile":[{"name":"classic","label":"经典"},...]}}

不带 `debug=1` 时 `shapes` 各类型均为空列表，避免把可用图形白名单暴露给前端。
```
