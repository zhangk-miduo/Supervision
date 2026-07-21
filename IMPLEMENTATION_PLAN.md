# IMPLEMENTATION_PLAN.md — Supervision 企业智能督办平台 MVP

> 基于 `doc/产品规划文档.md` 与 `doc/技术分析文档.md`，将文档落地为可部署系统。
> 技术栈：Java 17 / Spring Boot 3.2.2 / MyBatis-Plus / MySQL 8 / Redis 7 / RabbitMQ 3 / Quartz / Vue3+TS+Vite+Element Plus。
> 部署：Docker Compose（本地仅有 JDK8，后端经 `maven:3.9-eclipse-temurin-17` 多阶段镜像编译运行）。
> 工程形态：单 Maven 模块 `supervision-platform`，内部按 DDD 包分层（api/application/domain/infrastructure/entity），兼顾文档架构意图与构建可靠性。

任务按里程碑有序排列，Phase 3 构建循环依次消费。

---

## M1 — 后端骨架与配置

### Task 1: Maven 工程与依赖
Files: create `build/backend/pom.xml`
Verification: `docker compose run --rm maven mvn -B -q compile` 通过（依赖可解析）
内容：Spring Boot 3.2.2 父工程；依赖 spring-boot-starter-web、spring-boot-starter-data-redis、spring-boot-starter-amqp、mybatis-plus-boot-starter、mysql-connector-j、quartz、spring-boot-starter-validation、spring-boot-starter-actuator、lombok、spring-boot-starter-test。Java 17。

### Task 2: 启动类与全局配置
Files: create `build/backend/src/main/java/com/company/supervision/SupervisionApplication.java`, `build/backend/src/main/resources/application.yml`
Verification: 容器启动后 `curl -f http://localhost:8080/api/health` 返回 ok
内容：@SpringBootApplication；yml 配置端口 8080、MySQL/Redis/RabbitMQ 连接（环境变量）、MyBatis-Plus、Quartz（JDBC 存储）、Jackson 时区。

### Task 3: 基础设施配置类
Files: create `infrastructure/config/MybatisPlusConfig.java`, `RedisConfig.java`, `RabbitMQConfig.java`, `QuartzConfig.java`, `WebClientConfig.java`
Verification: 编译通过
内容：MyBatis-Plus 分页拦截器；RedisTemplate（JSON 序列化）；RabbitMQ 队列/交换机声明；Quartz SchedulerFactoryBean（指向 DataSource）；WebClient.Builder Bean。

---

## M2 — 领域模型与持久化

### Task 4: 领域实体与枚举
Files: create `domain/model/AutomationTask.java`, `TaskNode.java`, `TaskExecution.java`, `WechatRobot.java`, `TaskSchedule.java`；`domain/model/enumeration/*`
Verification: 编译通过
内容：字段对应技术文档 §5/§10 DDL；状态枚举（TaskStatus、ExecutionStatus、NodeType、ConditionOperator）。

### Task 5: MyBatis-Plus Mapper
Files: create `infrastructure/repository/*Mapper.java`（Task/Node/Execution/Robot/Schedule）
Verification: 编译通过
内容：继承 BaseMapper；XML 或注解方式；分页查询方法。

### Task 6: SQL 初始化脚本
Files: create `build/backend/src/main/resources/db/01_schema.sql`
Verification: `docker compose exec mysql mysql -usupervision -psupervision supervision -e "SHOW TABLES;"` 列出 5 张表
内容：技术文档 §10 的 5 张表 DDL + Quartz 表（由 Quartz 自动建表，spring.quartz.jdbc.initialize-schema=always）。

---

## M3 — 节点执行器、引擎、MQ、企微客户端

### Task 7: 执行上下文与结果
Files: create `domain/service/ExecutionContext.java`, `NodeResult.java`, `NodeExecutor.java`（接口）
Verification: 编译通过
内容：ExecutionContext 持有 Map<String,Object> 与 executionId；NodeResult{status,data,message}。

### Task 8: HTTP 节点执行器
Files: create `infrastructure/executor/HttpNodeExecutor.java`
Verification: 单测或手动：对某 GET 接口返回结果写入 context
内容：用 WebClient 调 config.url/method/headers/body，结果存入 context（key=nodeId）。

### Task 9: Condition 节点执行器
Files: create `infrastructure/executor/ConditionNodeExecutor.java`
Verification: 对 field/operator/value 判断正确（=,!=,>,<,>=,<=,contains,empty）
内容：从 context 取 field 值，按 operator 比较，返回 pass/fail；引擎据此决定是否继续。

