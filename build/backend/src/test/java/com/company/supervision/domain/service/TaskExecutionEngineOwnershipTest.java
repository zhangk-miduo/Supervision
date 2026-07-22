package com.company.supervision.domain.service;

import com.company.supervision.application.messaging.MessageService;
import com.company.supervision.domain.model.*;
import com.company.supervision.infrastructure.mq.*;
import com.company.supervision.infrastructure.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.*;
import java.time.Duration;
import java.util.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TaskExecutionEngineOwnershipTest {
    @Test void scheduledExecutionCopiesTaskOwner(){
        TaskMapper tasks=mock(TaskMapper.class);AutomationTask task=new AutomationTask();task.setId(1L);task.setName("任务");task.setOwnerAccountId(7L);when(tasks.selectById(1L)).thenReturn(task);
        ExecutionMapper executions=mock(ExecutionMapper.class);doAnswer(i->{TaskExecution e=i.getArgument(0);e.setId(11L);return 1;}).when(executions).insert(any());
        RedisTemplate<String,Object> redis=mock(RedisTemplate.class);ValueOperations<String,Object> values=mock(ValueOperations.class);when(redis.opsForValue()).thenReturn(values);when(values.setIfAbsent(anyString(),any(),any(Duration.class))).thenReturn(true);
        TaskNodeMapper nodes=mock(TaskNodeMapper.class);when(nodes.selectByTaskIdOrdered(1L)).thenReturn(List.of());MessageService messages=mock(MessageService.class);when(messages.targetIds(anyMap())).thenReturn(List.of());
        TaskExecutionEngine engine=new TaskExecutionEngine(List.of(),nodes,executions,redis,mock(NotificationProducer.class),tasks,messages,new ObjectMapper());
        engine.execute(1L,"SCHEDULED");
        verify(executions).insert(argThat(e->Long.valueOf(7L).equals(e.getOwnerAccountId())&&"SCHEDULED".equals(e.getTriggerType())));
    }

    @Test void skippedExecutionCopiesTaskOwner(){
        TaskMapper tasks=mock(TaskMapper.class);AutomationTask task=new AutomationTask();task.setId(1L);task.setName("任务");task.setOwnerAccountId(7L);when(tasks.selectById(1L)).thenReturn(task);
        ExecutionMapper executions=mock(ExecutionMapper.class);TaskExecutionEngine engine=new TaskExecutionEngine(List.of(),mock(TaskNodeMapper.class),executions,mock(RedisTemplate.class),mock(NotificationProducer.class),tasks,mock(MessageService.class),new ObjectMapper());
        engine.recordSkipped(1L,"HOLIDAY","休息日");
        verify(executions).insert(argThat(e->Long.valueOf(7L).equals(e.getOwnerAccountId())&&"SCHEDULED".equals(e.getTriggerType())));
    }
}
