# Captcha Toolkit · 通用行为验证码组件

前后端结合的极验风格验证码工具，内置 **滑块拼图** 与 **文字点选** 两种验证方式，
设计目标是“开箱即用、可配置、可扩展”，既能作为独立 demo 运行，也能直接嵌入其他项目。

## 目录结构

```text
backend/
  captcha-core/                  纯 Java 验证码引擎（不依赖 Spring，可单独复用）
  captcha-spring-boot-starter/   Spring Boot 自动配置（一行依赖即可暴露 HTTP 接口）
  captcha-demo/                  演示应用（依赖 starter，展示接入方式）
frontend/
  src/lib/                       Vue 3 组件库（可发布 npm / 源码引入）
  src/demo/                      演示站点（组件库的用法示例）
```

## 设计模式与扩展点

- **工厂模式**：`CaptchaFactory` 接口 + `SliderCaptchaFactory` / `ClickCaptchaFactory`。
  引擎只依赖工厂接口，新增验证码类型时实现工厂并注册即可，控制器与引擎零改动。
- **模板方法**：`AbstractCaptchaGenerator` 固定“生成挑战 → 保存会话 → 类型/过期/最短耗时检查 → 校验答案”的流程，
  具体验证码只需实现自己的生成与校验逻辑。
- **策略模式**：`PuzzleShape`（拼图形状）、`BackgroundProvider`（背景图来源）、
  `CaptchaSessionStore`（会话存储）、`CaptchaImageCodec`（图片编码）都是可替换策略。
- **门面模式**：`CaptchaEngine` 屏蔽工厂、生成器、存储、编码细节，Spring 控制器和纯 Java 代码都只面向它。
- **配置对象**：核心 `CaptchaConfig` 与 Spring `CaptchaProperties` 一一对应，所有可调参数集中管理。

## 后端：嵌入其他项目

1. 先安装到本地仓库（或发布到公司私服）：

   ```bash
   cd backend
   mvn clean install
   ```

2. 在宿主项目引入 starter：

   ```xml
   <dependency>
     <groupId>com.captcha.toolkit</groupId>
     <artifactId>captcha-spring-boot-starter</artifactId>
     <version>0.1.0</version>
   </dependency>
   ```

3. 启动后自动注册接口（前缀、参数全部可配）：

   - `GET {prefix}?type=slider|click|rotate&shape=classic&debug=1` 下发验证码
   - `POST {prefix}/verify` 校验答案（滑块 `{id,type,xNorm}`；点选 `{id,type,points:[{x,y}...]}`，坐标为归一化 0~1；开启行为校验时需附带 `td` 与 `clientType`）
   - `GET/POST {prefix}/ticket/verify?ticket=...` 业务接口校验一次性票据
   - `GET {prefix}/types` 查询后端支持的类型与形状（通用前端可动态渲染）

验证通过后 `POST {prefix}/verify` 会返回一次性 `ticket`（默认 120 秒有效），
登录等业务接口拿到 `ticket` 后调用 `POST {prefix}/ticket/verify`（请求体 `{"ticket":"..."}`）校验；
校验成功即消费，同一票据不能重复使用。

滑块拼图形状默认由**后端随机决定**（从 `slider.enabled-shapes` 中选择）；
只有前后端都处于 debug 模式（前端 `debug` + 后端 `debug-enabled`）时，前端传的 `shape` 参数才会生效。

### 程序化调用（不经过 HTTP）

```java
CaptchaConfig config = new CaptchaConfig();
CaptchaEngine engine = CaptchaEngine.of(
        config,
        new InMemoryCaptchaSessionStore(),
        new DataUriImageCodec(),
        List.of(),                                   // 自定义 CaptchaFactory 列表，可空
        FallbackBackgroundProvider.of(List.of("/images/captcha/default.jpg"), true));

CaptchaChallenge challenge = engine.create(CaptchaType.SLIDER, Map.of("shape", "classic"), false);
VerifyResult result = engine.verify(challenge.getId(),
        CaptchaAnswer.slider(100.0 / challenge.getWidth()));
```

