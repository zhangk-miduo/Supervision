package com.company.supervision.domain.model;
import com.baomidou.mybatisplus.annotation.*;import lombok.Data;import java.time.LocalDateTime;
@Data @TableName("supervision_task_execution") public class TaskExecution{
 @TableId(type=IdType.AUTO)private Long id;private Long taskId;private Integer status;private String result;private LocalDateTime startTime;private LocalDateTime endTime;
 private String taskNameSnapshot;private String triggerType;private String messageTypeSnapshot;private String messageSummarySnapshot;private Integer targetCount;private Integer successCount;private String scheduleDecision;private String scheduleDecisionReason;private Integer snapshotComplete;
}
