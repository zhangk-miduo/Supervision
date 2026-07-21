package com.company.supervision.domain.service;

import com.company.supervision.domain.model.TaskNode;

/**
 * 节点执行器统一接口。新增节点类型只需新增实现并注册为 Spring Bean。
 */
public interface NodeExecutor {

    /**
     * 该执行器支持的节点类型，对应 supervision_task_node.node_type（http/condition/wechat）。
     */
    String nodeType();

    /**
     * 执行节点。
     *
     * @param node 节点定义（含 config JSON）
     * @param ctx  执行上下文（前序节点结果可读取/写入）
     * @return 执行结果；condition 节点不满足条件时返回 fail，引擎据此停止链路
     */
    NodeResult execute(TaskNode node, ExecutionContext ctx);
}
