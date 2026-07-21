package com.company.supervision.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("supervision_task_schedule")
public class TaskSchedule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private String cronExpression;

    /** 1 启用 0 停用 */
    private Integer status;
}
