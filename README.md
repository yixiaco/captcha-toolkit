# 极验风格验证码原型（Vue 3 + Spring Boot 4）

前后端结合的行为验证码原型，参考 `puzzle_captcha` 项目：

- 滑块拼图：后端用 Java2D 生成大图（白色半透明缺口 + 内阴影）与小图（拼图块 + 柔光投影），
  经典形状采用 puzzle_captcha 的“上凹 + 右凸 + 左凹”图形；前端只负责拖动，由后端校验偏移量。
- 文字点选：后端生成带干扰的场景图，前端按提示点击，每次点击由后端按顺序校验坐标。
- 支持 7 种滑块形状：经典 / 叶子 / 三角 / 圆形 / 菱形 / 星星 / 爱心。

## 目录结构

```text
backend/   Spring Boot 4 后端（Java 21，Maven）
frontend/  Vue 3 + Vite 前端
```

## 运行后端

```bash
cd backend
set JAVA_HOME=D:\jdks\openjdk-21.0.2
mvn spring-boot:run
```

默认监听 `http://localhost:8080`，依赖按 Maven `conf/settings.xml` 配置的
`D:\Maven\.m2\repository` 存储。

## 运行前端

```bash
cd frontend
npm install
npm run dev
```

默认监听 `http://localhost:5173`，`/api` 已代理到后端 8080。

## 接口

- `GET /api/captcha?type=slider|click&shape=classic|leaf|triangle|circle|diamond|star|heart`
  返回验证码图片与元数据（`debug=1` 时额外返回答案，仅用于调试/自动化测试）
- `POST /api/captcha/verify` 提交验证：
  - 滑块：`{ id, type: "slider", x, width }`
  - 点选：`{ id, type: "click", points: [{ x, y }, ...] }`（点完目标字后一次性提交全部点，按顺序校验）

## 安全说明

答案只保存在后端内存缓存中（默认 5 分钟过期），并带最短验证耗时与容差校验。
生产环境建议把内存缓存替换为 Redis 等分布式缓存，并接入真实风控。
