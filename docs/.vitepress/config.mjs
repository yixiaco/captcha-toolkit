import { defineConfig } from 'vitepress'

// 中文搜索文案
const zhSearch = {
  placeholder: '搜索文档',
  translations: {
    button: { buttonText: '搜索文档', buttonAriaLabel: '搜索文档' },
    modal: {
      searchBox: {
        resetButtonTitle: '清除查询条件',
        resetButtonAriaLabel: '清除查询条件',
        cancelButtonText: '取消',
        cancelButtonAriaLabel: '取消',
      },
      startScreen: {
        recentSearchesTitle: '搜索历史',
        noRecentSearchesText: '没有搜索历史',
        saveRecentSearchButtonTitle: '保存至搜索历史',
        removeRecentSearchButtonTitle: '从搜索历史中移除',
        favoriteSearchesTitle: '收藏',
        removeFavoriteSearchButtonTitle: '从收藏中移除',
      },
      errorScreen: {
        titleText: '无法获取结果',
        helpText: '你可能需要检查你的网络连接',
      },
      footer: {
        selectText: '选择',
        navigateText: '切换',
        closeText: '关闭',
        searchByText: '搜索提供者',
      },
      noResultsScreen: {
        noResultsText: '无法找到相关结果',
        suggestedQueryText: '你可以尝试查询',
        reportMissingResultsText: '你认为该查询应该有结果？',
        reportMissingResultsLinkText: '点击反馈',
      },
    },
  },
}

// 中文导航与侧边栏
const zhNav = [
  { text: '首页', link: '/' },
  { text: '快速开始', link: '/guide/quickstart' },
  { text: '后端接入', link: '/guide/backend' },
  { text: '前端接入', link: '/guide/frontend' },
  { text: '行为校验', link: '/guide/behavior' },
]

const zhSidebar = [
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
]

// 英文导航与侧边栏
const enNav = [
  { text: 'Home', link: '/en/' },
  { text: 'Quick Start', link: '/en/guide/quickstart' },
  { text: 'Backend', link: '/en/guide/backend' },
  { text: 'Frontend', link: '/en/guide/frontend' },
  { text: 'Behavior', link: '/en/guide/behavior' },
]

const enSidebar = [
  {
    text: 'Guide',
    items: [
      { text: 'Introduction', link: '/en/guide/intro' },
      { text: 'Quick Start', link: '/en/guide/quickstart' },
      { text: 'Backend Integration', link: '/en/guide/backend' },
      { text: 'Frontend Integration', link: '/en/guide/frontend' },
      { text: 'Behavior Validation', link: '/en/guide/behavior' },
      { text: 'Configuration', link: '/en/guide/configuration' },
      { text: 'Deployment & Docs', link: '/en/guide/deploy' },
    ],
  },
]

// https://vitepress.dev/reference/site-config
export default defineConfig({
  // GitHub Pages 项目站点按仓库名部署；当前仓库为 captcha-toolkit，部署到自定义域名时改成 /
  base: '/captcha-toolkit/',
  ignoreDeadLinks: true,
  lastUpdated: true,
  markdown: {
    lineNumbers: true,
    image: {
      lazyLoading: true,
    },
  },
  themeConfig: {
    langMenuLabel: '语言 / Language',
  },
  locales: {
    root: {
      label: '简体中文',
      lang: 'zh-CN',
      title: 'Captcha Toolkit',
      description: '通用行为验证码组件库文档：滑块拼图 / 文字点选 / 图片旋转',
      themeConfig: {
        nav: zhNav,
        sidebar: zhSidebar,
        outline: { label: '本页导航' },
        docFooter: { prev: '上一页', next: '下一页' },
        lastUpdated: { text: '最后更新于' },
        search: {
          provider: 'local',
          options: {
            locales: {
              root: zhSearch,
            },
          },
        },
      },
    },
    en: {
      label: 'English',
      lang: 'en',
      title: 'Captcha Toolkit',
      description: 'Behavior captcha toolkit docs: slider / click / rotate',
      themeConfig: {
        nav: enNav,
        sidebar: enSidebar,
        outline: { label: 'On this page' },
        docFooter: { prev: 'Previous', next: 'Next' },
        lastUpdated: { text: 'Last updated' },
        search: {
          provider: 'local',
          options: {
            locales: {
              en: {
                placeholder: 'Search docs',
              },
            },
          },
        },
      },
    },
  },
})
