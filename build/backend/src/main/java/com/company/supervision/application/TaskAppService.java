package com.company.supervision.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.supervision.domain.model.AutomationTask;
import com.company.supervision.domain.model.enumeration.ExecutionStatus;
import com.company.supervision.domain.model.TaskNode;
import com.company.supervision.domain.model.TaskSchedule;
import com.company.supervision.domain.model.enumeration.ScheduleStatus;
import com.company.supervision.domain.model.enumeration.TaskStatus;
import com.company.supervision.domain.service.TaskExecutionEngine;
import com.company.supervision.entity.dto.TaskCreateRequest;
import com.company.supervision.entity.dto.TaskDetail;
import com.company.supervision.infrastructure.repository.ExecutionMapper;
import com.company.supervision.infrastructure.repository.ScheduleMapper;
import com.company.supervision.infrastructure.repository.TaskMapper;
import com.company.supervision.infrastructure.repository.TaskNodeMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class TaskAppService {

    private final TaskMapper taskMapper;
    private final TaskNodeMapper nodeMapper;
    private final ScheduleMapper scheduleMapper;
    private final ExecutionMapper executionMapper;
    private final TaskExecutionEngine executionEngine;
    private final SchedulerAppService schedulerAppService;

    public TaskAppService(TaskMapper taskMapper,
                          TaskNodeMapper nodeMapper,
                          ScheduleMapper scheduleMapper,
                          ExecutionMapper executionMapper,
                          TaskExecutionEngine executionEngine,
                          SchedulerAppService schedulerAppService) {
        this.taskMapper = taskMapper;
        this.nodeMapper = nodeMapper;
        this.scheduleMapper = scheduleMapper;
        this.executionMapper = executionMapper;
        this.executionEngine = executionEngine;
        this.schedulerAppService = schedulerAppService;
    }

    @Transactional
    public Long createTask(TaskCreateRequest req) {
        AutomationTask task = new AutomationTask();
        task.setName(req.getName());
        task.setDescription(req.getDescription());
        task.setScheduleType(req.getScheduleType() == null ? 0 : req.getScheduleType());
        task.setStatus(TaskStatus.ENABLED.getCode());
        task.setCreatedBy(req.getCreatedBy());
        taskMapper.insert(task);
        Long taskId = task.getId();

        saveNodes(taskId, req.getNodes());

        if (isCron(req) && hasText(req.getCronExpression())) {
            TaskSchedule s = new TaskSchedule();
            s.setTaskId(taskId);
            s.setCronExpression(req.getCronExpression());
            s.setStatus(ScheduleStatus.ENABLED.getCode());
            scheduleMapper.insert(s);
            schedulerAppService.register(taskId, req.getCronExpression());
        }
        return taskId;
    }

    @Transactional
    public void updateTask(Long id, TaskCreateRequest req) {
        AutomationTask task = taskMapper.selectById(id);
        if (task == null) throw new IllegalArgumentException("任务不存在: " + id);
        task.setName(req.getName());
        task.setDescription(req.getDescription());
        task.setScheduleType(req.getScheduleType() == null ? 0 : req.getScheduleType());
        taskMapper.updateById(task);

        // 重新编排节点
        nodeMapper.delete(new LambdaQueryWrapper<TaskNode>().eq(TaskNode::getTaskId, id));
        saveNodes(id, req.getNodes());

        // 重新编排调度
        TaskSchedule existing = scheduleMapper.selectByTaskId(id);
        if (isCron(req) && hasText(req.getCronExpression())) {
            schedulerAppService.register(id, req.getCronExpression());
            if (existing == null) {
                TaskSchedule s = new TaskSchedule();
                s.setTaskId(id);
                s.setCronExpression(req.getCronExpression());
                s.setStatus(ScheduleStatus.ENABLED.getCode());
                scheduleMapper.insert(s);
            } else {
                existing.setCronExpression(req.getCronExpression());
                existing.setStatus(ScheduleStatus.ENABLED.getCode());
                scheduleMapper.updateById(existing);
            }
        } else {
            if (existing != null) scheduleMapper.deleteById(existing.getId());
            schedulerAppService.remove(id);
        }
    }

    @Transactional
    public void deleteTask(Long id) {
        nodeMapper.delete(new LambdaQueryWrapper<TaskNode>().eq(TaskNode::getTaskId, id));
        TaskSchedule s = scheduleMapper.selectByTaskId(id);
        if (s != null) scheduleMapper.deleteById(s.getId());
        schedulerAppService.remove(id);
        taskMapper.deleteById(id);
    }

    public IPage<AutomationTask> listTasks(String name, int page, int size) {
        Page<AutomationTask> p = new Page<>(Math.max(page, 1), Math.max(size, 1));
        LambdaQueryWrapper<AutomationTask> w = new LambdaQueryWrapper<>();
        if (hasText(name)) w.like(AutomationTask::getName, name);
        w.orderByDesc(AutomationTask::getId);
        return taskMapper.selectPage(p, w);
    }

    public TaskDetail getTask(Long id) {
        AutomationTask task = taskMapper.selectById(id);
        if (task == null) throw new IllegalArgumentException("任务不存在: " + id);
        TaskDetail detail = new TaskDetail();
        detail.setTask(task);
        detail.setNodes(nodeMapper.selectByTaskIdOrdered(id));
        detail.setSchedule(scheduleMapper.selectByTaskId(id));
        return detail;
    }

    public Long executeNow(Long id) {
        AutomationTask task = taskMapper.selectById(id);
        if (task == null) throw new IllegalArgumentException("任务不存在: " + id);
        return executionEngine.execute(id);
    }

    private void saveNodes(Long taskId, List<TaskCreateRequest.NodeInput> nodes) {
        if (nodes == null) return;
        for (TaskCreateRequest.NodeInput n : nodes) {
            TaskNode node = new TaskNode();
            node.setTaskId(taskId);
            node.setNodeType(n.getNodeType());
            node.setNodeOrder(n.getNodeOrder());
            node.setConfig(n.getConfig());
            nodeMapper.insert(node);
        }
    }

    private boolean isCron(TaskCreateRequest req) {
        return req.getScheduleType() != null && req.getScheduleType() == 1;
    }

    private boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
