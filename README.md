# Esshop - 校园二手交易平台

基于 Spring Boot 4 + Vue 3 的校园二手交易平台，包含商品交易、社区、即时聊天、智能客服与语义搜索。

## 项目结构 

```
Esshop/
├── src/                  # Spring Boot 后端（Java 17）
├── frontend/             # 用户端前端（Vue 3 + Element Plus）
├── admin/                # 管理后台前端（Vue 3 + Element Plus）
├── ai-service/           # 智能客服服务（Python + FastAPI + LangChain + Chroma）
└── sql/                  # 数据库脚本
```

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot 4.1、MyBatis-Plus、MySQL、Redis、JWT、WebSocket、SpringDoc OpenAPI |
| 前端 | Vue 3、Vite、Element Plus、Axios、WebSocket |
| 智能客服 | Python、FastAPI、LangChain、Chroma 向量库、Ollama / OpenAI 兼容接口 |

## 功能模块

**用户端**
- 商品发布、浏览、搜索（关键词 + 语义搜索）、收藏、卖家评价
- 订单流程：下单、支付（模拟）、取消、完成，状态机校验
- 社区：发帖（管理员审核）、点赞、收藏、评论
- 私信：WebSocket 实时聊天、已读回执、在线状态
- 用户主页：关注/粉丝、卖家评价展示
- 智能客服：大模型问答，失败自动降级到关键词规则库

**管理后台**
- 数据看板、用户管理、商品审核、订单管理、社区审核、轮播图管理、客服知识库

## 快速开始

### 环境要求

- JDK 17、Maven 3.8+
- Node.js 18+
- MySQL 8、Redis 6
- （可选）Python 3.10+ 与 Ollama，用于智能客服与语义搜索

### 1. 数据库

```bash
mysql -u root -p < sql/schema.sql
mysql -u root -p esshop < sql/migration_chat_data.sql
```

默认管理员：`admin / admin123`（生产环境请修改）

### 2. 后端

修改 `src/main/resources/application.yml` 中的 MySQL / Redis 连接信息，然后：

```bash
mvn spring-boot:run
```

服务地址：http://localhost:8080 ，接口文档：http://localhost:8080/swagger-ui.html

### 3. 用户端前端

```bash
cd frontend
npm install
npm run dev
```

地址：http://localhost:3000 （开发模式已配置 /api、/uploads、/ws 代理到 8080）

### 4. 管理后台

```bash
cd admin
npm install
npm run dev
```

地址：http://localhost:5173

### 5. 智能客服（可选）

不启动该服务时，客服自动降级为关键词规则库，商品语义搜索回退到关键词搜索，主流程不受影响。

```bash
cd ai-service
copy .env.example .env    # 填入 LLM_API_KEY
pip install -r requirements.txt
python sync_knowledge.py            # 同步知识库到向量库
python sync_knowledge.py --products # 同步商品（语义搜索用）
python main.py
```

服务地址：http://localhost:8000 ，接口文档：http://localhost:8000/docs

## 接口约定

- 统一响应：`{ code, message, data }`，`code = 200` 为成功
- 鉴权：`Authorization: Bearer <token>`，JWT 有效期 24h，Redis 存储登录态
- WebSocket：`ws://localhost:8080/ws/chat?token=xxx`

## License

MIT
