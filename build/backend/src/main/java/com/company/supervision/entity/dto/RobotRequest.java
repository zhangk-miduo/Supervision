package com.company.supervision.entity.dto;

import lombok.Data;

@Data
public class RobotRequest {
    private String robotId;
    private String name;
    private String webhookUrl;
    private String template;
}
