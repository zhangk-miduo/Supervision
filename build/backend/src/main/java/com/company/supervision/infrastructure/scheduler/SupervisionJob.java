package com.company.supervision.infrastructure.scheduler;

import com.company.supervision.domain.service.TaskExecutionEngine;
import com.company.supervision.infrastructure.config.SpringContextHolder;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Quartz 作业：触发时加载任务节点链并执行。通过 SpringContextHolder 获取引擎 Bean。
 */
public class SupervisionJob implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Long taskId = context.getJobDetail().getJobDataMap().getLong("taskId");
        TaskExecutionEngine engine = SpringContextHolder.getBean(TaskExecutionEngine.class);
        try {
            engine.execute(taskId);
        } catch (Exception e) {
            throw new JobExecutionException("任务执行失败 taskId=" + taskId, e, false);
        }
    }
}