### 自定义扩展

- 新增验证码类型：实现 `CaptchaType` 枚举值 → `CaptchaFactory` → `AbstractCaptchaGenerator`，
  注册为 Spring Bean 后自动生效，未覆盖的类型仍使用内置工厂。
- 换存储：实现 `CaptchaSessionStore` 并注册 Bean（默认内存实现，生产可换成 Redis）。
- 换背景：实现 `BackgroundProvider` 并注册 Bean；默认支持 classpath/文件路径 + 程序生成兜底。
- 换形状：实现 `PuzzleShape` 并注册到 `PuzzleShapeRegistry`。
- 换词组来源：实现 `WordFactory` 并注册 Bean，点选目标词组可动态获取（数据库/远程接口等），
  默认实现从 `captcha.click.target-text` 读取静态词组：

  ```java
  @Bean
  public WordFactory dynamicWordFactory(WordService wordService) {
      return () -> wordService.randomWords(10);
  }
  ```

## 前端：嵌入其他项目

组件库支持两种用法：

### 全局插件

```js
import { createApp } from 'vue'
import CaptchaToolkit from 'captcha-toolkit-vue'
import 'captcha-toolkit-vue/style.css'

createApp(App)
  .use(CaptchaToolkit, {
    baseUrl: '/api/captcha',   // 后端接口前缀
    debug: false,              // 生产环境务必关闭
  })
  .mount('#app')
```

### 统一入口（嵌入 / 弹窗）

`Captcha` 组件通过 `display` 一个参数切换两种形态：

```vue
<template>
  <!-- 弹窗方式：visible 控制显隐 -->
  <Captcha
    display="modal"
    :visible="visible"
    mode="slider"
    @success="onVerified"
  />

  <!-- 嵌入方式：直接渲染在页面里 -->
  <Captcha
    display="inline"
    mode="click"
    :width="300"
    :height="170"
    @success="onVerified"
  />
</template>

<script setup>
import { Captcha } from 'captcha-toolkit-vue'
</script>
```

也可以按需单独使用底层组件：

- 嵌入：`SliderCaptcha` / `ClickCaptcha` / `RotateCaptcha`
- 弹窗：`CaptchaModal`

### 按需引入组件

```vue
<template>
  <CaptchaModal
    :visible="visible"
    mode="slider"
    :shape="'classic'"
    :debug="false"
    @success="onVerified"
  />
</template>

<script setup>
import { CaptchaModal } from 'captcha-toolkit-vue'
</script>
```

主要 Props（组件级 props 优先于插件级配置）：

| 参数 | 说明 | 默认值 |
| --- | --- | --- |
| `baseUrl` | 后端接口前缀 | `/api/captcha` |
| `api` | 自定义 API 客户端（`createCaptchaApi` 返回值） | 自动创建 |
| `request` | 自定义请求函数，兼容 `fetch` 签名 | 内置 fetch |
| `width` / `height` | 图片尺寸 | `340` / `190` |
| `shape` | 滑块初始形状（仅 debug 模式生效，正常模式由后端决定） | `''` |
| `shapes` | 形状选择器白名单 | 内置 9 种 |
| `shapeLabels` | 形状显示名覆盖（如 `{ classic: 'Classic' }`） | 内置中文名 |
| `showShapePicker` | 是否显示形状选择器 | `true` |
| `debug` | 是否请求调试答案（仅联调用） | `false` |
| `autoReload` | 验证失败后自动换一张 | `true` |
| `handleWidth` | 滑块手柄宽度 | `44` |
| `shapeLabel` / `randomLabel` / `sliderTip` | 滑块选择器与拖拽提示文案 | 中文默认 |
| `rotateTip` | 旋转模式拖拽提示文案 | 中文默认 |
| `promptPrefix` | 点选提示文案 | `请依次点选` |
| `loadingText` / `imageAlt` | 加载提示与图片 alt | 中文默认 |
| `title` / `brandText` / `sloganText` | 弹窗标题/品牌/标语文案 | 品牌与标语默认隐藏 |

