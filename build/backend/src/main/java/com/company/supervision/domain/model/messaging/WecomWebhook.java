package com.company.supervision.domain.model.messaging;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("supervision_wecom_webhook")
public class WecomWebhook {
    @TableId(type=IdType.AUTO) private Long id;
    private Long groupId;
    private String name;
    private String pushName;
    private String systemCode;
    private String webhookCipher;
    private Integer status;
    private LocalDateTime lastTestedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
