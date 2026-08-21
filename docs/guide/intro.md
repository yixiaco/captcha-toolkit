# 项目简介

Captcha Toolkit 是一套可复用的**行为验证码工具集**，设计目标参考极验：

- 滑块拼图：拖动拼图块到缺口位置
- 文字点选：按提示顺序点击目标文字
- 图片旋转：拖动滑块把错位圆盘转回正确角度

所有验证码的答案只保存在服务端会话中，验证通过后发放一次性票据，失败或过期立即销毁会话。

## 技术栈

- 后端：Java 21 + Spring Boot 4 + Maven，核心引擎不依赖 Spring，可单独复用
- 前端：Vue 3 + Vite 6，组件库与演示站分离

## 模块结构

```text
backend/
  captcha-core/                  纯 Java 验证码引擎（不依赖 Spring）
  captcha-spring-boot-starter/   Spring Boot 自动配置 + HTTP 控制器
  captcha-demo/                  可运行演示应用 + 图片素材
frontend/
  src/lib/                       Vue 3 组件库
  src/demo/                      演示站点
docs/                            VitePress 文档站（本页面）
```

## 核心特性

- **统一模板 + 分类型实现**：行为校验由 `AbstractBehaviorValidator` 定义通用流程，滑块/点选/旋转各自实现事件序列与答案关联校验
- **轨迹采集与压缩**：前端采集 `td` 轨迹（按下/移动/松开/点击），gzip + base64url 压缩后提交
- **分端画像**：Web（鼠标）、H5（触摸）、小程序使用不同的阈值，避免误杀
- **归一化坐标**：答案与轨迹统一使用 0~1 归一化坐标，与渲染尺寸、设备分辨率无关
- **一次性票据**：验证通过后发放 `ticket`，业务接口凭票据二次校验，票据一次性且带有效期
