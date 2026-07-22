package com.company.supervision.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.company.supervision.application.TaskAppService;
import com.company.supervision.application.identity.AuthService;
import com.company.supervision.entity.dto.*;
import com.company.supervision.infrastructure.security.AuthInterceptor;
import com.company.supervision.infrastructure.security.DataScope;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/tasks")
public class TaskController {
    private final TaskAppService service; public TaskController(TaskAppService service){this.service=service;}
    private DataScope scope(HttpServletRequest r){return DataScope.from((AuthService.SessionInfo)r.getAttribute(AuthInterceptor.SESSION_ATTRIBUTE));}
    @PostMapping public ApiResult<Long>create(@RequestBody TaskCreateRequest req,HttpServletRequest r){return ApiResult.ok(service.createTask(req,scope(r)));}
    @GetMapping public ApiResult<IPage<TaskView>>list(@RequestParam(required=false)String name,@RequestParam(required=false)Long creatorAccountId,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int size,HttpServletRequest r){return ApiResult.ok(service.listTasks(name,creatorAccountId,page,size,scope(r)));}
    @GetMapping("/{id}")public ApiResult<TaskDetail>get(@PathVariable Long id,HttpServletRequest r){return ApiResult.ok(service.getTask(id,scope(r)));}
    @PutMapping("/{id}")public ApiResult<Void>update(@PathVariable Long id,@RequestBody TaskCreateRequest req,HttpServletRequest r){service.updateTask(id,req,scope(r));return ApiResult.ok();}
    @DeleteMapping("/{id}")public ApiResult<Void>delete(@PathVariable Long id,HttpServletRequest r){service.deleteTask(id,scope(r));return ApiResult.ok();}
    @PostMapping("/{id}/execute")public ApiResult<Long>execute(@PathVariable Long id,HttpServletRequest r){return ApiResult.ok(service.executeNow(id,scope(r)));}
}
