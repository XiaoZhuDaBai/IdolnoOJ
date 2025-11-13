# oj_codesandbox 使用指南

`oj_codesandbox` 是 idolnoOJ 的独立判题服务，负责消费 RabbitMQ 判题任务、调度容器执行用户代码，并将结果回写到队列供后端更新数据库。

## 运行信息

- 默认端口：`8082`
- 关键队列与交换机：
  - 判题任务：`judge.exchange` → `judge.queue`（`judge.routing.key`）
  - 判题结果：`result.exchange` → `result.queue`（`result.routing.key`）
  - 死信交换机：`judge.dlx` → `judge.dlx.queue`
- RabbitMQ 连接配置位于 `src/main/resources/application.yml` 的 `spring.rabbitmq` 节点。

## 启动步骤

1. 确保 Docker、RabbitMQ、MySQL、Redis 已启动，且 `tcp://localhost:2375` Docker API 可访问。
2. 配置 `application.yml` 中的数据库 / RabbitMQ / Redis / 沙箱参数，可通过环境变量覆盖（推荐在生产环境外置配置）。
3. 执行
   ```bash
   mvn spring-boot:run
   ```
4. 打开 `http://localhost:8082/actuator/health` 检查服务是否上线。

## 容器池配置

`sandbox` 节点定义容器池：

- `docker.host`：Docker API 地址
- `languages`：各语言镜像、超时时间、内存/CPU 限制、容器池大小
- `monitoring.health-check-interval`：容器健康检查间隔（毫秒）
- `security`：文件大小限制、允许的系统调用等

扩容或新增语言：

1. 在 `application.yml` 的 `sandbox.languages` 内新增语言块。
2. 设置镜像、启动命令、`pool-size`。如需新镜像，提前拉取或通过私有仓库分发。
3. 重启服务后容器池会按照新配置预热。

## 监控与排错

- 通过 `/actuator/metrics/containerPool.*` 查看池内容器数量、等待任务等指标。
- `/actuator/containerPool` 暴露池状态详情（需在 `management.endpoints.web.exposure.include` 中开启）。
- 若任务进入 `judge.dlx.queue`，说明超过重试次数或消息格式异常，可使用 RabbitMQ 管理界面或 CLI 检查死信理由。
- 常见故障：
  - **镜像缺失**：日志出现 `No such image`，手动拉取或更新 `image-name`。
  - **Docker API 不可达**：确认 Docker 启动并允许远程访问，防火墙放行 2375。
  - **容器耗尽**：提高 `pool-size` 或优化题目测试数据；监控 `judge.queue` 堆积情况。

## 判题结果回写

消费 `result.queue` 的 `DatabaseUpdateConsumer` 会更新判题记录及题目统计，业务失败会调用 `handleUpdateFailure`，可在此扩展重试策略或报警。

## 本地开发建议

- 启用 Spring Boot DevTools 以便热重载。
- 使用 `docker stats` 实时观察沙箱资源消耗。
- 如需模拟判题任务，可手动向 `judge.queue` 写入 `ExecuteCodeRequest` JSON，或通过后端接口提交代码。

更多实现细节参考：

```14:122:src/main/java/oj/oj_codesandbox/judge/rabbitmq/RabbitMQConfig.java
// ... existing code ...
```

