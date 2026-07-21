package com.company.supervision.domain.service;

import com.company.supervision.domain.model.enumeration.ExecutionStatus;
import com.company.supervision.domain.model.TaskExecution;
import com.company.supervision.domain.model.TaskNode;
import com.company.supervision.domain.model.enumeration.NodeType;
import com.company.supervision.infrastructure.mq.NotificationProducer;
import com.company.supervision.infrastructure.repository.ExecutionMapper;
import com.company.supervision.infrastructure.repository.TaskNodeMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TaskExecutionEngine {

    private static final String LOCK_PREFIX = "supervision:task:lock:";
    private static final String CTX_PREFIX = "supervision:task:context:";

    private final Map<String, NodeExecutor> executors;
    private final TaskNodeMapper nodeMapper;
    private final ExecutionMapper executionMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final NotificationProducer notificationProducer;

    public TaskExecutionEngine(List<NodeExecutor> nodeExecutors,
                               TaskNodeMapper nodeMapper,
                               ExecutionMapper executionMapper,
                               RedisTemplate<String, Object> redisTemplate,
                               NotificationProducer notificationProducer) {
        this.executors = nodeExecutors.stream().collect(Collectors.toMap(NodeExecutor::nodeType, e -> e));
        this.nodeMapper = nodeMapper;
        this.executionMapper = executionMapper;
        this.redisTemplate = redisTemplate;
        this.notificationProducer = notificationProducer;
    }

    /**
     * 执行某个任务的完整节点链。
     *
     * @return 执行记录 id
     */
    public Long execute(Long taskId) {
        String lockKey = LOCK_PREFIX + taskId;
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofMinutes(10));
        if (locked == null || !locked) {
            throw new IllegalStateException("任务 " + taskId + " 正在执行中（分布式锁已占用）");
        }
        Long execId;
        try {
            TaskExecution record = new TaskExecution();
            record.setTaskId(taskId);
            record.setStatus(ExecutionStatus.RUNNING.getCode());
            record.setStartTime(LocalDateTime.now());
            record.setResult("{}");
            executionMapper.insert(record);
            execId = record.getId();

            ExecutionContext ctx = new ExecutionContext(execId);
            List<TaskNode> nodes = nodeMapper.selectByTaskIdOrdered(taskId);
            StringBuilder log = new StringBuilder();

            for (TaskNode node : nodes) {
                NodeType type = NodeType.of(node.getNodeType());
                NodeExecutor executor = executors.get(type.getCode());
                if (executor == null) {
                    log.append("[").append(type.getCode()).append("]SKIP(无执行器) ");
                    continue;
                }
                NodeResult result = executor.execute(node, ctx);
                log.append("[").append(type.getCode()).append("]")
                        .append(result.isSuccess() ? "OK" : "FAIL").append(" ");
                if (!result.isSuccess()) {
                    // condition 节点不满足条件、或任意节点执行失败，均记为本次执行失败并停止链路
                    record.setStatus(ExecutionStatus.FAILED.getCode());
                    record.setResult(log.toString());
                    record.setEndTime(LocalDateTime.now());
                    executionMapper.updateById(record);
                    return execId;
                }
            }

            record.setStatus(ExecutionStatus.SUCCESS.getCode());
            record.setResult(log.toString());
            record.setEndTime(LocalDateTime.now());
            executionMapper.updateById(record);

            try {
                redisTemplate.opsForValue().set(CTX_PREFIX + execId, new LinkedHashMap<>(ctx.getData()), Duration.ofHours(1));
            } catch (Exception ignore) {
                // 上下文持久化失败不影响主链路
            }
            try {
                notificationProducer.publishAudit(taskId, execId, record.getStatus());
            } catch (Exception ignore) {
                // 审计事件发送失败不影响主链路
            }
            return execId;
        } finally {
            redisTemplate.delete(lockKey);
        }
    }
}
