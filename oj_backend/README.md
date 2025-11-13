# oj_backend 模块说明

`oj_backend` 是 idolnoOJ 的核心后端服务，基于 Spring Boot 开发，负责题库管理、提交评测、用户鉴权、竞赛信息抓取与通知等功能。

## 模块架构

- `controller/`：HTTP 接口层，提供题库、提交、AI、反馈等 REST API。
- `service/` 与 `service/impl/`：业务逻辑实现，操作数据库、缓存和消息队列。
- `mapper/` 与 `resources/mapper/`：MyBatis-Plus Mapper 与 XML。
- `util/`：工具类（邮件、JWT、Redis 包装、Python 进程调度）。
- `python/`：定时抓取竞赛数据的脚本集合。

## 主要流程

1. **提交判题**：`QuestionController` → `UserService` 保存提交记录 → RabbitMQ `judge.exchange` 发布任务 → 等待沙箱回写结果。
2. **结果同步**：`oj_codesandbox` 将结果推送 `result.exchange` → `DatabaseUpdateConsumer` 更新 `CommitCase`、`Question` 统计 → Redis 缓存刷新。
3. **竞赛抓取**：`OjBackendApplication#fetchContests` 定时执行 Python 爬虫，结果写入 Redis `oj:contest:*` SortedSet，前端通过 `/fetch/contests` 拉取。

## 配置要点

`src/main/resources/application.yml` 中的敏感配置建议通过环境变量覆盖：

- `spring.datasource.*`：MySQL 连接信息。
- `spring.redis.*`：Redis 数据库（默认使用 index 3 存放竞赛数据与缓存）。
- `spring.rabbitmq.*`：RabbitMQ 凭证与虚拟主机。
- `dashscope.appId`：阿里云百炼应用 Key。
- `feedback.email.*`：邮件发送账号与授权码。

生产环境可以设置：

```bash
export SPRING_DATASOURCE_URL=...
export SPRING_REDIS_HOST=...
export DASH_SCOPE_APP_ID=...
```

并在 `application.yml` 中使用 `${ENV_KEY:default}` 读取。

## Redis 键约定

- `oj:contest:all`：存储竞赛 JSON（SortedSet，score 为开始时间）。
- `oj:question:{id}`：题目详情缓存。
- `oj:user:token:{uuid}`：用户登陆态（JWT 或 Session）。

根据业务扩展时注意设置 TTL 与序列化方式。

## 鉴权策略

- 基于 JWT：`JWTUtil` 负责签发和验证 Token。
- `SecurityConfig` 自定义路径放行策略，默认开放登录、注册等接口。
- 需要保护的 API 应在 Controller 上新增权限检查。

## Python 爬虫

脚本位于 `python/` 目录，依赖 `requests`、`beautifulsoup4` 等三方库。运行流程：

```bash
cd oj_backend/python
pip install -r requirements.txt  # 如需自定义依赖请创建该文件
python atcoder_contest.py
```

定时任务通过 `ProcessBuilder` 调用系统 `python` 命令，部署环境需提前安装 Python3，并将脚本目录与虚拟环境配置好。

## 数据库与迁移

- 初始结构位于根目录 `database.sql`。
- 推荐引入 Flyway/Liquibase 管控增量迁移；在此之前可使用 `mvn flyway:migrate` 等插件或手动执行 SQL。
- 重大变更请更新 SQL 文件并在 README/CHANGELOG 中记录。

## 测试与调试

- 单元测试：`mvn test`
- 关键测试类：
  - `service/FetchServiceTest`：竞赛抓取逻辑
  - `EmailUtilTest`：邮件发送
  - `JavaUsePythonTest`：Python 调用验证
- 可在 `application-dev.yml` 中使用内存数据库或本地 Redis/RabbitMQ 进行集成测试。

## 常见问题

- **竞赛列表为空**：检查 Redis 连接、Python 环境和 `schedule.contest.time` 设置。
- **邮件发送失败**：确认邮箱 SMTP 授权码有效，并开启 SSL/TLS 配置。
- **判题队列堆积**：观察 `judge.queue` 消息数，可能是沙箱不可用或网络阻塞。

更多细节可参考源码注释及 `SQL优化分析报告.md`。

