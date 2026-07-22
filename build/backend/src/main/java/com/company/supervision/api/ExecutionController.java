package com.company.supervision.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.company.supervision.application.ExecutionAppService;
import com.company.supervision.application.identity.AuthService;
import com.company.supervision.entity.dto.*;
import com.company.supervision.infrastructure.security.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/executions")
public class ExecutionController {
    private final ExecutionAppService service;public ExecutionController(ExecutionAppService service){this.service=service;}
    private DataScope scope(HttpServletRequest r){return DataScope.from((AuthService.SessionInfo)r.getAttribute(AuthInterceptor.SESSION_ATTRIBUTE));}
    @GetMapping public ApiResult<IPage<ExecutionSummary>>list(@RequestParam(required=false)Long taskId,@RequestParam(required=false)String status,@RequestParam(required=false)Long creatorAccountId,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int size,HttpServletRequest r){return ApiResult.ok(service.listExecutions(taskId,status,creatorAccountId,page,size,scope(r)));}
    @GetMapping("/{id}")public ApiResult<ExecutionDetail>get(@PathVariable Long id,HttpServletRequest r){return ApiResult.ok(service.getExecution(id,scope(r)));}
}
