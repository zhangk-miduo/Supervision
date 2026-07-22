package com.company.supervision.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SelectableRobot {
    private Long id;
    private Long groupId;
    private String groupName;
    private String pushName;
    private String label;
    private Boolean isPublic;
    private Long creatorAccountId;
    private String creatorUsername;
    private String creatorDisplayName;
}
