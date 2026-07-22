package com.company.supervision.domain.model.messaging;
import com.baomidou.mybatisplus.annotation.*;import lombok.Data;import java.time.LocalDateTime;
@Data @TableName("supervision_message_delivery") public class MessageDelivery{
 @TableId(type=IdType.AUTO)Long id;Long taskId;Long executionId;String channel;String targetSnapshot;String messageType;String idempotencyKey;String requestBatch;Integer wecomErrcode;String status;String failureReason;Integer retryCount;LocalDateTime sentAt;LocalDateTime createdAt;LocalDateTime updatedAt;
 Long webhookId;Long groupIdSnapshot;String groupNameSnapshot;String pushNameSnapshot;String contentSummarySnapshot;String normalizedCode;String normalizedMessage;String technicalDetailRedacted;
}
