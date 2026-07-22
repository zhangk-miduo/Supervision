package com.company.supervision.application;

import com.company.supervision.api.ResourceNotFoundException;
import com.company.supervision.application.identity.CreatorViewService;
import com.company.supervision.application.scheduling.AdvancedScheduleService;
import com.company.supervision.domain.model.AutomationTask;
import com.company.supervision.domain.service.TaskExecutionEngine;
import com.company.supervision.entity.dto.TaskCreateRequest;
import com.company.supervision.infrastructure.repository.*;
import com.company.supervision.infrastructure.repository.messaging.TaskRecipientMapper;
import com.company.supervision.infrastructure.security.DataScope;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TaskAppServiceTest {
    @Test void createsTaskWithAuthenticatedOwner(){
        TaskMapper mapper=mock(TaskMapper.class);doAnswer(i->{AutomationTask t=i.getArgument(0);t.setId(10L);return 1;}).when(mapper).insert(any());
        TaskAppService service=service(mapper);TaskCreateRequest req=new TaskCreateRequest();req.setName("daily");req.setScheduleType(0);
        assertThat(service.createTask(req,new DataScope(7L,false))).isEqualTo(10L);
        verify(mapper).insert(argThat(t->Long.valueOf(7L).equals(t.getOwnerAccountId())&&t.getCreatedBy()==null));
    }

    @Test void rejectsMutationOfAnotherAccountsTask(){
        TaskMapper mapper=mock(TaskMapper.class);AutomationTask task=new AutomationTask();task.setId(10L);task.setOwnerAccountId(8L);when(mapper.selectById(10L)).thenReturn(task);
        assertThatThrownBy(()->service(mapper).updateTask(10L,new TaskCreateRequest(),new DataScope(7L,true))).isInstanceOf(ResourceNotFoundException.class);
    }

    private TaskAppService service(TaskMapper mapper){return new TaskAppService(mapper,mock(TaskNodeMapper.class),mock(ScheduleMapper.class),mock(ExecutionMapper.class),mock(TaskExecutionEngine.class),mock(SchedulerAppService.class),mock(AdvancedScheduleService.class),mock(TaskRecipientMapper.class),new ObjectMapper(),mock(RobotAppService.class),mock(CreatorViewService.class));}
}
