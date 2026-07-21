package com.company.supervision.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("supervision_task_execution")
public class TaskExecution {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    /** 0 成功 1 失败 2 执行中 */
    private Integer status;

    /** 执行结果摘要（文本/JSON） */
    private String result;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
