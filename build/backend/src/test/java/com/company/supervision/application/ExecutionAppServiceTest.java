package com.company.supervision.application;

import com.company.supervision.api.ResourceNotFoundException;
import com.company.supervision.application.identity.CreatorViewService;
import com.company.supervision.domain.model.TaskExecution;
import com.company.supervision.infrastructure.repository.ExecutionMapper;
import com.company.supervision.infrastructure.repository.messaging.MessageDeliveryMapper;
import com.company.supervision.infrastructure.security.DataScope;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ExecutionAppServiceTest {
    @Test void rejectsCrossAccountExecutionDetail(){ExecutionMapper mapper=mock(ExecutionMapper.class);TaskExecution execution=new TaskExecution();execution.setId(9L);execution.setOwnerAccountId(2L);when(mapper.selectById(9L)).thenReturn(execution);ExecutionAppService service=new ExecutionAppService(mapper,mock(MessageDeliveryMapper.class),mock(CreatorViewService.class));assertThatThrownBy(()->service.getExecution(9L,new DataScope(1L,false))).isInstanceOf(ResourceNotFoundException.class);}
}
