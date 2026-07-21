package com.company.supervision.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.supervision.domain.model.TaskExecution;
import com.company.supervision.infrastructure.repository.ExecutionMapper;
import org.springframework.stereotype.Service;

@Service
public class ExecutionAppService {

    private final ExecutionMapper executionMapper;

    public ExecutionAppService(ExecutionMapper executionMapper) {
        this.executionMapper = executionMapper;
    }

    public IPage<TaskExecution> listExecutions(Long taskId, Integer status, int page, int size) {
        Page<TaskExecution> p = new Page<>(Math.max(page, 1), Math.max(size, 1));
        LambdaQueryWrapper<TaskExecution> w = new LambdaQueryWrapper<>();
        if (taskId != null) w.eq(TaskExecution::getTaskId, taskId);
        if (status != null) w.eq(TaskExecution::getStatus, status);
        w.orderByDesc(TaskExecution::getId);
        return executionMapper.selectPage(p, w);
    }

    public TaskExecution getExecution(Long id) {
        TaskExecution ex = executionMapper.selectById(id);
        if (ex == null) throw new IllegalArgumentException("执行记录不存在: " + id);
        return ex;
    }
}
