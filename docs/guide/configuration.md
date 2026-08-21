# 配置参考

所有配置位于 `captcha.*`，由 Spring Boot 自动绑定。

## 通用配置

| 配置 | 说明 | 默认值 |
| --- | --- | --- |
| `enabled` | 是否注册 HTTP 接口 | `true` |
| `api-prefix` | 接口前缀 | `/api/captcha` |
| `debug-enabled` | 是否允许 debug 返回答案 | `false` |
| `ticket-expire-seconds` | 票据有效期（秒） | `120` |
| `locale` | 默认提示语言（如 `zh_CN` / `en`） | `zh_CN` |

## 多语言提示

所有面向用户的提示（验证通过、验证码已过期、行为轨迹风险过高等）不再硬编码，
统一使用消息编码，从核心模块的 `captcha-messages*.properties` 加载，内置中文与英文：

- HTTP 请求可通过 `Accept-Language` 请求头（如 `en-US`）切换提示语言；
- 校验接口也可在答案或票据请求体中携带 `lang` 字段（如 `en`），优先级高于请求头；
- `captcha.locale` 控制服务端默认语言，编码缺失时原样返回编码；
- 业务代码可自定义 `MessageProvider` Bean，对接 Spring MessageSource 或自建字典。

完整消息编码见 `CaptchaMessages` 与 `captcha-messages*.properties`。

## 设备指纹与高频限流

前端组件默认采集设备指纹（UA、屏幕、时区、Canvas、WebGL 等），随下发与校验请求
自动携带；后端只保存 SHA-256 脱敏哈希，不落库原始指纹。

| 配置 | 说明 | 默认值 |
| --- | --- | --- |
| `rate-limit.enabled` | 是否开启同设备高频请求限流 | `false` |
| `rate-limit.max-requests` | 每个时间窗口内同一设备最大请求数 | `20` |
| `rate-limit.window-seconds` | 时间窗口长度（秒） | `60` |
| `rate-limit.fingerprint-salt` | 指纹脱敏盐 | 空 |

- 下发与校验都会按设备计数；超过上限时返回 `RATE_LIMITED` 业务码；
- 未携带指纹的请求不参与计数，便于存量前端平滑接入；
- 默认内存实现仅适合单实例，多实例请自定义 `DeviceRequestLimiter` Bean 接入 Redis 等共享存储。

## 背景图

| 配置 | 说明 | 默认值 |
| --- | --- | --- |
| `background.sources` | 背景图资源（classpath 或文件路径） | `/images/captcha/default.jpg` |
| `background.generate-fallback` | 素材缺失时程序生成风景图 | `true` |
| `click.background.sources` | 点选背景素材 | `[]` |
| `click.background.generate-fallback` | 点选背景生成兜底 | `true` |

## 行为校验

| 配置 | 说明 | 默认值 |
| --- | --- | --- |
| `behavior.enabled` | 是否开启行为校验 | `false` |
| `behavior.risk-enabled` | 是否开启第二层风险评分 | `false` |
| `behavior.risk-threshold` | 风险综合分数阈值（0~1） | `0.65` |
| `behavior.protocol` | 报文协议版本 | `1` |
| `behavior.min-points` | 轨迹点数量下限 | `3` |
| `behavior.min-duration-ms` | 行为总耗时下限 | `100` |
| `behavior.max-duration-ms` | 行为总耗时上限 | `60000` |
| `behavior.max-jump-ratio` | 相邻点最大跳跃（归一化） | `0.5` |
| `behavior.point-tolerance` | 轨迹与答案归一化容差 | `0.05` |
| `behavior.min-click-duration-ms` | 点选按下到松开最短时长 | `30` |
| `behavior.max-click-duration-ms` | 点选按下到松开最长时长 | `5000` |
| `behavior.h5.*` | H5 触摸画像（字段同上） | 触摸默认 |
| `behavior.mini-program.*` | 小程序触摸画像（字段同上） | 触摸默认 |

其中 H5 / 小程序的 `risk-threshold` 默认为 `0.8`，比 Web 宽松，以容忍触摸端的数据噪声。

## 滑块

