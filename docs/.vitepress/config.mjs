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
              {text: '会话管理', link: '/session-manage'},
              {text: '文件管理', link: '/file-manage'},
              {text: '复杂任务', link: '/task-flow'},
              {text: '业务认知', link: '/business-discover'},
              {text: '沙箱管理', link: '/sandbox-manage'},
              {
                text: '能力扩展',
                items: [
                  {text: '开发建议'},
                  {text: '工具包', link: '/extension-toolkit'},
                  {text: '模型配置', link: '/extension-model'},
                  {text: 'Agent配置', link: '/extension-agent'},
                  {text: 'MCP配置', link: '/extension-mcp'},
                  {text: '消息通道', link: '/extension-channel'},
                  {text: '消息片段', link: '/extension-fragment'},
                  {text: '扩展插件', link: '/extension-plugin'},
                  {text: '数据源', link: '/extension-datasource'},
                  {text: 'SQL解析与校验', link: '/extension-sql'},
                  {text: 'SQL重写（权限控制）', link: '/extension-rewrite'},
                  {text: '常见问题', link: '/extension-faq'}
                ]
              },
              {text: '关于ActX', link: '/about'}
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
