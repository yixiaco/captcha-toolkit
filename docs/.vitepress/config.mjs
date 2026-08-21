import { defineConfig } from 'vitepress'

// https://vitepress.dev/reference/site-config
export default defineConfig({
  lang: 'zh-CN',
  title: 'Captcha Toolkit',
  description: '通用行为验证码组件库文档：滑块拼图 / 文字点选 / 图片旋转',
  // GitHub Pages 项目站点按仓库名部署；部署到自定义域名时改成 /
  base: '/captcha_codex/',
  ignoreDeadLinks: true,
  lastUpdated: true,
  markdown: {
    lineNumbers: true,
  },
  themeConfig: {
    nav: [
      { text: '首页', link: '/' },
      { text: '快速开始', link: '/guide/quickstart' },
      { text: '后端接入', link: '/guide/backend' },
      { text: '前端接入', link: '/guide/frontend' },
      { text: '行为校验', link: '/guide/behavior' },
    ],
    sidebar: [
      {
        text: '指南',
        items: [
          { text: '项目简介', link: '/guide/intro' },
          { text: '快速开始', link: '/guide/quickstart' },
          { text: '后端接入', link: '/guide/backend' },
          { text: '前端接入', link: '/guide/frontend' },
          { text: '行为轨迹校验', link: '/guide/behavior' },
          { text: '配置参考', link: '/guide/configuration' },
          { text: '部署与文档站', link: '/guide/deploy' },
        ],
      },
    ],
    outline: { label: '本页导航' },
    docFooter: { prev: '上一页', next: '下一页' },
    lastUpdated: { text: '最后更新于' },
    search: { provider: 'local' },
  },
})
