package com.company.supervision.entity.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class RobotView extends AccountScopedView {
    private Long id;
    private Long groupId;
    private String robotId;
    private String groupName;
    private String pushName;
    private String webhookUrl;
    private String template;
    private Integer pushStatus;
    private Boolean isPublic;
    private boolean canUse;
    private LocalDateTime lastTestedAt;
    private LocalDateTime createdAt;
}
