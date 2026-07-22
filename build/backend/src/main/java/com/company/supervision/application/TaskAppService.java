package com.company.supervision.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.supervision.api.ResourceNotFoundException;
import com.company.supervision.application.identity.CreatorViewService;
import com.company.supervision.application.scheduling.AdvancedScheduleService;
import com.company.supervision.domain.model.*;
import com.company.supervision.domain.model.enumeration.TaskStatus;
import com.company.supervision.domain.model.messaging.TaskRecipient;
import com.company.supervision.domain.service.TaskExecutionEngine;
import com.company.supervision.entity.dto.*;
import com.company.supervision.infrastructure.repository.*;
import com.company.supervision.infrastructure.repository.messaging.TaskRecipientMapper;
import com.company.supervision.infrastructure.security.DataScope;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class TaskAppService {
    private final TaskMapper tasks; private final TaskNodeMapper nodes; private final ScheduleMapper schedules;
    private final ExecutionMapper executions; private final TaskExecutionEngine engine; private final AdvancedScheduleService advanced;
    private final TaskRecipientMapper recipients; private final ObjectMapper json; private final RobotAppService robotAppService;
    private final CreatorViewService creators;

    public TaskAppService(TaskMapper t,TaskNodeMapper n,ScheduleMapper s,ExecutionMapper e,TaskExecutionEngine x,
                          SchedulerAppService legacy,AdvancedScheduleService a,TaskRecipientMapper r,ObjectMapper j,
                          RobotAppService robotService,CreatorViewService creators){
        tasks=t;nodes=n;schedules=s;executions=e;engine=x;advanced=a;recipients=r;json=j;robotAppService=robotService;this.creators=creators;
    }

    @Transactional
    public Long createTask(TaskCreateRequest req, DataScope scope){
        AutomationTask task=new AutomationTask();copy(task,req,scope);task.setOwnerAccountId(scope.accountId());
        task.setCreatedBy(null);task.setStatus(TaskStatus.ENABLED.getCode());tasks.insert(task);saveChildren(task.getId(),req);return task.getId();
    }

    @Transactional
    public void updateTask(Long id,TaskCreateRequest req,DataScope scope){
        AutomationTask task=requiredOwner(id,scope);copy(task,req,scope);tasks.updateById(task);
        nodes.delete(new LambdaQueryWrapper<TaskNode>().eq(TaskNode::getTaskId,id));recipients.deleteByTask(id);saveChildren(id,req);
    }

    @Transactional
    public void deleteTask(Long id,DataScope scope){
        requiredOwner(id,scope);nodes.delete(new LambdaQueryWrapper<TaskNode>().eq(TaskNode::getTaskId,id));recipients.deleteByTask(id);
        TaskSchedule schedule=schedules.selectByTaskId(id);if(schedule!=null)schedules.deleteById(schedule.getId());advanced.remove(id);tasks.deleteById(id);
    }

    public IPage<TaskView> listTasks(String name,Long creatorAccountId,int page,int size,DataScope scope){
        if(creatorAccountId!=null&&!scope.admin())throw new IllegalArgumentException("Creator filter requires administrator permission");
        LambdaQueryWrapper<AutomationTask>w=new LambdaQueryWrapper<>();if(name!=null&&!name.isBlank())w.like(AutomationTask::getName,name);
        if(creatorAccountId!=null)w.eq(AutomationTask::getOwnerAccountId,creatorAccountId);else if(!scope.admin())w.eq(AutomationTask::getOwnerAccountId,scope.accountId());
        IPage<AutomationTask>raw=tasks.selectPage(new Page<>(Math.max(page,1),Math.max(size,1)),w.orderByDesc(AutomationTask::getId));
        Page<TaskView>out=new Page<>(raw.getCurrent(),raw.getSize(),raw.getTotal());out.setRecords(raw.getRecords().stream().map(t->view(t,scope)).toList());return out;
    }

    public TaskDetail getTask(Long id,DataScope scope){
        AutomationTask task=requiredReadable(id,scope);TaskDetail detail=new TaskDetail();detail.setTask(view(task,scope));
        detail.setNodes(nodes.selectByTaskIdOrdered(id));detail.setSchedule(schedules.selectByTaskId(id));detail.setRecipients(recipients.byTask(id));return detail;
    }

    public Long executeNow(Long id,DataScope scope){requiredOwner(id,scope);return engine.execute(id,"MANUAL",scope.accountId());}

    private void copy(AutomationTask task,TaskCreateRequest req,DataScope scope){
        task.setName(req.getName());task.setDescription(req.getDescription());
        task.setScheduleType(req.getSchedule()!=null&&!"MANUAL".equals(req.getSchedule().getMode())?1:(req.getScheduleType()==null?0:req.getScheduleType()));
        validateMessageTarget(req.getMessageDefinition(),scope);
        try{task.setMessageDefinition(req.getMessageDefinition()==null?null:json.writeValueAsString(req.getMessageDefinition()));}catch(Exception e){throw new IllegalArgumentException(e);}
    }

    private void validateMessageTarget(Map<String,Object> message,DataScope scope){
        if(message==null||!"GROUP_WEBHOOK".equals(String.valueOf(message.get("channel"))))return;
        LinkedHashSet<Long>ids=new LinkedHashSet<>();Object many=message.get("webhookIds");
        if(many instanceof Collection<?>)for(Object value:(Collection<?>)many){Long id=longValue(value);if(id!=null)ids.add(id);}
        Long legacy=longValue(message.get("webhookId"));if(legacy!=null)ids.add(legacy);
        robotAppService.validateSelectable(ids,scope);message.put("webhookIds",new ArrayList<>(ids));message.remove("webhookId");
    }

    private void saveChildren(Long id,TaskCreateRequest req){
        if(req.getNodes()!=null)for(TaskCreateRequest.NodeInput x:req.getNodes()){TaskNode n=new TaskNode();n.setTaskId(id);n.setNodeType(x.getNodeType());n.setNodeOrder(x.getNodeOrder());n.setConfig(x.getConfig());nodes.insert(n);}
        if(req.getRecipients()!=null)for(TaskCreateRequest.RecipientInput x:req.getRecipients()){TaskRecipient q=new TaskRecipient();q.setTaskId(id);q.setRecipientType(x.getRecipientType());q.setRecipientRef(x.getRecipientRef());q.setMentionMode(x.getMentionMode());recipients.insert(q);}
        advanced.save(id,req.getSchedule(),req.getCronExpression());
    }

    private TaskView view(AutomationTask task,DataScope scope){TaskView out=new TaskView();out.setId(task.getId());out.setName(task.getName());out.setDescription(task.getDescription());out.setStatus(task.getStatus());out.setScheduleType(task.getScheduleType());out.setMessageDefinition(task.getMessageDefinition());out.setCreatedAt(task.getCreatedAt());out.setUpdatedAt(task.getUpdatedAt());creators.fill(out,task.getOwnerAccountId(),scope);return out;}
    private AutomationTask requiredReadable(Long id,DataScope scope){AutomationTask t=tasks.selectById(id);if(t==null||!scope.canRead(t.getOwnerAccountId()))throw new ResourceNotFoundException("Task not found");return t;}
    private AutomationTask requiredOwner(Long id,DataScope scope){AutomationTask t=tasks.selectById(id);if(t==null||!scope.owns(t.getOwnerAccountId()))throw new ResourceNotFoundException("Task not found");return t;}
    private Long longValue(Object value){if(value==null)return null;if(value instanceof Number)return((Number)value).longValue();try{return Long.valueOf(value.toString());}catch(NumberFormatException ignored){return null;}}
}
