package com.company.supervision.entity.dto;

import lombok.Data;

import java.util.List;

@Data
public class TaskCreateRequest {

    private String name;
    private String description;
    /** 0 手动 1 定时 */
    private Integer scheduleType = 0;
    private String createdBy;
    /** 定时任务的 Cron 表达式（scheduleType=1 时必填） */
    private String cronExpression;
    /** 流程节点（有序） */
    private List<NodeInput> nodes;

    @Data
    public static class NodeInput {
        private String nodeType;
        private Integer nodeOrder;
        private String config;
    }
}
