import { defineConfig } from 'vitepress'

export default defineConfig({
  // 网站基础信息
  title: 'ActX',
  description: 'ActX - Java 通用业务智能体应用，轻量级、高扩展、无侵入',

  base: '/ActX/',

  // 本地搜索
  themeConfig: {
    // 导航栏 Logo
    logo: '/assets/logo.png',

    search: {
      provider: 'local'
    },

    // 社交链接
    socialLinks: [
      { icon: 'github', link: 'https://github.com/jiang2992/ActX' }
    ],

    // 顶部导航
    nav: [
      { text: '首页', link: '/' },
      { text: '开发文档', link: '/info' },
      {
        text: 'GitHub',
        link: 'https://github.com/jiang2992/ActX'
      }
    ],

    // 中文侧边栏
    sidebar: [
      {
        text: '开发文档',
        items: [
          { text: '项目介绍', link: '/info' },
        ]
      }
    ],

    // 中英文切换入口
    languageMenuLabel: '切换语言',
    returnToTopLabel: '返回顶部'
  },

  // 中英文站点配置
  locales: {
    root: {
      label: '简体中文',
      lang: 'zh-CN'
    },
    en: {
      label: 'English',
      lang: 'en-US',
      title: 'ActX',
      description: 'ActX - Java General-Purpose Business Agent Application'
    }
  }
})
