package com.company.supervision.domain.model.messaging;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("supervision_wecom_group")
public class WecomGroup {
    @TableId(type = IdType.AUTO) private Long id;
    private String tenantKey;
    private Long ownerAccountId;
    private String groupName;
    private Integer status;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
