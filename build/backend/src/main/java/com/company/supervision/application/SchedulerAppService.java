package com.company.supervision.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.supervision.domain.model.TaskSchedule;
import com.company.supervision.infrastructure.repository.ScheduleMapper;
import com.company.supervision.infrastructure.scheduler.SupervisionJob;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;

@Slf4j
@Service
public class SchedulerAppService {

    private final Scheduler scheduler;
    private final ScheduleMapper scheduleMapper;

    public SchedulerAppService(Scheduler scheduler, ScheduleMapper scheduleMapper) {
        this.scheduler = scheduler;
        this.scheduleMapper = scheduleMapper;
    }

    public void register(Long taskId, String cron) {
        try {
            String jobId = "task-" + taskId;
            JobDetail job = JobBuilder.newJob(SupervisionJob.class)
                    .withIdentity(jobId)
                    .storeDurably()
                    .build();
            job.getJobDataMap().put("taskId", taskId);

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("trig-" + taskId)
                    .forJob(job)
                    .withSchedule(CronScheduleBuilder.cronSchedule(cron))
                    .build();

            if (scheduler.checkExists(job.getKey())) {
                scheduler.deleteJob(job.getKey());
            }
            scheduler.scheduleJob(job, trigger);
            log.info("注册定时任务 taskId={} cron={}", taskId, cron);
        } catch (Exception e) {
            throw new RuntimeException("注册调度失败: " + e.getMessage(), e);
        }
    }

    public void remove(Long taskId) {
        try {
            scheduler.deleteJob(JobKey.jobKey("task-" + taskId));
            log.info("移除定时任务 taskId={}", taskId);
        } catch (Exception e) {
            log.warn("移除调度失败 taskId={}: {}", taskId, e.getMessage());
        }
    }

    @PostConstruct
    public void init() {
        try {
            List<TaskSchedule> all = scheduleMapper.selectList(
                    new LambdaQueryWrapper<TaskSchedule>().eq(TaskSchedule::getStatus, 1));
            for (TaskSchedule s : all) {
                register(s.getTaskId(), s.getCronExpression());
            }
            log.info("已恢复 {} 个启用的定时任务", all.size());
        } catch (Exception e) {
            log.warn("恢复定时任务失败（可忽略，首次启动或调度表未就绪）: {}", e.getMessage());
        }
    }
}
