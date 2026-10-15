import { defineConfig } from 'vitepress'

export default defineConfig({
  // 网站基础信息
  title: 'ACTX',
  description: 'ACTX Documentation',

  // 如果仓库名是 github-actx，保留此配置
  // 如果使用自定义域名，后续再调整
  base: '/github-actx/',

  // 本地搜索
  themeConfig: {
    search: {
      provider: 'local'
    },

    // 顶部导航
    nav: [
      { text: '首页', link: '/' },
      { text: '快速开始', link: '/getting-started' },
      {
        text: 'GitHub',
        link: 'https://github.com/你的用户名/github-actx'
      }
    ],

    // 中文侧边栏
    sidebar: [
      {
        text: '开始使用',
        items: [
          { text: '项目介绍', link: '/' },
          { text: '快速开始', link: '/getting-started' },
          { text: '安装部署', link: '/installation' }
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
      title: 'ACTX',
      description: 'ACTX Documentation'
    }
  }
})