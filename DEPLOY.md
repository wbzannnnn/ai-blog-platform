# Railway 全栈部署步骤

GitHub 仅保存源码；Railway 负责运行 MySQL、Spring Boot 后端和 Vue 前端。

## 一、推送到 GitHub

把本仓库推送到一个新的 GitHub 仓库，例如 `ai-blog-platform`。

## 二、创建 Railway 项目

1. 登录 Railway 并选择 **New Project**。
2. 添加 **MySQL** 服务。
3. 再添加两个 **Deploy from GitHub repo** 服务，均选择本仓库。

## 三、后端服务

- Root Directory：`/backend`
- Builder：自动识别 Dockerfile
- Variables：

```text
DB_URL=jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
DB_USERNAME=${{MySQL.MYSQLUSER}}
DB_PASSWORD=${{MySQL.MYSQLPASSWORD}}
DASHSCOPE_API_KEY=你的通义千问 API Key
JWT_SECRET=至少 32 字节的随机字符串
CORS_ALLOWED_ORIGINS=https://你的前端域名
```

部署后在 **Settings → Networking** 生成后端公网域名。

数据库初始化：打开 MySQL 服务的数据管理工具，执行 `database/schema.sql`。如果希望空库启动，也可先执行其中的建表语句。

## 四、前端服务

- Root Directory：`/frontend`
- Builder：自动识别 Dockerfile
- Variables：

```text
BACKEND_URL=https://你的后端域名
```

部署后在 **Settings → Networking** 生成前端公网域名。

最后回到后端，把 `CORS_ALLOWED_ORIGINS` 设置为该前端域名并重新部署。

## 五、验证

1. 打开前端公网域名。
2. 注册一个新用户并登录。
3. 发布文章、评论和点赞。
4. 配置有效 DashScope Key 后验证 AI 生成和智能检索。

## 文件上传注意事项

Railway 容器文件系统不适合长期保存用户上传内容。演示可直接使用；长期运行建议为头像和附件接入对象存储（OSS、S3 或 R2）。
