# idolnoOJ 在线判题系统

idolnoOJ 是一个支持多语言代码提交的在线判题平台，内置竞赛日历、AI 辅助问答、题目检索和独立沙箱服务，适合比赛训练和课程实验场景。

## 核心功能

- Docker 沙箱隔离执行用户代码，采用容器池机制复用运行中的容器，减少创建销毁开销，提升执行效率
- 判题策略可扩展，支持自定义测试用例和多语言编译运行
- RabbitMQ 异步调度判题任务，Redis 缓存热点数据，提高并发能力
- 自动抓取编程竞赛信息，提供日历和订阅提醒
- AI 问答模块为用户提供题解思路和代码优化建议

## 系统结构

```
idolnoOJ/
├── oj_backend/         后端服务，基于 Spring Boot
├── oj_front/           前端应用，基于 Vue 3
├── oj_codesandbox/     独立代码沙箱服务
├── database.sql        初始化数据库脚本
└── README.md
```

## 判题工作流

```
用户提交 →
oj_front (调用 API) →
oj_backend (写入数据库 / 推送 RabbitMQ judge.exchange) →
oj_codesandbox (JUDGE_QUEUE 执行判题) →
oj_codesandbox (RESULT_QUEUE 回写结果) →
oj_backend (结果入库 / 推送 Redis) →
前端轮询/推送展示
```

## 技术栈

**后端**  
Spring Boot 2.7、MyBatis、MySQL 8.0、Redis 7.0、RabbitMQ 3.8、Docker 20.0、JWT

**前端**  
Vue 3、Pinia、Vue Router、Vite、Element Plus

**开发工具**  
Maven、Node.js 16、IntelliJ IDEA 或 VS Code、Docker Desktop

## 运行前置条件

- Java 11 或更高
- Node.js 16 与 npm
- MySQL 8.0（初始化执行 `database.sql`）
- Redis 7.0
- RabbitMQ 3.8（开启虚拟主机与用户权限）
- Docker 20+（远程 API `tcp://localhost:2375` 开启并限制访问）
- Python 3.9（用于 `python/` 目录下的竞赛抓取脚本）与 `requests` 等依赖

## 快速开始

1. **准备环境**  
   安装 Java 17、Node.js 16、MySQL 8.0、Redis 7.0、Docker 20.0、RabbitMQ 3.8。

2. **克隆项目**
   ```bash
   git clone https://github.com/XiaoZhuDaBai/idolnoOJ.git
   cd idolnoOJ
   ```

3. **启动后端**
   ```bash
   cd oj_backend
   mvn clean install
   mvn spring-boot:run
   ```

4. **启动前端**
   ```bash
   cd oj_front
   npm install
   npm run dev
   ```

5. **启动代码沙箱**
   ```bash
   cd oj_codesandbox
   mvn clean install
   mvn spring-boot:run
   ```

6. **初始化数据库**
   ```bash
   mysql -u root -p < database.sql
   ```

## 配置说明

- **数据库**  
  编辑 `oj_backend/src/main/resources/application.yml`，补全数据库地址、用户和密码：
  ```yaml
  spring:
    datasource:
      url: jdbc:mysql://localhost:3306/idolnooj
      username: your_username
      password: your_password
  ```

- **Redis**  
  ```
  spring:
    redis:
      host: localhost
      port: 6379
      password: your_password
  ```

- **Docker**  
  启动 Docker 服务并确认当前用户具备运行容器的权限。

## 配置管理

- 建议使用环境变量或外部化配置（如 `application-prod.yml` + `SPRING_CONFIG_LOCATION`）覆盖敏感信息：数据库、Redis、RabbitMQ、DashScope、邮箱授权码等。
- 关键键值：
  - `spring.datasource.*`
  - `spring.redis.*`
  - `spring.rabbitmq.*`
  - `dashscope.appId`
  - `feedback.email.*`
- 生产环境请禁用仓库中的默认凭据，统一通过部署平台密钥或 `.env` 文件注入。

## 运维建议

- 判题服务依赖 RabbitMQ 队列：`judge.exchange/judge.queue`、`result.exchange/result.queue`、`judge.dlx.queue`。监控队列积压并设置告警。
- `oj_codesandbox` 提供 Actuator 端点（`/actuator/health`、`/actuator/metrics`、`/actuator/containerPool`）用于容器池健康检查。
- 定时抓取任务读取 `schedule.contest.time`（默认 3 小时），爬虫脚本位于 `oj_backend/python/`，请定期更新依赖并校验运行环境。

## 功能截图

![首页](oj_backend/img/首页.png)
![题库](oj_backend/img/题库.png)
![答题面板](oj_backend/img/答题面板.png)
![答题面板控制台](oj_backend/img/答题面板的控制台.png)
![竞赛日历](oj_backend/img/竞赛日历.png)
![提交记录](oj_backend/img/查询提交.png)

## 许可证

本项目使用 MIT License，详情见 `LICENSE`。