| 配置 | 说明 | 默认值 |
| --- | --- | --- |
| `slider.width` / `height` | 图片尺寸 | `340` / `190` |
| `slider.tolerance` | 校验容差（服务端像素） | `8` |
| `slider.min-elapsed-ms` | 最短验证耗时 | `500` |
| `slider.expire-seconds` | 会话有效期 | `300` |
| `slider.default-shape` | 默认形状 | `classic` |
| `slider.enabled-shapes` | 可用形状白名单 | 9 种 |
| `slider.fake-target-count` | 假目标数量 | `0` |
| `slider.fake-target-min-gap` | 假目标最小间距 | `24` |
| `slider.fake-target-axis-threshold` | 同轴判定阈值 | `12` |

## 点选

提示词以下发单张透明背景 PNG 图片（`promptImage`）为主，避免依赖前端字体；
后端渲染顺序为：`click.fonts` 配置字体 → 内置开源字体
（`captcha-core/src/main/resources/fonts/`，ZCOOL 快乐体，OFL 协议）→
系统常见中文字体（微软雅黑/黑体/宋体/Noto Sans CJK 等）→ 逻辑字体兜底。

| 配置 | 说明 | 默认值 |
| --- | --- | --- |
| `click.width` / `height` | 图片尺寸 | `340` / `190` |
| `click.target-count` | 目标字数 | `3` |
| `click.distractor-count` | 干扰字数 | `5` |
| `click.target-text` | 目标词组候选 | `[]` |
| `click.tolerance` | 点击容差（服务端像素） | `18` |
| `click.min-elapsed-ms` | 最短验证耗时 | `800` |
| `click.font-size-min/max` | 字号范围 | `18` / `24` |
| `click.min-spacing` | 字符最小间距 | `40` |
| `click.char-pool` | 汉字字库 | CJK 常用汉字 |

## 旋转

| 配置 | 说明 | 默认值 |
| --- | --- | --- |
| `rotate.width` / `height` | 图片尺寸 | `340` / `190` |
| `rotate.tolerance` | 角度容差（度） | `3` |
| `rotate.min-angle` / `max-angle` | 错位角度范围 | `20` / `340` |
| `rotate.min-elapsed-ms` | 最短验证耗时 | `800` |
| `rotate.render-scale` | 抗锯齿超采样倍数 | `2` |

## 角度验证

角度验证只下发一张圆形图：把背景场景按随机角度旋转后裁成圆形，
场景本身具有“上下”方向，拖动滑块把圆形图转回正立方向即可通过；
不带背景框、凹口或方向箭头。

| 配置 | 说明 | 默认值 |
| --- | --- | --- |
| `angle.width` / `height` | 图片尺寸 | `340` / `190` |
| `angle.tolerance` | 角度容差（度） | `3` |
| `angle.min-angle` / `max-angle` | 箭头初始错位角度范围 | `20` / `340` |
| `angle.min-elapsed-ms` | 最短验证耗时 | `800` |
| `angle.expire-seconds` | 会话有效期 | `300` |
| `angle.disc-radius-ratio` | 圆盘半径占画布短边比例（越大圆盘越大） | `0.4` |
| `angle.render-scale` | 抗锯齿超采样倍数 | `2` |

## 刮刮乐

刮刮乐复用 `background.sources` 主背景图：图中埋入多个与背景融合的图形
（颜色从图案中心背景采样，做低明度差、小幅色相偏移与半透明叠加），
前端套上银色蒙版，用户拖动滑块从左往右横扫揭开蒙版，
提示图形全部出现后立即停止。

| 配置 | 说明 | 默认值 |
| --- | --- | --- |
| `scratch.width` / `height` | 图片尺寸 | `340` / `190` |
| `scratch.pattern-count` | 图中埋入的图案总数 | `6` |
| `scratch.target-count` | 目标图形数量上限（实际在 min~max 间随机） | `3` |
| `scratch.target-count-min` | 目标图形数量下限 | `1` |
| `scratch.pattern-size-ratio` | 图案边长占图宽比例上限（实际在 min~max 间随机） | `0.13` |
| `scratch.pattern-size-min-ratio` | 图案边长占图宽比例下限 | `0.06` |
| `scratch.pattern-min-gap` | 图案最小中心间距（像素） | `36` |
| `scratch.tolerance` | 滑块位置校验容差（归一化 0~1） | `0.03` |
| `scratch.lightness-delta-min/max` | 图案相对背景的明度差范围 | `0.04` / `0.12` |
| `scratch.hue-shift-max` | 图案相对背景的色相偏移上限（度） | `8` |
| `scratch.alpha-min/max` | 图案透明度范围（与滑块拼图凹槽一致，约 0.8） | `0.75` / `0.85` |
| `scratch.hole-white-alpha` | 图案白色透明层透明度（与滑块拼图凹槽一致） | `0.5` |
| `scratch.min-elapsed-ms` | 最短验证耗时 | `1000` |
| `scratch.expire-seconds` | 会话有效期 | `300` |
| `scratch.render-scale` | 抗锯齿超采样倍数 | `2` |