### Task 10: 企业微信客户端与节点执行器
Files: create `infrastructure/client/WechatClient.java`, `infrastructure/executor/WechatNodeExecutor.java`
Verification: `POST /api/robots/{id}/test` 返回企微返回码
内容：WechatClient POST Webhook（markdown/text）；WechatNodeExecutor 取 robot 与 template 发送。

### Task 11: 执行引擎
Files: create `domain/service/TaskExecutionEngine.java`
Verification: 手动执行一个含 http→condition→wechat 的任务链成功落库
内容：按 node_order 取节点链，依次执行；Redis 上下文 + 分布式锁；异常记录；写 TaskExecution。

### Task 12: RabbitMQ 异步通知
Files: create `infrastructure/mq/NotificationProducer.java`, `NotificationConsumer.java`
Verification: 投递消息后消费者调用 WechatClient 成功
内容：引擎将通知投 `supervision.notification.queue`；消费者发送，失败记录。

---

## M4 — 控制器、应用服务、调度

### Task 13: 应用服务
Files: create `application/TaskAppService.java`, `RobotAppService.java`, `ExecutionAppService.java`, `SchedulerAppService.java`
Verification: 编译通过
内容：用例编排；任务 CRUD、手动执行（调用引擎）、机器人 CRUD/测试、执行查询、调度注册/移除。

### Task 14: REST 控制器
Files: create `api/TaskController.java`, `RobotController.java`, `ExecutionController.java`, `HealthController.java`
Verification: `curl` 各端点 2xx
内容：路由同技术文档 §13（/api/tasks, /api/robots, /api/executions, /api/health, /api/tasks/{id}/execute, /api/robots/{id}/test）。

### Task 15: Quartz 调度集成
Files: create `infrastructure/scheduler/SupervisionJob.java`, 在 SchedulerAppService 注册 Trigger
Verification: 建一个每 1 分钟 cron 的任务，观察自动执行落库
内容：Job 触发→取节点链→调用引擎；启停同步 Trigger。

---

## M5 — 前端管理后台

### Task 16: 前端工程脚手架
Files: create `build/web/package.json`, `vite.config.ts`, `tsconfig.json`, `index.html`, `src/main.ts`, `src/App.vue`, `src/router/index.ts`
Verification: `cd build/web && npm install && npm run build` 成功
内容：Vue3+TS+Vite+Element Plus+Pinia+Vue Router；代理 /api → :8080。

### Task 17: API 层与状态
Files: create `src/api/index.ts`（axios 封装 + 各接口）
Verification: 编译通过
内容：tasks/robots/executions 接口封装。

### Task 18: 页面
Files: create `src/views/dashboard/index.vue`, `src/views/task/*`, `src/views/robot/*`, `src/views/execution/*`, `src/components/NodeForm.vue`
Verification: `npm run build` 成功；页面可创建任务并手动执行
内容：仪表盘、任务管理（含节点配置表单）、机器人管理（含测试）、执行日志。

---

## M6 — Docker 部署

### Task 19: 后端 Dockerfile
Files: create `build/backend/Dockerfile`
Verification: `docker compose build supervision-api` 成功
内容：多阶段（maven:3.9-eclipse-temurin-17 编译 → eclipse-temurin:17-jre 运行）。

### Task 20: Nginx 托管前端
Files: create `docker/nginx/Dockerfile`, `docker/nginx/nginx.conf`
Verification: 前端页面可访问，/api 反代到后端
内容：nginx 托管 dist，反代 /api。

### Task 21: Docker Compose
Files: create `docker-compose.yml`
Verification: `docker compose up -d` 全绿
内容：mysql(8)+redis(7)+rabbitmq(3)+supervision-api+nginx(前端构建阶段)；卷持久化；env 注入；healthcheck。

---

## M7 — 构建、冒烟、文档

### Task 22: 构建与冒烟测试
Files: （无新增，执行验证）
Verification: 全流程跑通——创建任务(含 http+condition+wechat 节点) → 手动执行 → 查执行日志成功
内容：修复编译/运行问题直至闭环。

### Task 23: 部署文档
Files: create `DEPLOYMENT.md`, 更新 `README.md`
Verification: 文档可执行
内容：一键部署步骤、环境变量、默认账号、扩展示例。
