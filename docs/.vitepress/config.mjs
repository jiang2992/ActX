import { defineConfig } from 'vitepress'

export default defineConfig({
  // 网站基础信息
  title: 'ActX',
  description: 'ActX - Java 通用业务智能体应用，轻量级、高扩展、无侵入',

  base: '/ActX/',

  head: [
    ['link', {
      rel: 'icon',
      type: 'image/png',
      href: 'favicon.png'
    }]
  ],

  // 全局主题配置
  themeConfig: {
    // 导航栏 Logo
    logo: '/favicon.png',

    search: {
      provider: 'local'
    },

    // 社交链接
    socialLinks: [
      { icon: 'github', link: 'https://github.com/jiang2992/ActX' }
    ]
  },

  // 中英文站点配置
  locales: {
    root: {
      label: '简体中文',
      lang: 'zh-CN',
      link: '/',
      themeConfig: {
        nav: [
          { text: '首页', link: '/' },
          { text: '开发文档', link: '/info' },
          {
            text: 'GitHub',
            link: 'https://github.com/jiang2992/ActX'
          }
        ],
        sidebar: [
          {
            text: '开发文档',
            items: [
              { text: '项目介绍', link: '/info' },
            ]
          }
        ]
      }
    },
    en: {
      label: 'English',
      lang: 'en-US',
      link: '/en/',
      title: 'ActX',
      description: 'ActX - Java General-Purpose Business Agent Application',
      themeConfig: {
        nav: [
          { text: 'Home', link: '/en/' },
          { text: 'Documentation', link: '/en/info' },
          {
            text: 'GitHub',
            link: 'https://github.com/jiang2992/ActX'
          }
        ],
        sidebar: [
          {
            text: 'Documentation',
            items: [
              { text: 'Introduction', link: '/en/info' },
            ]
          }
        ]
      }
    }
  }
})
