package com.company.supervision.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("supervision_task_node")
public class TaskNode {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    /** http / condition / wechat */
    private String nodeType;

    /** 执行顺序 */
    private Integer nodeOrder;

    /** 节点配置（JSON 字符串） */
    private String config;

    private LocalDateTime createdAt;
}
