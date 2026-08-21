# 快速开始

## 环境要求

- JDK 21
- Maven 3.9+（本仓库使用 `D:\software\apache-maven-3.9.11`，本地仓库为 `D:\Maven\.m2`）
- Node.js 18+（Vite 6 要求；本机默认 Node 16 会报 `crypto$2.getRandomValues is not a function`，请使用 Node 18+ 或 Codex 捆绑的 Node 24）

## 启动后端

```powershell
cd backend
$env:JAVA_HOME='D:\jdks\openjdk-21.0.2'
D:\software\apache-maven-3.9.11\bin\mvn.cmd clean install
D:\software\apache-maven-3.9.11\bin\mvn.cmd -pl captcha-demo spring-boot:run
```

后端默认监听 `http://localhost:18080`。

::: tip 修改了 captcha-core 或 starter 源码后
先重新执行 `mvn.cmd -pl captcha-core,captcha-spring-boot-starter install -DskipTests`，
否则 demo 会使用本地仓库中的旧版本。
:::

## 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端开发服务器默认监听 `http://localhost:5173`，并把 `/api` 代理到 `:18080`。

## 打开演示

浏览器访问：

- `http://localhost:5173/?captcha=slider` 滑块拼图
- `http://localhost:5173/?captcha=click` 文字点选
- `http://localhost:5173/?captcha=rotate` 图片旋转
- `http://localhost:5173/?captcha=random` 随机模式

滑块调试时可追加形状参数，例如 `?captcha=slider&shape=classic`。

## 接口自检

```bash
curl http://localhost:18080/api/captcha/types
```

返回后端支持的类型与滑块形状：

```json
{"types":["click","rotate","slider"],"shapes":{"slider":["classic","leaf","triangle","circle","diamond","star","heart","moon","hexagon"]}}
```
