package com.company.supervision.domain.service;

import com.company.supervision.application.messaging.MessageService;
import com.company.supervision.domain.model.*;
import com.company.supervision.domain.model.enumeration.*;
import com.company.supervision.domain.model.messaging.MessageDelivery;
import com.company.supervision.infrastructure.mq.NotificationProducer;
import com.company.supervision.infrastructure.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.*;import java.util.*;import java.util.stream.Collectors;

@Slf4j @Service public class TaskExecutionEngine{
 private static final String LOCK_PREFIX="supervision:task:lock:";private static final String CTX_PREFIX="supervision:task:context:";
 private final Map<String,NodeExecutor>executors;private final TaskNodeMapper nodeMapper;private final ExecutionMapper executionMapper;private final RedisTemplate<String,Object>redisTemplate;private final NotificationProducer notificationProducer;private final TaskMapper taskMapper;private final MessageService messageService;private final ObjectMapper objectMapper;
 public TaskExecutionEngine(List<NodeExecutor>nodeExecutors,TaskNodeMapper nodeMapper,ExecutionMapper executionMapper,RedisTemplate<String,Object>redisTemplate,NotificationProducer notificationProducer,TaskMapper taskMapper,MessageService messageService,ObjectMapper objectMapper){this.executors=nodeExecutors.stream().collect(Collectors.toMap(NodeExecutor::nodeType,x->x));this.nodeMapper=nodeMapper;this.executionMapper=executionMapper;this.redisTemplate=redisTemplate;this.notificationProducer=notificationProducer;this.taskMapper=taskMapper;this.messageService=messageService;this.objectMapper=objectMapper;}
 public Long execute(Long taskId){return execute(taskId,"MANUAL");}
 public Long execute(Long taskId,String triggerType){
  String lockKey=LOCK_PREFIX+taskId;Boolean locked=redisTemplate.opsForValue().setIfAbsent(lockKey,"1",Duration.ofMinutes(10));if(locked==null||!locked)throw new IllegalStateException("任务正在执行中，请稍后重试");
  try{
   AutomationTask task=taskMapper.selectById(taskId);if(task==null)throw new IllegalArgumentException("任务不存在: "+taskId);
   Map<String,Object>definition=parse(task.getMessageDefinition());List<Long>targets=messageService.targetIds(definition);
   TaskExecution record=new TaskExecution();record.setTaskId(taskId);record.setStatus(ExecutionStatus.RUNNING.getCode());record.setStartTime(LocalDateTime.now());record.setResult("执行中");record.setTaskNameSnapshot(task.getName());record.setTriggerType(triggerType);record.setMessageTypeSnapshot(string(definition.get("messageType")));record.setMessageSummarySnapshot(summary(definition));record.setTargetCount(targets.size());record.setSuccessCount(0);record.setScheduleDecision("EXECUTE");record.setScheduleDecisionReason("满足执行条件");record.setSnapshotComplete(1);executionMapper.insert(record);Long execId=record.getId();
   ExecutionContext ctx=new ExecutionContext(execId);StringBuilder nodeSummary=new StringBuilder();
   for(TaskNode node:nodeMapper.selectByTaskIdOrdered(taskId)){NodeType type=NodeType.of(node.getNodeType());NodeExecutor executor=executors.get(type.getCode());if(executor==null){nodeSummary.append(type.getCode()).append("已跳过；");continue;}NodeResult result=executor.execute(node,ctx);nodeSummary.append(type.getCode()).append(result.isSuccess()?"成功；":"失败；");if(!result.isSuccess()){finish(record,ExecutionStatus.FAILED,0,"任务节点执行失败："+result.getMessage());return execId;}}
   if(!definition.isEmpty()){
    definition.put("taskId",taskId);definition.put("executionId",execId);definition.put("idempotencyKey","task-"+taskId+"-execution-"+execId);
    List<MessageDelivery>sent=messageService.sendAll(definition);int success=(int)sent.stream().filter(x->"SUCCESS".equals(x.getStatus())).count();record.setTargetCount(sent.size());record.setSuccessCount(success);
    if(success==sent.size())finish(record,ExecutionStatus.SUCCESS,success,"推送成功 "+success+"/"+sent.size());
    else if(success>0)finish(record,ExecutionStatus.PARTIAL_SUCCESS,success,"部分成功 "+success+"/"+sent.size());
    else finish(record,ExecutionStatus.FAILED,0,"推送失败 0/"+sent.size());
   }else finish(record,ExecutionStatus.SUCCESS,0,nodeSummary.length()==0?"执行成功":nodeSummary.toString());
   try{redisTemplate.opsForValue().set(CTX_PREFIX+execId,new LinkedHashMap<>(ctx.getData()),Duration.ofHours(1));}catch(Exception ignore){}
   try{notificationProducer.publishAudit(taskId,execId,record.getStatus());}catch(Exception ignore){}
   return execId;
  }finally{redisTemplate.delete(lockKey);}
 }
 public Long recordSkipped(Long taskId,String decision,String reason){AutomationTask task=taskMapper.selectById(taskId);TaskExecution r=new TaskExecution();r.setTaskId(taskId);r.setTaskNameSnapshot(task==null?"历史任务":task.getName());r.setTriggerType("SCHEDULED");r.setStatus(ExecutionStatus.SUCCESS.getCode());r.setResult("已跳过："+reason);r.setStartTime(LocalDateTime.now());r.setEndTime(LocalDateTime.now());r.setTargetCount(0);r.setSuccessCount(0);r.setScheduleDecision(decision);r.setScheduleDecisionReason(reason);r.setSnapshotComplete(1);executionMapper.insert(r);return r.getId();}
 private void finish(TaskExecution r,ExecutionStatus status,int success,String result){r.setStatus(status.getCode());r.setSuccessCount(success);r.setResult(result);r.setEndTime(LocalDateTime.now());executionMapper.updateById(r);}
 @SuppressWarnings("unchecked")private Map<String,Object>parse(String value){if(value==null||value.isBlank())return new LinkedHashMap<>();try{return objectMapper.readValue(value,LinkedHashMap.class);}catch(Exception e){throw new IllegalArgumentException("任务消息定义无效",e);}}
 private String summary(Map<String,Object>d){Object c=d.get("content");if(!(c instanceof Map<?,?>m))return"";Object text=m.get("content");String s=text==null?String.valueOf(c):text.toString();return s.length()>500?s.substring(0,500)+"…":s;}
 private String string(Object v){return v==null?null:v.toString();}
}
