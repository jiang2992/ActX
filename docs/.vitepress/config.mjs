import {defineConfig} from 'vitepress'

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
      {icon: 'github', link: 'https://github.com/jiang2992/ActX'}
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
          {text: '首页', link: '/'},
          {text: '开发文档', link: '/info'},
          {
            text: 'GitHub',
            link: 'https://github.com/jiang2992/ActX'
          }
        ],
        sidebar: [
          {
            text: '开发文档',
            items: [
              {text: '项目介绍', link: '/info'},
              {
                text: '安装部署',
                items: [
                  {text: '快速运行', link: '/quick-start'},
                  {text: '图数据库', link: '/graph-database'},
                  {text: '配置沙箱', link: '/sandbox-config'},
                  {text: '日志配置', link: '/log-config'},
                ]
              },
              {text: '会话管理'},
              {text: '文件管理'},
              {text: '复杂任务'},
              {text: '业务认知'},
              {text: '沙箱管理'},
              {
                text: '能力扩展',
                items: [
                  {text: '开发建议'},
                  {text: '工具包'},
                  {text: '模型配置'},
                  {text: 'Agent配置'},
                  {text: 'MCP配置'},
                  {text: '消息通道'},
                  {text: '消息片段'},
                  {text: '扩展插件'},
                  {text: '数据源'},
                  {text: 'SQL解析与校验'},
                  {text: 'SQL重写（权限控制）'},
                  {text: '常见问题'}
                ]
              },
              {text: '关于ActX'}
            ],
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
          {text: 'Home', link: '/en/'},
          {text: 'Documentation', link: '/en/info'},
          {
            text: 'GitHub',
            link: 'https://github.com/jiang2992/ActX'
          }
        ],
        sidebar: [
          {
            text: 'Documentation',
            items: [
              {text: 'Introduction', link: '/en/info'},
              {
                text: 'Installation & Deployment',
                items: [
                  {text: 'Quick Start', link: '/en/quick-start'},
                  {text: 'Graph Database'},
                  {text: 'Sandbox Configuration'},
                  {text: 'Log Configuration'},
                ]
              },
              {text: 'Session Management'},
              {text: 'File Management'},
              {text: 'Complex Tasks'},
              {text: 'Business Discovery'},
              {text: 'Sandbox Management'},
              {
                text: 'Capability Extension',
                items: [
                  {text: 'Development Suggestions'},
                  {text: 'Toolkit'},
                  {text: 'Model Configuration'},
                  {text: 'Agent Configuration'},
                  {text: 'MCP Configuration'},
                  {text: 'Message Channel'},
                  {text: 'Message Fragment'},
                  {text: 'Extension Plugin'},
                  {text: 'Data Source'},
                  {text: 'SQL Parsing & Validation'},
                  {text: 'SQL Rewriting (Access Control)'},
                  {text: 'FAQ'}
                ]
              },
              {text: 'About ActX'}
            ],
          }
        ]
      }
    }
  }
})
