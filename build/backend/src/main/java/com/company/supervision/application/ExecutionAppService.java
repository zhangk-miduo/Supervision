package com.company.supervision.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.supervision.api.ResourceNotFoundException;
import com.company.supervision.application.identity.CreatorViewService;
import com.company.supervision.domain.model.TaskExecution;
import com.company.supervision.domain.model.messaging.MessageDelivery;
import com.company.supervision.entity.dto.*;
import com.company.supervision.infrastructure.repository.ExecutionMapper;
import com.company.supervision.infrastructure.repository.messaging.MessageDeliveryMapper;
import com.company.supervision.infrastructure.security.DataScope;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.*;

@Service
public class ExecutionAppService {
    private final ExecutionMapper executions;private final MessageDeliveryMapper deliveries;private final CreatorViewService creators;
    public ExecutionAppService(ExecutionMapper e,MessageDeliveryMapper d,CreatorViewService creators){executions=e;deliveries=d;this.creators=creators;}

    public IPage<ExecutionSummary>listExecutions(Long taskId,String status,Long creatorAccountId,int page,int size,DataScope scope){
        if(creatorAccountId!=null&&!scope.admin())throw new IllegalArgumentException("Creator filter requires administrator permission");
        Page<TaskExecution>p=new Page<>(Math.max(page,1),Math.max(size,1));LambdaQueryWrapper<TaskExecution>w=new LambdaQueryWrapper<>();
        if(taskId!=null)w.eq(TaskExecution::getTaskId,taskId);Integer code=code(status);if(code!=null)w.eq(TaskExecution::getStatus,code);
        if(creatorAccountId!=null)w.eq(TaskExecution::getOwnerAccountId,creatorAccountId);else if(!scope.admin())w.eq(TaskExecution::getOwnerAccountId,scope.accountId());
        w.orderByDesc(TaskExecution::getId);IPage<TaskExecution>raw=executions.selectPage(p,w);Page<ExecutionSummary>out=new Page<>(raw.getCurrent(),raw.getSize(),raw.getTotal());out.setRecords(raw.getRecords().stream().map(e->summary(e,scope)).toList());return out;
    }

    public ExecutionDetail getExecution(Long id,DataScope scope){TaskExecution e=executions.selectById(id);if(e==null||!scope.canRead(e.getOwnerAccountId()))throw new ResourceNotFoundException("执行记录不存在");ExecutionDetail d=new ExecutionDetail();d.setSummary(summary(e,scope));d.setDeliveries(deliveries.selectList(new LambdaQueryWrapper<MessageDelivery>().eq(MessageDelivery::getExecutionId,id).orderByAsc(MessageDelivery::getId)));return d;}

    private ExecutionSummary summary(TaskExecution e,DataScope scope){ExecutionSummary x=new ExecutionSummary();x.setId(e.getId());x.setTaskId(e.getTaskId());x.setTaskName(e.getTaskNameSnapshot()==null?"历史任务 #"+e.getTaskId():e.getTaskNameSnapshot());x.setTriggerType(e.getTriggerType()==null?"UNKNOWN":e.getTriggerType());x.setStatus(status(e.getStatus()));x.setStatusLabel(label(e.getStatus(),e.getSuccessCount(),e.getTargetCount()));x.setResultSummary(readableResult(e));x.setMessageType(e.getMessageTypeSnapshot());x.setMessageSummary(e.getMessageSummarySnapshot()==null?"历史记录未保存推送内容":e.getMessageSummarySnapshot());x.setTargetCount(e.getTargetCount()==null?0:e.getTargetCount());x.setSuccessCount(e.getSuccessCount()==null?0:e.getSuccessCount());x.setStartTime(e.getStartTime());x.setEndTime(e.getEndTime());x.setDurationMillis(e.getStartTime()!=null&&e.getEndTime()!=null?Duration.between(e.getStartTime(),e.getEndTime()).toMillis():null);x.setScheduleDecision(e.getScheduleDecision());x.setScheduleDecisionReason(e.getScheduleDecisionReason());x.setSnapshotComplete(Objects.equals(e.getSnapshotComplete(),1));x.setTriggeredByAccountId(e.getTriggeredByAccountId());creators.fill(x,e.getOwnerAccountId(),scope);return x;}
    private String readableResult(TaskExecution e){if(e.getScheduleDecision()!=null&&!"EXECUTE".equals(e.getScheduleDecision()))return"已跳过："+e.getScheduleDecisionReason();if(Objects.equals(e.getStatus(),0))return e.getTargetCount()!=null&&e.getTargetCount()>0?"全部推送成功 "+e.getSuccessCount()+"/"+e.getTargetCount():"执行成功";if(Objects.equals(e.getStatus(),3))return"部分成功 "+e.getSuccessCount()+"/"+e.getTargetCount();if(Objects.equals(e.getStatus(),2))return"正在执行";return e.getResult()==null?"执行失败":e.getResult();}
    private Integer code(String s){if(s==null||s.isBlank())return null;return switch(s){case"SUCCESS"->0;case"FAILED"->1;case"RUNNING"->2;case"PARTIAL_SUCCESS"->3;default->null;};}private String status(Integer c){return switch(c==null?2:c){case 0->"SUCCESS";case 1->"FAILED";case 3->"PARTIAL_SUCCESS";default->"RUNNING";};}private String label(Integer c,Integer ok,Integer total){return switch(c==null?2:c){case 0->"成功";case 1->"失败";case 3->"部分成功 "+(ok==null?0:ok)+"/"+(total==null?0:total);default->"执行中";};}
}
