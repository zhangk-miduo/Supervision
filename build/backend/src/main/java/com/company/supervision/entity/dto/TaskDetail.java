package com.company.supervision.entity.dto;

import com.company.supervision.domain.model.AutomationTask;
import com.company.supervision.domain.model.TaskNode;
import com.company.supervision.domain.model.TaskSchedule;
import lombok.Data;

import java.util.List;

@Data
public class TaskDetail {
    private AutomationTask task;
    private List<TaskNode> nodes;
    private TaskSchedule schedule;
}
