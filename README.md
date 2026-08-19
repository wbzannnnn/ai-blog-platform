# AI Blog Platform

一个前后端分离的 AI 内容创作与博客分析平台。

## 技术栈

- 前端：Vue 3、TypeScript、Vite、Element Plus、Pinia
- 后端：Java 17、Spring Boot、Spring Security、JWT、MyBatis-Plus
- AI：Spring AI Alibaba、DashScope
- 数据库：MySQL 8

## 功能

- 用户注册登录、JWT 鉴权和角色权限控制
- 文章发布、审核、评论、点赞和标签管理
- AI 文章生成、摘要、标签和内容审核
- 智能文章检索与多轮对话
- 标签热度、趋势、热门文章和互动率分析
- 用户端与管理端页面

## 本地运行

### 1. 数据库

创建 MySQL 数据库后执行：

```sql
source database/schema.sql;
```

### 2. 后端

复制示例配置或设置环境变量：

```bash
DB_URL=jdbc:mysql://localhost:3306/db_ai_blog
DB_USERNAME=root
DB_PASSWORD=your_password
DASHSCOPE_API_KEY=your_dashscope_key
JWT_SECRET=replace_with_a_long_random_secret
```

启动：

```bash
cd backend
./mvnw spring-boot:run
```

### 3. 前端

```bash
cd frontend
npm ci
npm run dev
```

开发环境默认通过 Vite 代理访问 `http://localhost:8080`。

## Railway 部署

本仓库按 Monorepo 方式部署三个服务：

1. MySQL：在 Railway 项目中添加 MySQL 服务。
2. 后端：Root Directory 设为 `/backend`，配置数据库、AI 和 JWT 环境变量。
3. 前端：Root Directory 设为 `/frontend`，配置 `BACKEND_URL` 为后端 Railway 公网域名。

详细步骤见 [DEPLOY.md](DEPLOY.md)。

## 安全说明

- 仓库不包含真实数据库密码、DashScope API Key 或 JWT Secret。
- 请只通过部署平台环境变量注入密钥。
- 如果密钥曾经写入或上传到公开位置，请立即轮换。
