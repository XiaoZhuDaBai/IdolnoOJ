# oj_front 开发指南

`oj_front` 基于 Vue 3 + Vite + Element Plus，实现题库、竞赛日历、提交记录、AI 助手等页面。

## 环境准备

- Node.js 16+
- npm 或 pnpm（示例使用 npm）
- 推荐 IDE：VS Code + Volar（禁用 Vetur）

## 环境变量

在项目根目录创建 `.env.local` 或修改现有 `.env`：

```
VITE_APP_API_BASE=https://localhost:8081/api   # 后端网关地址
VITE_APP_WS_BASE=wss://localhost:8081/api/ws   # 若使用 WebSocket
VITE_APP_AI_ENDPOINT=https://localhost:8081/api/ai/chat
```

开发环境可指向代理服务器，生产部署前请根据实际域名修改。

## 常用脚本

```bash
npm install            # 安装依赖
npm run dev            # 启动开发服务器 (默认 5173)
npm run build          # 构建生产包
npm run preview        # 预览生产构建
npm run test:unit      # 运行单元测试 (Vitest)
npm run lint           # ESLint 检查
```

建议在 `vite.config.ts` 中配置代理，将 `/api` 请求转发到后端服务：

```ts
server: {
  proxy: {
    '/api': {
      target: process.env.VITE_APP_API_BASE,
      changeOrigin: true
    }
  }
}
```

## 主要页面

- `/`：首页概览，展示公告与热门题目
- `/problem`：题库检索与筛选
- `/problem/:id`：题目详情与代码编辑器，支持多语言提交
- `/calendar`：竞赛日历，读取 Redis 缓存的赛事信息
- `/record`：提交记录/我的错题
- `/ai`：AI 助手，调用后端 DashScope 代理

路由定义位于 `src/router/index.ts`，状态管理使用 Pinia。

## 构建部署

1. 执行 `npm run build`，生成静态资源于 `dist/`
2. 根据部署环境选择托管方案：
   - Nginx：
     ```nginx
     server {
       listen 80;
       server_name your-domain.com;
       root /var/www/oj_front/dist;
       location / {
         try_files $uri $uri/ /index.html;
       }
       location /api/ {
         proxy_pass http://backend:8081/api/;
         proxy_set_header Host $host;
         proxy_set_header X-Real-IP $remote_addr;
       }
     }
     ```
   - 其他静态托管（如 Vercel、Netlify）需配置后端 API 代理。

## 与判题服务联调

- 提交代码会调用 `POST /api/question/submit`，需确保 `oj_backend` 与 `oj_codesandbox` 已运行。
- `result.exchange` 更新后，前端通过轮询提交详情接口刷新结果。
- 本地调试时可借助 Mock 数据：在 `src/mocks` 新建模拟 API，并使用 Vite 插件启用。

## UI 自定义

- 主题变量位于 `src/styles/variables.scss`
- 组件封装在 `src/components`，常见如代码编辑器、判题状态标签等
- 图标库使用 `@iconify/vue`

如遇构建问题，请检查依赖版本或执行 `npm cache clean --force` 后重新安装。更多细节参考源码及注释。