所有界面文案都支持配置：插件级 `app.use(CaptchaToolkit, { loadingText: 'Loading...' })`
或组件级 `:loading-text="'Loading...'"`，未配置时使用内置中文默认值。

## 运行演示

后端（JDK 21 + Maven）：

```bash
cd backend/captcha-demo
set JAVA_HOME=D:\jdks\openjdk-21.0.2
mvn spring-boot:run
```

前端：

```bash
cd frontend
npm install
npm run dev
```

打开 `http://localhost:5173`，也可用 URL 直接指定验证方式：
`?captcha=slider&shape=classic`、`?captcha=click`、`?captcha=random`。

## 关键配置项（`captcha.*`）

| 配置 | 说明 | 默认 |
| --- | --- | --- |
| `enabled` | 是否注册 HTTP 接口 | `true` |
| `api-prefix` | 接口前缀 | `/api/captcha` |
| `debug-enabled` | 是否允许 `debug=1` 返回答案 | `false` |
| `ticket-expire-seconds` | 验证通过后票据有效期（秒），业务接口凭票据校验 | `120` |
| `background.sources` | 背景图资源（classpath 或文件路径） | `/images/captcha/default.jpg` |
| `background.generate-fallback` | 素材缺失时用程序生成风景图 | `true` |
| `slider.width/height` | 滑块图片尺寸 | `340/190` |
| `slider.tolerance` | 滑块校验容差（px） | `8` |
| `slider.min-elapsed-ms` | 滑块最短耗时 | `500` |
| `slider.enabled-shapes` | 允许的形状白名单 | 内置 9 种 |
| `slider.render-scale` | 抗锯齿超采样倍数 | `2` |
| `slider.fake-target-count` | 假目标（干扰凹槽）数量 | `0` |
| `slider.fake-target-min-gap` | 假目标中心最小间距（px） | `24` |
| `slider.fake-target-axis-threshold` | 判定同一 y/x 轴的像素阈值 | `12` |
| `click.target-count` | 点选目标字数 | `3` |
| `click.distractor-count` | 干扰字数 | `5` |
| `click.target-text` | 目标文字候选数组，每次随机选一个（如 `[星巴克, 麦当劳]`），留空则随机选字 | `[]` |
| `click.background.sources` | 点选背景素材（默认空 = 程序生成风景图） | `[]` |
| `click.background.generate-fallback` | 点选背景生成兜底 | `true` |
| `click.tolerance` | 点选容差（px） | `18` |
| `click.font-size-min/max` | 字号范围 | `18/24` |
| `click.char-pool` | 汉字字库（默认中文常见字符范围 U+4E00–U+9FA5） | CJK 常用汉字 |
| `click.lightness-delta-min/max` | 字形与背景明度差 | `0.12/0.18` |
| `click.hue-shift-max` | 字形色相偏移上限 | `5` |
| `click.curve-count` / `dash-count` / `dot-count` | 干扰线/噪点数量 | `24/12/160` |
| `rotate.width/height` | 旋转验证图片尺寸 | `340/190` |
| `rotate.tolerance` | 旋转角度容差（度） | `3` |
| `rotate.min-angle/max-angle` | 错位角度范围（度） | `20/340` |

滑块可配置多个假目标凹槽（`slider.fake-target-count`）。假目标允许与真目标/彼此落在
同一 y 轴，但遵循以下规则：
- 任意图形不能重叠（`slider.fake-target-min-gap` 控制中心最小间距）；
- 与真目标/彼此同 y（差值小于 `slider.fake-target-axis-threshold`）时，**大小或旋转必须不同**；
- 同 x 时 y 必须不同且不能重叠；
- 不触发上述规则时，假目标的大小和旋转与真目标**完全一致**。

## 安全说明

- 答案只保存在后端会话中，验证通过或失败后立即销毁（一次性使用）。
- 默认最短耗时与容差校验可拦截脚本秒答；`debug-enabled` 生产环境必须关闭。
- 默认会话存储为内存实现，多实例部署请实现 `CaptchaSessionStore` 接入 Redis 等共享存储。
