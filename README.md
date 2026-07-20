# Supervision
企业智能督办平台 将企业中依赖人工推动的事情，通过自动化规则变成系统主动发现、主动提醒、主动跟进。

选择 **Supervision** 是一个更偏企业管理产品定位的命名，相比 ExecFlow，它更贴近你最初的产品愿景：

> 企业智能督办平台
> 帮助企业把“靠人记忆推动的管理动作”，变成自动触发、自动提醒、自动反馈的执行流程。

我认为这个名字更适合作为企业内部平台产品名。

不过建议不要直接使用 `Supervision` 作为代码仓库名，因为英文语义上有几个问题：

* Supervision 本身更偏“监督、监管、监控”
* 容易让人理解成：

  * 安防监控
  * 运维监控
  * 人员监管
* 缺少“自动化执行”的含义

建议采用：

# 推荐项目名称

## **Supervision**

全称：

# Enterprise Intelligent Supervision Platform

中文：

# 企业智能督办平台

---

# 代码仓库命名建议

推荐：

```
supervision-platform
```

或者：

```
supervision
```

企业级项目更推荐：

```
supervision-platform
```

因为未来可能扩展：

```
supervision-platform

├── supervision-server
├── supervision-web
├── supervision-gateway
├── supervision-job
├── supervision-docs

```

---

# Maven模块命名建议

结合你当前 Java + Spring Boot + DDD 架构：

```
com.company.supervision
```

模块：

```
supervision

├── supervision-api

├── supervision-application

├── supervision-domain

├── supervision-infrastructure

├── supervision-entity

└── supervision-web

```

---

# 数据库命名

建议：

```
supervision
```

表：

```
supervision_task

supervision_task_node

supervision_task_execution

supervision_wechat_robot

supervision_schedule

```

---

# Redis Key规范

例如：

任务锁：

```
supervision:task:lock:{taskId}
```

执行上下文：

```
supervision:execution:{executionId}
```

---

# RabbitMQ命名

Exchange：

```
supervision.exchange
```

Queue：

```
supervision.notification.queue
```

RoutingKey：

```
supervision.wechat.send
```

---

# README.md（Supervision版）

下面是调整后的项目 README。

---

```markdown
# Supervision

## Enterprise Intelligent Supervision Platform

企业智能督办与自动化执行平台


---

## 1. 项目介绍


Supervision 是一个面向企业内部管理场景的智能督办自动化平台。


通过：

- 定时触发
- 数据查询
- 规则判断
- 消息通知


帮助企业将大量依赖人工推动的管理事项自动化。


---

## 产品理念


传统管理模式：


人工检查

↓

发现问题

↓

人工通知

↓

人工跟进



Supervision：


系统自动触发

↓

自动查询数据

↓

智能判断

↓

自动提醒

↓

记录执行结果



---

## 2. 核心能力


### 自动化任务 Task


创建企业管理任务。


例如：

- 日报提醒
- 周报提醒
- 项目进度提醒
- 数据异常提醒
- 系统巡检提醒



---

### 流程节点 Node


任务由多个节点组成：


```

Trigger

↓

HTTP Request

↓

Condition

↓

Notification

```



支持节点：

|节点|说明|
|-|-|
|HTTP Node|调用业务接口|
|Condition Node|规则判断|
|Wechat Node|企业微信通知|



未来支持：

- SQL Node
- Email Node
- DingTalk Node
- Feishu Node
- AI Node



---

## 3. 技术架构


```

```
         Supervision


              |

       Web Management


              |

      Spring Boot API


              |
```

---

MySQL

Redis

RabbitMQ

```
              |

    Task Execution Engine


              |
```

---

HTTP

Condition

Wechat

```



---

## 4. 技术栈


Backend:

- Java 17
- Spring Boot 3
- MyBatis-Plus
- MySQL 8
- Redis 7
- RabbitMQ
- Quartz


Frontend:

- Vue 3
- TypeScript
- Vite
- Element Plus
- Pinia



Deployment:

- Docker Compose



---

## 5. 核心模型


### Task


自动化任务。


```

Task

id

name

status

schedule

```



---

### TaskNode


执行节点。


```

TaskNode

id

taskId

nodeType

config

order

```



---

### Execution


执行记录。


```

TaskExecution

id

taskId

status

result

time

```



---

## 6. 执行流程


示例：

日报提醒



```

17:30

↓

Quartz触发

↓

HTTP查询日报数据

↓

判断未提交人数

↓

发送企业微信

↓

保存执行日志

```



---

## 7. 项目结构


```

supervision-platform

├── supervision-server

├── supervision-api

├── supervision-domain

├── supervision-application

├── supervision-infrastructure

├── supervision-web

├── docs

└── docker

```



---

## 8. Roadmap


### V1.0

企业微信自动督办


完成：

- 任务管理
- 定时调度
- HTTP节点
- 条件节点
- 企业微信通知
- 执行日志



---


### V2.0


组织能力增强：

- 企业微信组织同步
- 动态@人员
- 用户权限
- 多租户



---


### V3.0


智能化：

- AI生成督办任务
- AI分析执行风险
- 企业智能运营助手



---

## 9. Design Principle


### 简单

不做复杂BPM。


### 可扩展

通过Node模型扩展能力。


### 企业级

支持：

- Docker部署
- 高可用
- 日志审计
- 权限扩展



---

## Supervision


让企业管理，从人工推动走向智能督办。

```


