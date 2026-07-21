package com.company.supervision.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("supervision_task")
public class AutomationTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    /** 1 启用 0 停用 */
    private Integer status;

    /** 0 手动 1 定时 */
    private Integer scheduleType;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