## 曲线绘制

曲线绘制复用 `background.sources` 主背景图，在其上绘制引导曲线与起终点标记。

| 配置 | 说明 | 默认值 |
| --- | --- | --- |
| `curve.width` / `height` | 图片尺寸 | `340` / `190` |
| `curve.tolerance` | 判定绘制点贴近期望曲线的容差（服务端像素） | `12` |
| `curve.min-elapsed-ms` | 最短验证耗时 | `800` |
| `curve.expire-seconds` | 会话有效期 | `300` |
| `curve.control-point-count` | 引导曲线控制点数量 | `5` |
| `curve.point-count` | 期望曲线采样点数 | `48` |
| `curve.min-coverage` | 最小覆盖率（0~1） | `0.6` |
| `curve.min-drawn-points` | 绘制答案最少点数 | `5` |

## 滑动曲线

滑动曲线复用 `background.sources` 主背景图：大图上绘制两端固定的曲线与多个凹槽，
拖动滑块改变曲线的摆动量（0~1），只有摆动到真凹槽对应的位置时曲线才与其重合；
假凹槽与真曲线共用两端固定点，但形状/振幅不同，任何摆动都无法对准。

| 配置 | 说明 | 默认值 |
| --- | --- | --- |
| `slide-curve.width` / `height` | 图片尺寸 | `340` / `190` |
| `slide-curve.tolerance` | 摆动量校验容差（归一化 0~1） | `0.035` |
| `slide-curve.min-elapsed-ms` | 最短验证耗时 | `800` |
| `slide-curve.expire-seconds` | 会话有效期 | `300` |
| `slide-curve.fake-target-count` | 假凹槽数量 | `2` |
| `slide-curve.amplitude-min/max` | 曲线振幅范围（像素，会按画布高度收敛） | `28` / `48` |
| `slide-curve.sample-count` | 摆动曲线采样点数量 | `24` |
| `slide-curve.swing-min/max` | 真凹槽摆动答案范围 | `0.1` / `0.8` |

## 滑块摆动图块

滑块摆动图块复用 `background.sources` 主背景图：小图块沿多阶贝塞尔曲线从起点
运动到目标凹槽，移动过程中方向随路径摆动，终点方向与真凹槽一致；
假凹槽数量与间距可配置。

| 配置 | 说明 | 默认值 |
| --- | --- | --- |
| `swing-tile.width` / `height` | 图片尺寸 | `340` / `190` |
| `swing-tile.tolerance` | 滑块位置校验容差（归一化 0~1） | `0.03` |
| `swing-tile.answer-min/max` | 真凹槽在路径上的位置范围 | `0.35` / `0.8` |
| `swing-tile.min-elapsed-ms` | 最短验证耗时 | `800` |
| `swing-tile.expire-seconds` | 会话有效期 | `300` |
| `swing-tile.fake-target-count` | 假凹槽数量 | `2` |
| `swing-tile.fake-target-min-gap` | 假凹槽最小中心间距 | `56` |
| `swing-tile.control-point-count` | 贝塞尔控制点数量（多阶曲线） | `2` |
| `swing-tile.piece-size-ratio` | 图块形状尺寸占图宽比例 | `0.12` |
| `swing-tile.render-scale` | 抗锯齿超采样倍数（与原滑块一致） | `2` |
| `swing-tile.rotation-swing-amplitude` | 方向摆动幅度（度） | `45` |
| `swing-tile.start-rotation-max` | 起始方向随机偏移范围（度） | `60` |
| `swing-tile.end-rotation-min/max` | 目标凹槽方向范围（度） | `-20` / `20` |

## 安全说明

- `debug-enabled` 生产环境必须关闭
- 会话一次性、带有效期，验证后立即销毁
- 默认内存存储仅适合单实例；多实例请实现 `CaptchaSessionStore` 接入 Redis 等共享存储
- CORS 配置属于宿主应用，不应放在 starter 中
