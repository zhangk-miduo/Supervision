package com.company.supervision.entity.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class TaskView extends AccountScopedView {
    private Long id;
    private String name;
    private String description;
    private Integer status;
    private Integer scheduleType;
    private String messageDefinition;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
