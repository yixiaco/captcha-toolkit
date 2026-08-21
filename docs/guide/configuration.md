# 配置参考

所有配置位于 `captcha.*`，由 Spring Boot 自动绑定。

## 通用配置

| 配置 | 说明 | 默认值 |
| --- | --- | --- |
| `enabled` | 是否注册 HTTP 接口 | `true` |
| `api-prefix` | 接口前缀 | `/api/captcha` |
| `debug-enabled` | 是否允许 debug 返回答案 | `false` |
| `ticket-expire-seconds` | 票据有效期（秒） | `120` |

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

## 安全说明

- `debug-enabled` 生产环境必须关闭
- 会话一次性、带有效期，验证后立即销毁
- 默认内存存储仅适合单实例；多实例请实现 `CaptchaSessionStore` 接入 Redis 等共享存储
- CORS 配置属于宿主应用，不应放在 starter 中
