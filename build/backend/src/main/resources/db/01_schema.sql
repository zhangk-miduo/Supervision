-- Supervision 平台初始化表结构（MySQL 8）
-- Quartz 调度表由 Spring Boot（spring.quartz.jdbc.initialize-schema=always）自动创建，此处仅建业务表。

USE supervision;

-- 应用连接账号（与 application.yml 中 DB_USERNAME / DB_PASSWORD 保持一致）
CREATE USER IF NOT EXISTS 'supervision'@'%' IDENTIFIED BY 'supervision';
GRANT ALL PRIVILEGES ON supervision.* TO 'supervision'@'%';
FLUSH PRIVILEGES;

CREATE TABLE IF NOT EXISTS supervision_task (
    id           BIGINT       PRIMARY KEY AUTO_INCREMENT,
    name         VARCHAR(128) NOT NULL COMMENT '任务名称',
    description  VARCHAR(512) DEFAULT NULL COMMENT '任务描述',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    schedule_type TINYINT     NOT NULL DEFAULT 0 COMMENT '0手动 1定时',
    created_by   VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='督办任务';

CREATE TABLE IF NOT EXISTS supervision_task_node (
    id         BIGINT       PRIMARY KEY AUTO_INCREMENT,
    task_id    BIGINT       NOT NULL COMMENT '关联任务',
    node_type  VARCHAR(32)  NOT NULL COMMENT 'http/condition/wechat',
    node_order INT          NOT NULL COMMENT '执行顺序',
    config     JSON         NOT NULL COMMENT '节点配置',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_task_node_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程节点';

CREATE TABLE IF NOT EXISTS supervision_task_execution (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    task_id     BIGINT       NOT NULL COMMENT '关联任务',
    status      TINYINT      NOT NULL COMMENT '0成功 1失败 2执行中',
    result      TEXT         DEFAULT NULL COMMENT '执行结果摘要',
    start_time  DATETIME     NOT NULL,
    end_time    DATETIME     DEFAULT NULL,
    INDEX idx_exec_task (task_id),
    INDEX idx_exec_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='执行记录';

CREATE TABLE IF NOT EXISTS supervision_wechat_robot (
    id           BIGINT       PRIMARY KEY AUTO_INCREMENT,
    robot_id     VARCHAR(64)  NOT NULL COMMENT '机器人标识',
    name         VARCHAR(128) NOT NULL COMMENT '机器人名称',
    webhook_url  VARCHAR(512) NOT NULL COMMENT 'Webhook 地址',
    template     VARCHAR(1024) DEFAULT NULL COMMENT '消息模板',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_robot_id (robot_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业微信机器人';

CREATE TABLE IF NOT EXISTS supervision_task_schedule (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    task_id         BIGINT       NOT NULL COMMENT '关联任务',
    cron_expression VARCHAR(64)  NOT NULL COMMENT 'Cron 表达式',
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    UNIQUE KEY uk_schedule_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调度配置';
